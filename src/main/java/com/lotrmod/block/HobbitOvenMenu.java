package com.lotrmod.block;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Port of {@code LOTRContainerHobbitOven}'s slot layout: 9 input slots (row y=21), 9 output
 * slots (row y=67, one per input, index+9), 1 shared fuel slot (80,111), player inventory below.
 */
public class HobbitOvenMenu extends AbstractContainerMenu {

    private final Container oven;
    private final ContainerData data;

    public HobbitOvenMenu(int id, Inventory playerInv) {
        this(id, playerInv, new SimpleContainer(HobbitOvenBlockEntity.SLOT_COUNT), new SimpleContainerData(4));
    }

    public HobbitOvenMenu(int id, Inventory playerInv, Container oven, ContainerData data) {
        super(ModMenus.HOBBIT_OVEN.get(), id);
        checkContainerSize(oven, HobbitOvenBlockEntity.SLOT_COUNT);
        this.oven = oven;
        this.data = data;

        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(oven, i, 8 + i * 18, 21));
        }
        for (int i = 0; i < 9; i++) {
            final int idx = i;
            addSlot(new Slot(oven, i + 9, 8 + i * 18, 67) {
                @Override
                public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        addSlot(new Slot(oven, HobbitOvenBlockEntity.FUEL_SLOT, 80, 111) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.isFuel(stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 133 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 191));
        }

        addDataSlots(data);
    }

    public boolean isLit() { return data.get(0) > 0; }
    public int getCookProgressScaled(int scale) { int total = data.get(3); return total == 0 ? 0 : data.get(2) * scale / total; }
    public int getLitProgressScaled(int scale) { int dur = data.get(1); return dur == 0 ? 0 : data.get(0) * scale / dur; }

    @Override
    public boolean stillValid(Player player) {
        return oven.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int fuelSlot = 18, ovenEnd = 19, invEnd = 55;
            if (index < ovenEnd) {
                if (!moveItemStackTo(stack, ovenEnd, invEnd, true)) return ItemStack.EMPTY;
            } else if (net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.isFuel(stack)) {
                if (!moveItemStackTo(stack, fuelSlot, fuelSlot + 1, false) && !moveItemStackTo(stack, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, 9, false)) {
                if (!moveItemStackTo(stack, ovenEnd, invEnd, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
