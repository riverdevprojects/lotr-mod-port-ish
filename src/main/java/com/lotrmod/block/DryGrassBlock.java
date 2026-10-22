package com.lotrmod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A tuft of Rohan's grass, sun-dried to straw: short grass in its own colours
 * rather than the biome's, so it stays gold wherever it grows. Unlike vanilla's
 * short grass, bone meal doesn't grow it.
 */
public class DryGrassBlock extends BushBlock {
    public static final MapCodec<DryGrassBlock> CODEC = simpleCodec(DryGrassBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public DryGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<DryGrassBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
