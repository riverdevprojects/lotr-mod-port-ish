package com.lotrmod.worldgen.dale;

import com.lotrmod.block.DaleBlocks;
import com.lotrmod.block.ModBlocks;
import com.lotrmod.worldgen.DaleTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * The fields of Dale: the farmland of the vale, laid out as a patchwork of
 * plots on one grid across the whole of it, about twenty blocks by twenty,
 * the edges a little ragged.
 *
 * <p>In Girion's day each plot is sown with wheat, mostly, or carrots,
 * potatoes or beetroot in rows, with an irrigation ditch down every sixth row
 * where the ground is level enough to hold water; or left to pasture or hay
 * meadow, or a patch of pumpkins and melons. Between them run hedges, dry
 * stone walls, fences and cart-tracks. Here and there a plot is a farmstead:
 * a Dale house ({@link DaleBuildings#house}) with a yard of hay.
 *
 * <p>In the Desolation the same lines show what the fire left. Where it
 * burnt ({@link DaleTerrain#scorched}), the walls are tumbled stones, the
 * hedges charred stumps, the tracks bare earth, the furrows still faintly
 * scored in the dead ground, and the farmsteads burnt shells. Further out,
 * where the folk only fled, the walls are mossed over, the hedges grown wild,
 * the fields gone to wild oats and seeding grass, and the farmsteads stand
 * empty and falling in.
 */
final class DaleFields {
    private DaleFields() {}

    private static final int CELL_X = 23;
    private static final int CELL_Z = 19;
    /** How far a line between plots is pushed off its place on the grid. */
    private static final int JITTER = 4;

    enum Crop { WHEAT, CARROTS, POTATOES, BEETROOTS, PASTURE, HAY, GOURDS, FARMSTEAD }

    enum Edge { HEDGE, WALL, FENCE, TRACK }

    // ── the grid ─────────────────────────────────────────────────────────────

    private static int line(int n, int cell, int salt) {
        return n * cell + (int) Math.floor((DaleTown.hash(n, salt, 51) - 0.5) * 2.0 * JITTER);
    }

    /** Which plot along one axis a coordinate lies in. */
    private static int cell(int v, int cell, int salt) {
        int n = Math.floorDiv(v, cell);
        if (v < line(n, cell, salt)) return n - 1;
        if (v >= line(n + 1, cell, salt)) return n + 1;
        return n;
    }

    private static Crop crop(int cx, int cz) {
        double r = DaleTown.hash(cx, cz, 52);
        if (r < 0.38) return Crop.WHEAT;
        if (r < 0.48) return Crop.CARROTS;
        if (r < 0.57) return Crop.POTATOES;
        if (r < 0.63) return Crop.BEETROOTS;
        if (r < 0.77) return Crop.PASTURE;
        if (r < 0.85) return Crop.HAY;
        if (r < 0.92) return Crop.GOURDS;
        // A farmstead, unless the plot to the west or north is one already
        double w = DaleTown.hash(cx - 1, cz, 52), n = DaleTown.hash(cx, cz - 1, 52);
        return w >= 0.92 || n >= 0.92 ? Crop.PASTURE : Crop.FARMSTEAD;
    }

    private static Edge edge(int n, int across, boolean alongX) {
        double r = DaleTown.hash(n * 7 + (alongX ? 1 : 0), across, 53);
        if (r < 0.34) return Edge.HEDGE;
        if (r < 0.6) return Edge.WALL;
        if (r < 0.78) return Edge.FENCE;
        return Edge.TRACK;
    }

    // ── laying them out ──────────────────────────────────────────────────────

    static void build(WorldGenLevel level, BoundingBox box, DaleEra era) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                if (DaleTerrain.zone(x, z) != DaleTerrain.Zone.FIELDS) continue;
                DaleBuilder.State state = state(era, x, z);
                int cx = cell(x, CELL_X, 1), cz = cell(z, CELL_Z, 2);
                int lx = line(cx, CELL_X, 1), lz = line(cz, CELL_Z, 2);
                int u = x - lx, v = z - lz;
                int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                pos.set(x, ground, z);
                if (!level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) continue;

                Crop crop = crop(cx, cz);
                if (u <= 1 || v <= 1) {
                    // The line between plots: whichever runs along this edge
                    Edge edge = u <= 1 ? edge(cx, cz, false) : edge(cz, cx, true);
                    int w = u <= 1 ? u : v;
                    if (edge != Edge.TRACK && w == 1) {
                        if (crop != Crop.FARMSTEAD) sow(level, pos, x, z, ground, crop, state, u, v);
                        continue;
                    }
                    border(level, pos, x, z, ground, edge, state);
                    continue;
                }
                if (crop == Crop.FARMSTEAD) continue;
                sow(level, pos, x, z, ground, crop, state, u, v);
            }
        }
        farmsteads(level, box, era);
    }

    /** What has become of the land here: tilled, burnt, or left. */
    private static DaleBuilder.State state(DaleEra era, int x, int z) {
        if (era == DaleEra.FLOURISHING) return DaleBuilder.State.INTACT;
        return DaleTerrain.scorched(x, z) ? DaleBuilder.State.BURNT : DaleBuilder.State.ABANDONED;
    }

    private static void border(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int ground, Edge edge, DaleBuilder.State state) {
        double r = DaleTown.hash(x, z, 54);
        int y = ground + 1;
        clear(level, pos, x, z, y);
        switch (state) {
            case INTACT -> {
                switch (edge) {
                    case HEDGE -> {
                        put(level, pos.set(x, y, z), hedge());
                        if (r < 0.6) put(level, pos.set(x, y + 1, z), hedge());
                    }
                    case WALL -> {
                        put(level, pos.set(x, y, z), (r < 0.3 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE).defaultBlockState());
                        if (r < 0.12) put(level, pos.set(x, y + 1, z), Blocks.COBBLESTONE_SLAB.defaultBlockState());
                    }
                    case FENCE -> put(level, pos.set(x, y, z), Blocks.OAK_FENCE.defaultBlockState());
                    case TRACK -> put(level, pos.set(x, ground, z), r < 0.15 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState());
                }
            }
            case BURNT -> {
                switch (edge) {
                    case HEDGE -> {
                        if (r < 0.2) put(level, pos.set(x, y, z), DaleBlocks.CHARRED_LOG.get().defaultBlockState());
                        else if (r < 0.3) put(level, pos.set(x, y, z), Blocks.DEAD_BUSH.defaultBlockState());
                    }
                    case WALL -> {
                        if (r < 0.45) put(level, pos.set(x, y, z), (r < 0.15 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE).defaultBlockState());
                        else if (r < 0.6) put(level, pos.set(x, ground, z), Blocks.GRAVEL.defaultBlockState());
                    }
                    case FENCE -> {
                        if (r < 0.1) put(level, pos.set(x, y, z), DaleBlocks.CHARRED_LOG.get().defaultBlockState());
                    }
                    case TRACK -> put(level, pos.set(x, ground, z), r < 0.5 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState());
                }
            }
            default -> {
                switch (edge) {
                    case HEDGE -> {
                        // Grown wild: taller and gappy
                        if (r < 0.85) {
                            put(level, pos.set(x, y, z), hedge());
                            if (r < 0.7) put(level, pos.set(x, y + 1, z), hedge());
                            if (r < 0.3) put(level, pos.set(x, y + 2, z), hedge());
                        }
                    }
                    case WALL -> {
                        if (r < 0.7) put(level, pos.set(x, y, z), (r < 0.5 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE).defaultBlockState());
                    }
                    case FENCE -> {
                        if (r < 0.25) put(level, pos.set(x, y, z), Blocks.OAK_FENCE.defaultBlockState());
                    }
                    case TRACK -> {
                        if (r < 0.55) put(level, pos.set(x, ground, z), Blocks.DIRT_PATH.defaultBlockState());
                    }
                }
            }
        }
    }

    private static BlockState hedge() {
        return Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    /** One column inside a plot: its crop, its pasture, or what is left of them. */
    private static void sow(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int ground, Crop crop,
            DaleBuilder.State state, int u, int v) {
        double r = DaleTown.hash(x, z, 55);
        int y = ground + 1;
        boolean rows = crop == Crop.WHEAT || crop == Crop.CARROTS || crop == Crop.POTATOES || crop == Crop.BEETROOTS;
        // Rows run along the plot's longer side; every sixth is a ditch
        boolean ditch = rows && Math.floorMod(v, 6) == 3;

        if (state == DaleBuilder.State.BURNT) {
            // The old furrows, still faintly scored in the dead ground
            if (rows && Math.floorMod(v, 2) == 0 && r < 0.45) put(level, pos.set(x, ground, z), Blocks.COARSE_DIRT.defaultBlockState());
            return;
        }
        if (state == DaleBuilder.State.ABANDONED) {
            if (crop == Crop.PASTURE || crop == Crop.FARMSTEAD) return;
            clear(level, pos, x, z, y);
            // Gone to wild oats and seeding grass
            if (r < 0.35) put(level, pos.set(x, y, z), ModBlocks.WILD_OATS.get().defaultBlockState());
            else if (r < 0.5) put(level, pos.set(x, y, z), ModBlocks.DRY_GRASS.get().defaultBlockState());
            else if (r < 0.58) tall(level, pos, x, y, z, ModBlocks.TALL_DRY_GRASS.get().defaultBlockState());
            else if (r < 0.7) put(level, pos.set(x, y, z), Blocks.SHORT_GRASS.defaultBlockState());
            return;
        }

        switch (crop) {
            case PASTURE -> {
                // Grazing: left to the grass, and the flowers the biome brings
            }
            case HAY -> {
                clear(level, pos, x, z, y);
                if (r < 0.025) put(level, pos.set(x, y, z), Blocks.HAY_BLOCK.defaultBlockState());
                else if (r < 0.03) {
                    put(level, pos.set(x, y, z), Blocks.HAY_BLOCK.defaultBlockState());
                    put(level, pos.set(x, y + 1, z), Blocks.HAY_BLOCK.defaultBlockState());
                } else if (r < 0.6) tall(level, pos, x, y, z, Blocks.TALL_GRASS.defaultBlockState());
                else put(level, pos.set(x, y, z), Blocks.SHORT_GRASS.defaultBlockState());
            }
            case GOURDS -> {
                clear(level, pos, x, z, y);
                if (Math.floorMod(v, 3) == 0) {
                    put(level, pos.set(x, ground, z), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
                    Block stem = DaleTown.hash(x / 6, z / 6, 56) < 0.6 ? Blocks.PUMPKIN_STEM : Blocks.MELON_STEM;
                    put(level, pos.set(x, y, z), stem.defaultBlockState().setValue(net.minecraft.world.level.block.StemBlock.AGE, 7));
                } else if (r < 0.18) {
                    put(level, pos.set(x, y, z), (DaleTown.hash(x / 6, z / 6, 56) < 0.6 ? Blocks.PUMPKIN : Blocks.MELON).defaultBlockState());
                }
            }
            default -> {
                clear(level, pos, x, z, y);
                if (ditch && level(level, x, z, ground)) {
                    put(level, pos.set(x, ground, z), Blocks.WATER.defaultBlockState());
                    return;
                }
                put(level, pos.set(x, ground, z), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
                Block plant = switch (crop) {
                    case CARROTS -> Blocks.CARROTS;
                    case POTATOES -> Blocks.POTATOES;
                    case BEETROOTS -> Blocks.BEETROOTS;
                    default -> Blocks.WHEAT;
                };
                BlockState state0 = plant.defaultBlockState();
                // Most of the crop near ripe, the plot sown on one day
                double ripe = DaleTown.hash(x / CELL_X, z / CELL_Z, 57);
                int max = plant == Blocks.BEETROOTS ? 3 : 7;
                int age = r < 0.85 ? (int) Math.round(max * (0.5 + ripe * 0.5)) : (int) (r * max);
                if (plant == Blocks.BEETROOTS) {
                    state0 = state0.setValue(net.minecraft.world.level.block.BeetrootBlock.AGE, Math.min(3, age));
                } else {
                    state0 = state0.setValue(CropBlock.AGE, Math.min(7, age));
                }
                put(level, pos.set(x, y, z), state0);
            }
        }
    }

    /** Whether the ground is level all round, so a ditch here holds its water. */
    private static boolean level(WorldGenLevel level, int x, int z, int ground) {
        for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            if (level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x + d[0], z + d[1]) - 1 != ground) return false;
        }
        return true;
    }

    private static void tall(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int y, int z, BlockState plant) {
        if (!plant.hasProperty(net.minecraft.world.level.block.DoublePlantBlock.HALF)) {
            put(level, pos.set(x, y, z), plant);
            return;
        }
        put(level, pos.set(x, y, z), plant);
        put(level, pos.set(x, y + 1, z), plant.setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
    }

    /** Clears grass and flowers already grown here, so what is planted stands alone. */
    private static void clear(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int y) {
        for (int j = y; j <= y + 1; j++) {
            BlockState state = level.getBlockState(pos.set(x, j, z));
            if (!state.isAir() && state.canBeReplaced() && state.getFluidState().isEmpty()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private static void put(WorldGenLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2);
        if (state.getBlock() instanceof net.minecraft.world.level.block.CrossCollisionBlock) {
            level.getChunk(pos).markPosForPostprocessing(pos.immutable());
        }
    }

    // ── farmsteads ───────────────────────────────────────────────────────────

    /**
     * The farmsteads of the plots near this chunk: a Dale house in the middle
     * of its plot, on ground level enough for it, with hay stacked in the yard.
     */
    private static void farmsteads(WorldGenLevel level, BoundingBox box, DaleEra era) {
        int cx0 = cell(box.minX() - 16, CELL_X, 1), cx1 = cell(box.maxX() + 16, CELL_X, 1);
        int cz0 = cell(box.minZ() - 16, CELL_Z, 2), cz1 = cell(box.maxZ() + 16, CELL_Z, 2);
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                if (crop(cx, cz) != Crop.FARMSTEAD) continue;
                int x0 = line(cx, CELL_X, 1), x1 = line(cx + 1, CELL_X, 1) - 1;
                int z0 = line(cz, CELL_Z, 2), z1 = line(cz + 1, CELL_Z, 2) - 1;
                int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
                int rotation = (int) (DaleTown.hash(cx, cz, 58) * 4);
                // The house's front, set so the house sits in the middle of its plot
                int[] f = DaleBuilder.footprint(mx, mz, rotation, 1, DaleBuildings.HOUSE);
                int sx = mx - ((f[0] + f[2]) / 2 - mx), sz = mz - ((f[1] + f[3]) / 2 - mz);
                f = DaleBuilder.footprint(sx, sz, rotation, 1, DaleBuildings.HOUSE);
                if (f[2] < box.minX() - 1 || f[0] > box.maxX() + 1 || f[3] < box.minZ() - 1 || f[1] > box.maxZ() + 1) continue;
                if (!site(f)) continue;
                int ground = DaleTerrain.groundY(sx, sz);
                DaleBuilder builder = new DaleBuilder(level, box, state(era, mx, mz));
                RandomSource random = RandomSource.create(DaleTown.hash(cx, cz, 59) > 0.5 ? cx * 341873128712L + cz : cz * 132897987541L + cx);
                DaleBuildings.house(builder, random, sx, ground, sz, rotation);
            }
        }
    }

    /** Whether a house fits here: all of it in the fields, the ground under it level within a few blocks. */
    private static boolean site(int[] f) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int x = f[0]; x <= f[2]; x += Math.max(1, (f[2] - f[0]) / 3)) {
            for (int z = f[1]; z <= f[3]; z += Math.max(1, (f[3] - f[1]) / 3)) {
                if (DaleTerrain.zone(x, z) != DaleTerrain.Zone.FIELDS) return false;
                int y = DaleTerrain.groundY(x, z);
                min = Math.min(min, y);
                max = Math.max(max, y);
            }
        }
        return max - min <= 3;
    }
}
