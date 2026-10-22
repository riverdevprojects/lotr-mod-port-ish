package com.lotrmod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A port of the original mod's {@code LOTRBlockChandelier}: a candle-ring that
 * hangs from the block above, drawn as crossed planes, giving light 14 and a
 * flame and wisp of smoke at each of its four candles.
 *
 * <p>It hangs from anything the original allowed: a fence, a wall, a chain, a
 * bottom slab, an upright stair, or any block whose underside is solid.
 */
public class ChandelierBlock extends Block {
    public static final MapCodec<ChandelierBlock> CODEC = simpleCodec(ChandelierBlock::new);

    private static final VoxelShape SHAPE = Block.box(1.0, 3.0, 1.0, 15.0, 16.0, 15.0);

    public ChandelierBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ChandelierBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos abovePos = pos.above();
        BlockState above = level.getBlockState(abovePos);
        Block block = above.getBlock();
        if (block instanceof FenceBlock || block instanceof WallBlock) return true;
        if (block instanceof ChainBlock) return above.getValue(ChainBlock.AXIS) == Direction.Axis.Y;
        if (block instanceof SlabBlock) return above.getValue(SlabBlock.TYPE) == SlabType.BOTTOM;
        if (block instanceof StairBlock) return above.getValue(StairBlock.HALF) == Half.BOTTOM;
        return above.isFaceSturdy(level, abovePos, Direction.DOWN);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double near = 0.13;
        double far = 1.0 - near;
        double y = pos.getY() + 0.6875;
        candle(level, pos.getX() + near, y, pos.getZ() + near);
        candle(level, pos.getX() + far, y, pos.getZ() + far);
        candle(level, pos.getX() + near, y, pos.getZ() + far);
        candle(level, pos.getX() + far, y, pos.getZ() + near);
    }

    private static void candle(Level level, double x, double y, double z) {
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
    }
}
