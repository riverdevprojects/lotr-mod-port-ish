package com.lotrmod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The original mod's hobbit crafting table, which stands in a hobbit hole's
 * kitchen. It opens an ordinary crafting grid: the original's hobbit-only
 * recipes and its faction-alignment check have nothing to port to here.
 *
 * <p>Vanilla's {@link CraftingMenu} closes itself unless it was opened from a
 * {@code minecraft:crafting_table}, so the menu is told to accept this block.
 */
public class HobbitTableBlock extends CraftingTableBlock {
    public static final MapCodec<HobbitTableBlock> CODEC = simpleCodec(HobbitTableBlock::new);

    private static final Component TITLE = Component.translatable("container.lotrmod.hobbit_table");

    public HobbitTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends CraftingTableBlock> codec() {
        return CODEC;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        ContainerLevelAccess access = ContainerLevelAccess.create(level, pos);
        return new SimpleMenuProvider((id, inventory, player) -> new CraftingMenu(id, inventory, access) {
            @Override
            public boolean stillValid(Player user) {
                return stillValid(access, user, HobbitTableBlock.this);
            }
        }, TITLE);
    }
}
