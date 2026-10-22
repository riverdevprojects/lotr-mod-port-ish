package com.lotrmod.worldgen.feature;

import com.lotrmod.block.ModBlocks;
import com.mojang.serialization.Codec;
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
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * {@code lotrmod:pine} — a port of the original mod's {@code LOTRWorldGenPine},
 * the tall pine of Mirkwood: a bare trunk 12 to 24 blocks tall, with stub
 * branches low down, under a ragged spire of leaf rings, one to three blocks
 * out, over its upper half.
 */
public class PineFeature extends Feature<NoneFeatureConfiguration> {
    private static final Direction[] BRANCH_DIRS = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};

    public PineFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int i = origin.getX();
        int j = origin.getY();
        int k = origin.getZ();
        int height = Mth.nextInt(random, 12, 24);

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
        tree.leaf(new BlockPos(i, j + height, k));
        leafLayer(level, tree, i, j + height - 1, k, 1);
        int leafHeight = j + height - 3;
        int minLeafHeight = j + (int) (height * 0.5F);
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

        BlockState log = ModBlocks.PINE_LOG.get().defaultBlockState();
        int lastDir = -1;
        for (int j1 = j; j1 < j + height; j1++) {
            tree.log(pos.set(i, j1, k), log);
            if (j1 < j + 3 || j1 >= minLeafHeight || random.nextInt(3) != 0) continue;
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
