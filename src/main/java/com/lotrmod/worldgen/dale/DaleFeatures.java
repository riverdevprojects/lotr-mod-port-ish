package com.lotrmod.worldgen.dale;

import com.lotrmod.block.DaleBlocks;
import com.lotrmod.worldgen.DaleTerrain;
import com.lotrmod.worldgen.MiddleEarthBiomeSource;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;

/**
 * Dale's features. Each of the first three is placed once a chunk, in every
 * one of Dale's biomes in both eras, and builds what of its part of Dale
 * falls in the chunk, as it is in the era the dimension's Dale is in
 * ({@link MiddleEarthBiomeSource#daleEra}): so the same field, house or
 * orchard tree stands in the same place in both, whole or ruined.
 */
public final class DaleFeatures {
    private DaleFeatures() {}

    static DaleEra era(ChunkGenerator generator) {
        return generator.getBiomeSource() instanceof MiddleEarthBiomeSource source ? source.daleEra() : DaleEra.live();
    }

    static BoundingBox chunkBox(WorldGenLevel level, BlockPos origin) {
        ChunkPos chunk = new ChunkPos(origin);
        return new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
    }

    /**
     * {@code lotrmod:dale_landmarks}: the town of Dale and its roads, bridge
     * and jetty, the Front Gate of Erebor, and the watch-post on Ravenhill.
     */
    public static final class Landmarks extends Feature<NoneFeatureConfiguration> {
        public Landmarks(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            BoundingBox box = chunkBox(level, context.origin());
            DaleEra era = era(context.chunkGenerator());
            DaleTown.build(level, box, era == DaleEra.FLOURISHING ? DaleBuilder.State.INTACT : DaleBuilder.State.BURNT);
            DaleRoads.build(level, box, era);
            DaleGate.build(level, box, era);
            ravenhill(level, box, era);
            return true;
        }

        /**
         * The Dwarves' watch-post on Ravenhill: the original's Dale
         * watchtower on the top of the knoll, its door towards the town,
         * within a low ring of wall. In the Desolation it stands empty and
         * falling in, as Thorin's company found it.
         */
        private static void ravenhill(WorldGenLevel level, BoundingBox box, DaleEra era) {
            int x = DaleTerrain.RAVENHILL_X, z = DaleTerrain.RAVENHILL_Z;
            if (box.maxX() < x - 12 || box.minX() > x + 12 || box.maxZ() < z - 12 || box.minZ() > z + 12) return;
            DaleBuilder.State state = era == DaleEra.FLOURISHING ? DaleBuilder.State.INTACT : DaleBuilder.State.ABANDONED;
            int ground = DaleTerrain.groundY(x, z);
            DaleBuilder ring = new DaleBuilder(level, box, state);
            ring.origin(x, ground, z, 0, 0);
            ring.roll(RandomSource.create(2941L));
            for (int dx = -10; dx <= 10; dx++) {
                for (int dz = -10; dz <= 10; dz++) {
                    double r = Math.sqrt(dx * dx + dz * dz);
                    if (r < 8.5 || r > 9.5) continue;
                    // The gap in the ring faces the path up from Dale, to the east
                    if (dx > 6 && Math.abs(dz) <= 2) continue;
                    int y = DaleTerrain.groundY(x + dx, z + dz) - ground;
                    ring.fillDown(-dx, y, y - 2, dz, ring.brick);
                    ring.set(-dx, y + 1, dz, ring.brick);
                    ring.set(-dx, y + 2, dz, ring.brickWall);
                }
            }
            DaleBuilder tower = new DaleBuilder(level, box, state);
            // Its door east, towards Dale: the spot three blocks east of the middle
            DaleBuildings.watchtower(tower, RandomSource.create(2770L), x + 3, ground, z, 1);
        }
    }

    /** {@code lotrmod:dale_fields}: the patchwork of fields in the vale ({@link DaleFields}). */
    public static final class Fields extends Feature<NoneFeatureConfiguration> {
        public Fields(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            DaleFields.build(context.level(), chunkBox(context.level(), context.origin()), era(context.chunkGenerator()));
            return true;
        }
    }

    /**
     * {@code lotrmod:dale_orchards}: orchards of apple, pear and plum in the
     * meadows, the trees in rows seven blocks apart, where a broad noise
     * says. In the Desolation, where the fire reached, a charred stump stands
     * where each tree stood; further out the orchards are left, half the
     * trees dead and the rest grown shaggy.
     */
    public static final class Orchards extends Feature<NoneFeatureConfiguration> {
        private static final PerlinSimplexNoise ORCHARDS = new PerlinSimplexNoise(RandomSource.create(81101L), List.of(0, 1));
        private static final int SPACING = 7;

        public Orchards(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            BoundingBox box = chunkBox(level, context.origin());
            DaleEra era = era(context.chunkGenerator());
            // Trees whose crowns reach into this chunk, from the rows either side
            for (int gx = Math.floorDiv(box.minX() - 3, SPACING); gx <= Math.floorDiv(box.maxX() + 3, SPACING); gx++) {
                for (int gz = Math.floorDiv(box.minZ() - 3, SPACING); gz <= Math.floorDiv(box.maxZ() + 3, SPACING); gz++) {
                    // Each other row set over by half a space, as orchards are planted
                    int x = gx * SPACING + (Math.floorMod(gz, 2) == 0 ? 0 : SPACING / 2);
                    int z = gz * SPACING;
                    if (!orchard(x, z)) continue;
                    tree(level, box, x, z, era, RandomSource.create(BlockPos.asLong(x, 0, z) ^ 81102L));
                }
            }
            return true;
        }

        private static boolean orchard(int x, int z) {
            return DaleTerrain.zone(x, z) == DaleTerrain.Zone.MEADOWS
                    && ORCHARDS.getValue(x / 260.0, z / 260.0, false) > 0.32;
        }

        private static void tree(WorldGenLevel level, BoundingBox box, int x, int z, DaleEra era, RandomSource random) {
            // Each orchard of one fruit, by its patch
            double kind = DaleTown.hash(x / 90, z / 90, 61);
            Block log = kind < 0.5 ? DaleBlocks.APPLE_LOG.get() : kind < 0.8 ? DaleBlocks.PEAR_LOG.get() : DaleBlocks.PLUM_LOG.get();
            Block leaves = kind < 0.5 ? DaleBlocks.APPLE_LEAVES.get() : kind < 0.8 ? DaleBlocks.PEAR_LEAVES.get() : DaleBlocks.PLUM_LEAVES.get();
            int ground = DaleTerrain.groundY(x, z);
            if (DaleTerrain.riverDistance(x, z) < 14.0) return;
            int trunk = 3 + random.nextInt(2);
            double fate = random.nextDouble();
            if (era == DaleEra.DESOLATION && DaleTerrain.scorched(x, z)) {
                // A charred stump, or nothing
                if (fate < 0.7) {
                    int h = 1 + random.nextInt(2);
                    for (int y = 1; y <= h; y++) set(level, box, x, ground + y, z, DaleBlocks.CHARRED_LOG.get().defaultBlockState());
                }
                return;
            }
            boolean dead = era == DaleEra.DESOLATION && fate < 0.5;
            if (era == DaleEra.DESOLATION && fate > 0.85) return;
            BlockState wood = log.defaultBlockState();
            for (int y = 1; y <= trunk; y++) set(level, box, x, ground + y, z, wood);
            int top = ground + trunk;
            if (dead) {
                // Bare boughs, crooked
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    if (random.nextInt(3) == 0) continue;
                    int len = 1 + random.nextInt(2);
                    for (int s = 1; s <= len; s++) {
                        set(level, box, x + d.getStepX() * s, top + s - 1, z + d.getStepZ() * s,
                                wood.setValue(RotatedPillarBlock.AXIS, d.getAxis()));
                    }
                }
                return;
            }
            // A round crown, fuller where the orchard has gone untended
            int radius = era == DaleEra.DESOLATION ? 3 : 2;
            BlockState leaf = leaves.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
            for (int dy = -1; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        double d = dx * dx + dz * dz + (dy - 0.5) * (dy - 0.5) * 1.6;
                        if (d > radius * radius + 1.2 || d > radius * radius && random.nextInt(3) == 0) continue;
                        BlockPos at = new BlockPos(x + dx, top + dy + 1, z + dz);
                        if (!box.isInside(at)) continue;
                        BlockState here = level.getBlockState(at);
                        if (here.isAir() || here.canBeReplaced()) level.setBlock(at, leaf, 2);
                    }
                }
            }
            set(level, box, x, top + 1, z, wood);
            // Windfalls under the tree
            if (era == DaleEra.FLOURISHING && random.nextInt(3) == 0) {
                set(level, box, x + random.nextInt(5) - 2, ground + 1, z + random.nextInt(5) - 2,
                        Blocks.PINK_PETALS.defaultBlockState());
            }
        }

        private static void set(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!box.isInside(pos)) return;
            BlockState here = level.getBlockState(pos);
            if (here.isAir() || here.canBeReplaced() || here.is(BlockTags.LEAVES)) level.setBlock(pos, state, 2);
        }
    }

    /**
     * {@code lotrmod:charred_tree} and {@code lotrmod:charred_stump}: what
     * Smaug left of a tree, charred black.
     *
     * <p>A tree is a dead trunk five to eleven high, two blocks square one
     * time in five, leaning a little as it rises, its top broken off. Two to
     * four boughs climb out from its upper half, some forking, all snapped
     * short; burnt roots spread at its foot. A stump is one to three blocks of
     * trunk with roots, or now and then a trunk fallen across the ground.
     */
    public static final class CharredTree extends Feature<NoneFeatureConfiguration> {
        private final boolean stump;

        public CharredTree(Codec<NoneFeatureConfiguration> codec, boolean stump) {
            super(codec);
            this.stump = stump;
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos origin = context.origin();
            BlockState below = level.getBlockState(origin.below());
            if (!below.is(BlockTags.DIRT) && !below.is(Blocks.GRAVEL) && !below.is(DaleBlocks.ASH.get())) return false;
            BlockState log = DaleBlocks.CHARRED_LOG.get().defaultBlockState();
            BlockState wood = DaleBlocks.CHARRED_WOOD.get().defaultBlockState();

            if (stump) {
                if (random.nextInt(4) == 0) return fallen(level, random, origin, log);
                int height = 1 + random.nextInt(3);
                for (int y = 0; y < height; y++) put(level, origin.above(y), log);
                roots(level, random, origin, wood);
                return true;
            }

            boolean thick = random.nextInt(5) == 0;
            int height = (thick ? 8 : 5) + random.nextInt(thick ? 5 : 6);
            Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos.MutableBlockPos at = origin.mutable();
            BlockPos[] trunk = new BlockPos[height];
            for (int y = 0; y < height; y++) {
                // Lean a block over once or twice on the way up
                if (y > 2 && random.nextInt(5) == 0) {
                    put(level, at.immutable().relative(lean), wood);
                    at.move(lean);
                }
                trunk[y] = at.immutable();
                if (!put(level, trunk[y], log)) return y > 0;
                if (thick) {
                    put(level, trunk[y].east(), log);
                    put(level, trunk[y].south(), log);
                    put(level, trunk[y].south().east(), log);
                }
                at.move(Direction.UP);
            }
            roots(level, random, origin, wood);
            if (thick) roots(level, random, origin.south().east(), wood);

            // Boughs from the upper half, climbing outwards, some forking
            int boughs = 2 + random.nextInt(3);
            for (int b = 0; b < boughs; b++) {
                Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos from = trunk[height / 2 + random.nextInt(height - height / 2)];
                if (thick) {
                    if (d == Direction.EAST) from = from.east();
                    if (d == Direction.SOUTH) from = from.south();
                }
                bough(level, random, from, d, 2 + random.nextInt(3), log, wood);
            }
            return true;
        }

        /** A bough: out a block, then on out and up, now and then forking off a twig. */
        private static void bough(WorldGenLevel level, RandomSource random, BlockPos from, Direction d, int length,
                BlockState log, BlockState wood) {
            BlockPos.MutableBlockPos at = from.mutable();
            for (int l = 0; l < length; l++) {
                at.move(d);
                if (l > 0 && random.nextBoolean()) at.move(Direction.UP);
                if (!put(level, at, l == 0 ? log.setValue(RotatedPillarBlock.AXIS, d.getAxis()) : wood)) return;
                if (l == 1 && random.nextInt(3) == 0) {
                    Direction side = random.nextBoolean() ? d.getClockWise() : d.getCounterClockWise();
                    put(level, at.immutable().relative(side).above(), wood);
                }
            }
            // A snapped-off tip, pointing up
            if (random.nextBoolean()) put(level, at.immutable().above(), log);
        }

        private static void roots(WorldGenLevel level, RandomSource random, BlockPos origin, BlockState wood) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (random.nextInt(3) != 0) continue;
                BlockPos pos = origin.relative(d);
                if (level.getBlockState(pos).canBeReplaced() && !level.getBlockState(pos.below()).canBeReplaced()) {
                    level.setBlock(pos, wood.setValue(RotatedPillarBlock.AXIS, d.getAxis()), 2);
                }
            }
        }

        /** A burnt trunk lying on the ground, three to six long. */
        private static boolean fallen(WorldGenLevel level, RandomSource random, BlockPos origin, BlockState log) {
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            int length = 3 + random.nextInt(4);
            for (int l = 0; l < length; l++) {
                BlockPos pos = origin.relative(d, l);
                if (level.getBlockState(pos.below()).canBeReplaced()) break;
                if (!put(level, pos, log.setValue(RotatedPillarBlock.AXIS, d.getAxis()))) break;
            }
            return true;
        }

        private static boolean put(WorldGenLevel level, BlockPos pos, BlockState state) {
            BlockState here = level.getBlockState(pos);
            if (!here.canBeReplaced() || !here.getFluidState().isEmpty()) return false;
            level.setBlock(pos, state, 2);
            return true;
        }
    }
}
