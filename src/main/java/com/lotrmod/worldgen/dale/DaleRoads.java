package com.lotrmod.worldgen.dale;

import com.lotrmod.block.DaleBlocks;
import com.lotrmod.worldgen.DaleTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * The roads out of the town of Dale: north up the vale beside the river to
 * the Front Gate of Erebor, south over the river by a bridge of stone and
 * down to a landing on the Long Lake, and west up onto Ravenhill.
 *
 * <p>In Girion's day they are beaten earth with gravel at the edges, the
 * bridge whole, with a parapet; the landing a jetty of planks out into the
 * lake. In the Desolation the roads are faint tracks of coarse earth and
 * gravel, the bridge broken in the middle, and the jetty gone but for its
 * posts.
 */
final class DaleRoads {
    private DaleRoads() {}

    private static final double HALF_WIDTH = 2.2;
    /** The bridge's deck, the block a traveller walks on, a few blocks over the water. */
    private static final int DECK = 65;

    private static final double[][][] ROADS = {
            // North, to the Front Gate
            {{3920, -12322}, {3922, -12400}, {3925, -12480}, {3928, -12560}, {3933, -12640}, {3939, -12700}, {3941, -12720}},
            // South, over the river and down to the Long Lake
            {{3920, -12136}, {3920, -12100}, {3918, -12060}, {3905, -11990}, {3870, -11920}, {3820, -11860}, {3792, -11800}, {3778, -11770}},
            // West, up onto Ravenhill
            {{3826, -12230}, {3780, -12226}, {3730, -12212}, {3690, -12196}, {3660, -12182}}};

    /** The landing on the Long Lake: from the shore at the end of the south road, out into the water. */
    private static final int JETTY_X = 3774;
    private static final int JETTY_Z0 = -11768;
    private static final int JETTY_Z1 = -11726;

    private record Near(double distance, double along) {}

    static void build(WorldGenLevel level, BoundingBox box, DaleEra era) {
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                if (DaleTown.radius(x, z) < DaleTown.WALL_OUT + 0.5) continue;
                Near near = nearest(x, z);
                if (near == null || near.distance > HALF_WIDTH + 1.5) continue;
                column(level, x, z, near, era);
            }
        }
        jetty(level, box, era);
    }

    /** The nearest point of any road, if one is close. */
    private static Near nearest(double x, double z) {
        Near best = null;
        for (double[][] road : ROADS) {
            double along = 0.0;
            for (int k = 0; k < road.length - 1; k++) {
                double ax = road[k][0], az = road[k][1], bx = road[k + 1][0], bz = road[k + 1][1];
                double ex = bx - ax, ez = bz - az;
                double l2 = ex * ex + ez * ez;
                double length = Math.sqrt(l2);
                if (x < Math.min(ax, bx) - 8 || x > Math.max(ax, bx) + 8 || z < Math.min(az, bz) - 8 || z > Math.max(az, bz) + 8) {
                    along += length;
                    continue;
                }
                double t = Mth.clamp(((x - ax) * ex + (z - az) * ez) / l2, 0.0, 1.0);
                double d = Math.sqrt(sq(x - (ax + t * ex)) + sq(z - (az + t * ez)));
                if (best == null || d < best.distance) best = new Near(d, along + t * length);
                along += length;
            }
        }
        return best;
    }

    private static void column(WorldGenLevel level, int x, int z, Near near, DaleEra era) {
        boolean flourishing = era == DaleEra.FLOURISHING;
        double noise = DaleTown.hash(x, z, 21);
        double river = DaleTerrain.riverDistance(x, z) - DaleTerrain.riverHalfWidth(x, z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        if (river < 4.0) {
            bridge(level, x, z, near, river, flourishing);
            return;
        }
        int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
        pos.set(x, ground, z);
        BlockState top = level.getBlockState(pos);
        if (!top.is(Blocks.GRASS_BLOCK) && !top.is(Blocks.DIRT) && !top.is(Blocks.COARSE_DIRT) && !top.is(Blocks.PODZOL)
                && !top.is(Blocks.GRAVEL) && !top.is(Blocks.STONE)) {
            return;
        }
        boolean edge = near.distance > HALF_WIDTH;
        BlockState surface;
        if (flourishing) {
            if (edge) {
                surface = noise < 0.5 ? Blocks.GRAVEL.defaultBlockState() : top;
            } else {
                surface = noise < 0.12 ? Blocks.GRAVEL.defaultBlockState()
                        : noise < 0.2 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState();
            }
        } else {
            if (edge || noise < 0.3) return;
            surface = noise < 0.6 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
        }
        level.setBlock(pos, surface, 2);
        // Nothing growing in the road
        BlockState above = level.getBlockState(pos.move(0, 1, 0));
        if (!above.isAir() && above.canBeReplaced()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
    }

    /**
     * A stone bridge over the river where the road crosses it: a deck of
     * Dale brick on piers every six blocks, with a low wall along each side.
     * Broken in the middle in the Desolation.
     */
    private static void bridge(WorldGenLevel level, int x, int z, Near near, double river, boolean flourishing) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        if (!flourishing && river < -DaleTerrain.riverHalfWidth(x, z) * 0.45) return;
        boolean side = near.distance > HALF_WIDTH - 0.3;
        BlockState brick = DaleBlocks.DALE_BRICKS.get().defaultBlockState();
        BlockState deck = flourishing ? Blocks.STONE_BRICKS.defaultBlockState()
                : DaleTown.hash(x, z, 22) < 0.5 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        level.setBlock(pos.set(x, DECK - 1, z), side ? brick : deck, 2);
        for (int y = DECK; y <= DECK + 3; y++) level.setBlock(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        if (side && near.distance <= HALF_WIDTH + 1.0) {
            level.setBlock(pos.set(x, DECK, z), DaleBlocks.DALE_BRICK_WALL.get().defaultBlockState(), 2);
            level.getChunk(pos).markPosForPostprocessing(pos.immutable());
        }
        // Piers down to the bed, and arches between them
        boolean pier = Math.floorMod((int) Math.round(near.along), 6) == 0;
        int below = DECK - 2;
        if (pier) {
            for (int y = below; y > level.getMinBuildHeight(); y--) {
                pos.set(x, y, z);
                BlockState state = level.getBlockState(pos);
                if (!state.isAir() && state.getFluidState().isEmpty()) break;
                level.setBlock(pos, brick, 2);
            }
        } else {
            level.setBlock(pos.set(x, below, z), DaleBlocks.DALE_BRICK_SLAB.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP), 2);
        }
    }

    /** A jetty of planks on posts, out into the Long Lake; in the Desolation only its posts stand. */
    private static void jetty(WorldGenLevel level, BoundingBox box, DaleEra era) {
        if (JETTY_X + 2 < box.minX() || JETTY_X - 2 > box.maxX() || JETTY_Z1 < box.minZ() || JETTY_Z0 > box.maxZ()) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean flourishing = era == DaleEra.FLOURISHING;
        for (int x = JETTY_X - 1; x <= JETTY_X + 1; x++) {
            for (int z = JETTY_Z0; z <= JETTY_Z1; z++) {
                if (!box.isInside(x, 63, z)) continue;
                boolean post = Math.abs(x - JETTY_X) == 1 && Math.floorMod(z - JETTY_Z0, 4) == 0;
                if (flourishing) level.setBlock(pos.set(x, 63, z), Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
                if (post) {
                    for (int y = flourishing ? 62 : 63; y > level.getMinBuildHeight(); y--) {
                        pos.set(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir() && state.getFluidState().isEmpty()) break;
                        level.setBlock(pos, (flourishing ? Blocks.SPRUCE_LOG : DaleBlocks.CHARRED_LOG.get()).defaultBlockState(), 2);
                    }
                    if (flourishing) {
                        level.setBlock(pos.set(x, 64, z), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
                        if (z == JETTY_Z1) level.setBlock(pos.set(x, 65, z), Blocks.LANTERN.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static double sq(double v) {
        return v * v;
    }
}
