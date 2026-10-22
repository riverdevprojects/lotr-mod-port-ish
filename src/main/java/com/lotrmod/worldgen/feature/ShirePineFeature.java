package com.lotrmod.worldgen.feature;

import com.lotrmod.block.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
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
 * {@code lotrmod:shire_pine} — a port of the original mod's
 * {@code LOTRWorldGenShirePine}: a tall straight trunk, 10 to 20 blocks, under
 * a stack of narrow leaf tiers that widen and close in again on the way down,
 * with the odd stub branch below the crown.
 */
public class ShirePineFeature extends Feature<NoneFeatureConfiguration> {
    private static final Direction[] BRANCH_DIRS = {Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST};

    public ShirePineFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int x = origin.getX();
        int y = origin.getY();
        int z = origin.getZ();

        int height = 10 + random.nextInt(11);
        int leafHeight = 6 + random.nextInt(4);
        int minLeafHeight = y + height - leafHeight;
        int maxLeafWidth = 2 + random.nextInt(2);

        if (y < level.getMinBuildHeight() + 1 || y + height + 1 >= level.getMaxBuildHeight()) return false;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int j1 = y; j1 <= y + 1 + height; j1++) {
            int range = j1 < minLeafHeight ? 0 : maxLeafWidth;
            for (int i1 = x - range; i1 <= x + range; i1++) {
                for (int k1 = z - range; k1 <= z + range; k1++) {
                    if (!TreeFeature.validTreePos(level, pos.set(i1, j1, k1))) return false;
                }
            }
        }

        BlockPos below = origin.below();
        if (!level.getBlockState(below).is(BlockTags.DIRT)) return false;
        if (!level.getBlockState(below).is(Blocks.DIRT)) level.setBlock(below, Blocks.DIRT.defaultBlockState(), 2);

        TreeBlocks tree = new TreeBlocks();
        int leafWidth = random.nextInt(2);
        int leafWidthLimit = 1;
        int nextLeafWidth = 0;
        for (int j1 = y + height; j1 >= minLeafHeight; j1--) {
            for (int i1 = x - leafWidth; i1 <= x + leafWidth; i1++) {
                for (int k1 = z - leafWidth; k1 <= z + leafWidth; k1++) {
                    boolean corner = leafWidth > 0 && Math.abs(i1 - x) == leafWidth && Math.abs(k1 - z) == leafWidth;
                    if (corner) continue;
                    pos.set(i1, j1, k1);
                    if (TreeFeature.validTreePos(level, pos)) tree.leaf(pos);
                }
            }
            if (leafWidth >= leafWidthLimit) {
                leafWidth = nextLeafWidth;
                nextLeafWidth = 1;
                leafWidthLimit = Math.min(leafWidthLimit + 1, maxLeafWidth);
            } else {
                leafWidth++;
            }
        }

        BlockState log = ModBlocks.SHIRE_PINE_LOG.get().defaultBlockState();
        int lastDir = -1;
        for (int j1 = y; j1 < y + height; j1++) {
            tree.log(pos.set(x, j1, z), log);
            if (j1 < y + 3 || j1 >= minLeafHeight || random.nextInt(3) != 0) continue;
            int dir = random.nextInt(4);
            if (dir == lastDir) continue;
            lastDir = dir;
            Direction branchDir = BRANCH_DIRS[dir];
            BlockPos branch = new BlockPos(x, j1, z).relative(branchDir);
            if (TreeFeature.validTreePos(level, branch)) {
                tree.log(branch, log.setValue(RotatedPillarBlock.AXIS, branchDir.getAxis()));
            }
        }

        tree.place(level, ModBlocks.SHIRE_PINE_LEAVES.get().defaultBlockState());
        return true;
    }
}
