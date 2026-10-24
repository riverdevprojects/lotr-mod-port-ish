package com.lotrmod.worldgen.feature;

import com.lotrmod.block.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * {@code lotrmod:pine} — a port of the original mod's {@code LOTRWorldGenPine},
 * the tall pine of Mirkwood: a bare trunk 12 to 24 blocks tall, with stub
 * branches low down, under a ragged spire of leaf rings, one to three blocks
 * out, over its upper half.
 *
 * <p>With its defaults ({@code "config": {}}) it is the original's. Its
 * config can make a fuller tree for open country: {@code bare} sets how much
 * of the trunk is bare below the crown, {@code branches} whether it has its
 * stubs, and {@code full} gives it a crown of one ragged cone instead of the
 * original's stack of plates: a ring of needles at every height, widening
 * steadily from the tip down to two or three blocks out, every other ring
 * tucked in a block, their edges left gappy.
 *
 * <pre>{"type": "lotrmod:pine", "config": {"min_height": 12, "max_height": 24, "bare": 0.5, "branches": true, "full": false}}</pre>
 */
public class PineFeature extends Feature<PineFeature.Config> {
    private static final Direction[] BRANCH_DIRS = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};

    public record Config(int minHeight, int maxHeight, float bare, boolean branches, boolean full)
            implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(4, 40).optionalFieldOf("min_height", 12).forGetter(Config::minHeight),
                Codec.intRange(4, 40).optionalFieldOf("max_height", 24).forGetter(Config::maxHeight),
                Codec.floatRange(0.0F, 0.9F).optionalFieldOf("bare", 0.5F).forGetter(Config::bare),
                Codec.BOOL.optionalFieldOf("branches", true).forGetter(Config::branches),
                Codec.BOOL.optionalFieldOf("full", false).forGetter(Config::full)
        ).apply(instance, Config::new));
    }

    public PineFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        Config config = context.config();
        BlockPos origin = context.origin();
        int i = origin.getX();
        int j = origin.getY();
        int k = origin.getZ();
        int height = Mth.nextInt(random, config.minHeight(), Math.max(config.minHeight(), config.maxHeight()));

        if (j < level.getMinBuildHeight() + 1 || j + height + 1 >= level.getMaxBuildHeight()) return false;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int j1 = j; j1 <= j + height + 1; j1++) {
            int range = j1 == j ? 0 : j1 >= j + height - 1 ? 2 : 1;
            for (int i1 = i - range; i1 <= i + range; i1++) {
                for (int k1 = k - range; k1 <= k + range; k1++) {
                    if (!TreeFeature.validTreePos(level, pos.set(i1, j1, k1))) return false;
                }
            }
        }

        BlockPos below = origin.below();
        if (!level.getBlockState(below).is(BlockTags.DIRT)) return false;
        if (!level.getBlockState(below).is(Blocks.DIRT)) level.setBlock(below, Blocks.DIRT.defaultBlockState(), 2);

        TreeBlocks tree = new TreeBlocks();
        int minLeafHeight = j + (int) (height * config.bare());
        if (config.full()) {
            cone(level, tree, random, i, j + height, k, minLeafHeight);
        } else {
            plates(level, tree, random, i, j, k, height, minLeafHeight);
        }

        BlockState log = ModBlocks.PINE_LOG.get().defaultBlockState();
        int lastDir = -1;
        for (int j1 = j; j1 < j + height; j1++) {
            tree.log(pos.set(i, j1, k), log);
            if (!config.branches() || j1 < j + 3 || j1 >= minLeafHeight || random.nextInt(3) != 0) continue;
            int dir = random.nextInt(4);
            if (dir == lastDir) continue;
            lastDir = dir;
            Direction branchDir = BRANCH_DIRS[dir];
            BlockPos branch = new BlockPos(i, j1, k).relative(branchDir);
            if (TreeFeature.validTreePos(level, branch)) {
                tree.log(branch, log.setValue(RotatedPillarBlock.AXIS, branchDir.getAxis()));
            }
        }

        tree.place(level, ModBlocks.PINE_LEAVES.get().defaultBlockState());
        return true;
    }

    /** The original's crown: a ragged spire of leaf rings, one to three blocks out, down to {@code minLeafHeight}. */
    private static void plates(WorldGenLevel level, TreeBlocks tree, RandomSource random, int i, int j, int k,
                               int height, int minLeafHeight) {
        tree.leaf(new BlockPos(i, j + height, k));
        leafLayer(level, tree, i, j + height - 1, k, 1);
        int leafHeight = j + height - 3;
        while (leafHeight > minLeafHeight) {
            int r = random.nextInt(3);
            if (r == 0) {
                leafLayer(level, tree, i, leafHeight, k, 1);
                leafHeight -= 2;
            } else {
                // A ring two or three out, between narrower ones
                int wide = r == 1 ? 2 : 3;
                leafHeight--;
                leafLayer(level, tree, i, leafHeight + 1, k, wide - 1);
                leafLayer(level, tree, i, leafHeight, k, wide);
                leafLayer(level, tree, i, leafHeight - 1, k, wide - 1);
                leafHeight -= 3;
            }
        }
        leafLayer(level, tree, i, leafHeight, k, 1);
    }

    /**
     * A crown of one ragged cone from just above the trunk's {@code top} down
     * to {@code minLeafHeight}: round rings, widening quickly below the tip and
     * then more slowly, to about three blocks out at the foot, every other one
     * tucked in a little, the lowest drawn in to round off its foot, and the
     * leaves on their edges left out one time in four.
     */
    private static void cone(WorldGenLevel level, TreeBlocks tree, RandomSource random, int i, int top, int k,
                             int minLeafHeight) {
        int span = Math.max(1, top - minLeafHeight);
        double widest = span >= 9 ? 2.6 : 1.9;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        tree.leaf(new BlockPos(i, top, k));
        for (int y = top - 1; y >= minLeafHeight; y--) {
            double r = 0.6 + widest * Math.sqrt((double) (top - y) / span);
            if ((top - y) % 2 == 0) r -= 0.6;
            if (y == minLeafHeight) r -= 1.0;
            r = Math.max(1.0, r);
            int reach = Mth.ceil(r);
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > r + 0.3) continue;
                    if (d > r - 0.7 && d > 1.0 && random.nextInt(4) == 0) continue;
                    if (TreeFeature.validTreePos(level, pos.set(i + dx, y, k + dz))) tree.leaf(pos);
                }
            }
        }
    }

    /** A diamond of leaves {@code range} blocks out from the trunk. */
    private static void leafLayer(WorldGenLevel level, TreeBlocks tree, int i, int j, int k, int range) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i1 = i - range; i1 <= i + range; i1++) {
            for (int k1 = k - range; k1 <= k + range; k1++) {
                if (Math.abs(i1 - i) + Math.abs(k1 - k) > range) continue;
                if (TreeFeature.validTreePos(level, pos.set(i1, j, k1))) tree.leaf(pos);
            }
        }
    }
}
