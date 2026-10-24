package com.lotrmod.worldgen.feature;

import com.lotrmod.worldgen.LOTRWorldGen;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * {@code lotrmod:clear_of_trees} — passes only where a tree would stand clear
 * of the trees already there: no trunk within {@code radius} blocks, and no
 * leaves overhanging the spot. Trees only check that their own trunk has room,
 * and leaves count as room, so without this one can grow up through
 * another's crown.
 *
 * <pre>{"type": "lotrmod:clear_of_trees", "radius": 4}</pre>
 */
public class ClearOfTreesFilter extends PlacementFilter {
    public static final MapCodec<ClearOfTreesFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 8).optionalFieldOf("radius", 4).forGetter(f -> f.radius)
    ).apply(instance, ClearOfTreesFilter::new));

    /** How far above the spot to look for another tree's trunk, and for leaves overhead. */
    private static final int TRUNK_HEIGHT = 8;
    private static final int CROWN_HEIGHT = 16;

    private final int radius;

    public ClearOfTreesFilter(int radius) {
        this.radius = radius;
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return clear(context.getLevel(), pos, radius);
    }

    /**
     * Whether a tree at {@code pos} would stand clear of others: no log
     * within {@code radius} blocks across, from a couple of blocks below to
     * {@link #TRUNK_HEIGHT} above, and no leaves over the spot or the blocks
     * beside it, up to {@link #CROWN_HEIGHT}.
     */
    public static boolean clear(WorldGenLevel level, BlockPos pos, int radius) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius + 1) continue;
                for (int dy = -2; dy <= TRUNK_HEIGHT; dy++) {
                    if (level.getBlockState(at.setWithOffset(pos, dx, dy, dz)).is(BlockTags.LOGS)) return false;
                }
            }
        }
        for (int dy = 0; dy <= CROWN_HEIGHT; dy++) {
            if (level.getBlockState(at.setWithOffset(pos, 0, dy, 0)).is(BlockTags.LEAVES)) return false;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(at.setWithOffset(pos, dir.getStepX(), dy, dir.getStepZ())).is(BlockTags.LEAVES)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public PlacementModifierType<?> type() {
        return LOTRWorldGen.CLEAR_OF_TREES.get();
    }
}
