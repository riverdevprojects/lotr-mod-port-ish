package com.lotrmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Port of {@code LOTRTileEntityHobbitOven} (1.7.10): 19 slots (0-8 input, 9-17 output — one
 * output per input, 18 = single shared fuel slot). All 9 input/output pairs with a currently
 * valid recipe cook SIMULTANEOUSLY off the one shared fuel slot and one shared 400-tick
 * progress timer, exactly like the original — this is not 9 independent furnaces.
 *
 * <p>Cook-input restriction ({@code isCookResultAcceptable} in the original): a slot is only
 * accepted if smelting it yields a food result. The original also allowed pipeweed and dried
 * reeds results; neither item exists in this codebase yet, so only the food check is ported
 * (see TODO below).
 */
public class HobbitOvenBlockEntity extends BaseContainerBlockEntity {

    public static final int INPUT_SLOTS = 9;
    public static final int OUTPUT_START = 9;
    public static final int FUEL_SLOT = 18;
    public static final int SLOT_COUNT = 19;
    private static final int COOK_TIME_TOTAL = 400;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int litTime;
    private int litDuration;
    private int cookingProgress;

    protected final SimpleContainerData dataAccess = new SimpleContainerData(4) {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> litTime;
                case 1 -> litDuration;
                case 2 -> cookingProgress;
                case 3 -> COOK_TIME_TOTAL;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> litTime = value;
                case 1 -> litDuration = value;
                case 2 -> cookingProgress = value;
                default -> {}
            }
        }

        @Override
        public int getCount() { return 4; }
    };

    public HobbitOvenBlockEntity(BlockPos pos, BlockState state) {
        super(com.lotrmod.block.ModBlockEntities.HOBBIT_OVEN.get(), pos, state);
    }

    public boolean isLit() {
        return litTime > 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HobbitOvenBlockEntity oven) {
        boolean wasLit = oven.isLit();
        boolean needsUpdate = false;

        if (oven.litTime > 0) oven.litTime--;

        if (oven.litTime == 0 && oven.canCookAny(level)) {
            ItemStack fuel = oven.items.get(FUEL_SLOT);
            int burn = AbstractFurnaceBlockEntity.getFuel().getOrDefault(fuel.getItem(), 0);
            if (burn > 0) {
                oven.litDuration = burn;
                oven.litTime = burn;
                needsUpdate = true;
                fuel.shrink(1);
            }
        }

        if (oven.isLit() && oven.canCookAny(level)) {
            oven.cookingProgress++;
            if (oven.cookingProgress >= COOK_TIME_TOTAL) {
                oven.cookingProgress = 0;
                for (int i = 0; i < INPUT_SLOTS; i++) {
                    oven.cookSlot(level, i);
                }
                needsUpdate = true;
            }
        } else {
            oven.cookingProgress = 0;
        }

        if (wasLit != oven.isLit()) {
            needsUpdate = true;
            level.setBlock(pos, state.setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT, oven.isLit()), 3);
        }
        if (needsUpdate) oven.setChanged();
    }

    private boolean canCookAny(Level level) {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            if (canCook(level, i)) return true;
        }
        return false;
    }

    private boolean canCook(Level level, int i) {
        ItemStack input = items.get(i);
        if (input.isEmpty()) return false;
        ItemStack result = smeltResult(level, input);
        if (!isCookResultAcceptable(result)) return false;
        ItemStack out = items.get(OUTPUT_START + i);
        if (out.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(out, result)) return false;
        int newCount = out.getCount() + result.getCount();
        return newCount <= getMaxStackSize() && newCount <= out.getMaxStackSize();
    }

    private void cookSlot(Level level, int i) {
        if (!canCook(level, i)) return;
        ItemStack input = items.get(i);
        ItemStack result = smeltResult(level, input);
        ItemStack out = items.get(OUTPUT_START + i);
        if (out.isEmpty()) {
            items.set(OUTPUT_START + i, result.copy());
        } else {
            out.grow(result.getCount());
        }
        input.shrink(1);
    }

    private static ItemStack smeltResult(Level level, ItemStack input) {
        RecipeManager rm = level.getRecipeManager();
        Optional<net.minecraft.world.item.crafting.RecipeHolder<SmeltingRecipe>> recipe =
                rm.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level);
        return recipe.map(r -> r.value().assemble(new SingleRecipeInput(input), level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    /** TODO: original also accepted pipeweed and dried-reeds results; neither item exists yet in this codebase. */
    public static boolean isCookResultAcceptable(ItemStack result) {
        if (result.isEmpty()) return false;
        return result.has(DataComponents.FOOD);
    }

    public int getCookProgressScaled(int scale) {
        return cookingProgress * scale / COOK_TIME_TOTAL;
    }

    public int getLitProgressScaled(int scale) {
        if (litDuration == 0) return 0;
        return litTime * scale / litDuration;
    }

    // ── Container ────────────────────────────────────────────────────────────────

    @Override
    protected NonNullList<ItemStack> getItems() { return items; }

    @Override
    protected void setItems(NonNullList<ItemStack> newItems) { this.items = newItems; }

    @Override
    public int getContainerSize() { return SLOT_COUNT; }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(items, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.lotrmod.hobbit_oven");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv) {
        return new HobbitOvenMenu(id, inv, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("LitTime", litTime);
        tag.putInt("LitDuration", litDuration);
        tag.putInt("CookingProgress", cookingProgress);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        litTime = tag.getInt("LitTime");
        litDuration = tag.getInt("LitDuration");
        cookingProgress = tag.getInt("CookingProgress");
        ContainerHelper.loadAllItems(tag, items, registries);
    }
}
