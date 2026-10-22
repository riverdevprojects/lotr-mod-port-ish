package com.lotrmod.worldgen.feature;

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

import java.util.ArrayList;
import java.util.List;

/**
 * {@code lotrmod:poplar} — vanilla's poplar from the dappled forest (Java
 * 26.3), which 1.21.1 doesn't have: its {@code poplar_trunk_placer} and
 * {@code poplar_foliage_placer} with the numbers from its {@code red_poplar}
 * feature. A straight trunk of 7 to 11 with up to four stub branches just
 * under a broad, lopsided diamond of leaves five or six high, its outer edge
 * nibbled by holes.
 *
 * <pre>{"type": "lotrmod:poplar", "config": {"log": {...}, "leaves": {...}}}</pre>
 *
 * <p>The shelf mushrooms vanilla hangs on the trunk are left out, as 1.21.1
 * has no such block.
 */
public class PoplarFeature extends Feature<PoplarFeature.Config> {

    public record Config(BlockState log, BlockState leaves) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockState.CODEC.fieldOf("log").forGetter(Config::log),
                BlockState.CODEC.fieldOf("leaves").forGetter(Config::leaves)
        ).apply(instance, Config::new));
    }

    // red_poplar's trunk_placer and foliage_placer
    private static final int BASE_HEIGHT = 7;
    private static final int HEIGHT_RAND = 4;
    private static final int TRUNK_ABOVE_BRANCHES = 4;
    private static final float SIDE_HOLE_CHANCE = 0.15F;

    public PoplarFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        Config config = context.config();
        BlockPos origin = context.origin();

        int treeHeight = BASE_HEIGHT + random.nextInt(HEIGHT_RAND + 1);
        int branches = Mth.randomBetweenInclusive(random, 1, 4);
        int foliageHeight = Mth.randomBetweenInclusive(random, 5, 6);
        int roll = random.nextInt(12);
        int radius = roll < 5 ? 5 : roll < 10 ? 6 : roll < 11 ? 7 : 8;
        int top = origin.getY() + treeHeight + 1;
        if (origin.getY() < level.getMinBuildHeight() + 1 || top >= level.getMaxBuildHeight()) return false;

        // two_layers_feature_size with upper_size 2: room one block round the
        // foot of the trunk and two blocks round the rest
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = 0; y <= treeHeight + 1; y++) {
            int r = y < 1 ? 1 : 2;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (!TreeFeature.validTreePos(level, pos.setWithOffset(origin, dx, y, dz))) return false;
                }
            }
        }
        BlockPos below = origin.below();
        if (!level.getBlockState(below).is(BlockTags.DIRT)) return false;
        if (!level.getBlockState(below).is(Blocks.DIRT)) level.setBlock(below, Blocks.DIRT.defaultBlockState(), 2);

        // poplar_trunk_placer
        TreeBlocks tree = new TreeBlocks();
        int branchY = treeHeight - TRUNK_ABOVE_BRANCHES;
        for (int y = 0; y < treeHeight; y++) {
            tree.log(pos.setWithOffset(origin, 0, y, 0), config.log());
            List<Direction> directions = shuffledHorizontals(random);
            if (branchY - 1 == y) {
                for (int b = 0; b < branches; b++) {
                    Direction direction = directions.get(b);
                    pos.setWithOffset(origin, direction.getStepX(), y, direction.getStepZ());
                    if (TreeFeature.validTreePos(level, pos)) tree.log(pos, sideways(config.log(), direction.getAxis()));
                }
            }
        }

        // poplar_foliage_placer
        BlockPos foliagePos = origin.above(branchY);
        int r = radius - 1;
        boolean flip = random.nextBoolean();
        leavesRow(level, tree, random, foliagePos, r - 2, foliageHeight - 1, foliageHeight, flip);
        leavesRow(level, tree, random, foliagePos, r - 1, foliageHeight - 2, foliageHeight, flip);
        leavesRow(level, tree, random, foliagePos, r - 1, foliageHeight - 3, foliageHeight, flip);
        for (int y = foliageHeight - 4; y >= 1; y--) {
            leavesRow(level, tree, random, foliagePos, r, y, foliageHeight, flip);
        }
        leavesToLogs(tree, config.log(), foliagePos, r, foliageHeight - 4, foliageHeight, flip);
        leavesRow(level, tree, random, foliagePos, r - 1, 0, foliageHeight, flip);
        leavesRow(level, tree, random, foliagePos, Mth.clamp(r - 2, 1, 2), -1, foliageHeight, flip);

        tree.place(level, config.leaves());
        return true;
    }

    private static void leavesRow(WorldGenLevel level, TreeBlocks tree, RandomSource random, BlockPos origin,
                                  int r, int y, int foliageHeight, boolean flip) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean partial = isPartialRow(foliageHeight, y);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int absDx = Math.abs(dx);
                int absDz = Math.abs(dz);
                if (partial && (absDx == r || absDz == r)) continue;
                int hole = random.nextFloat() <= SIDE_HOLE_CHANCE ? 1 : 0;
                if (!inRhombus(r, absDx, absDz, cornerCut(dx, dz, r, partial, flip), hole)) continue;
                pos.setWithOffset(origin, dx, y, dz);
                if (TreeFeature.validTreePos(level, pos)) tree.leaf(pos);
            }
        }
    }

    /** The boughs inside the crown: leaves along the axes, four or more in from the edge, turned to logs. */
    private static void leavesToLogs(TreeBlocks tree, BlockState log, BlockPos origin,
                                     int r, int y, int foliageHeight, boolean flip) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean partial = isPartialRow(foliageHeight, y);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int absDx = Math.abs(dx);
                int absDz = Math.abs(dz);
                if (inRhombus(r, absDx, absDz, cornerCut(dx, dz, r, partial, flip), 2)
                        && (absDz == 0 && r - absDx >= 4 || absDx == 0 && r - absDz >= 4)) {
                    pos.setWithOffset(origin, dx, y, dz);
                    if (tree.hasLeaf(pos)) tree.log(pos, sideways(log, absDz == 0 ? Direction.Axis.X : Direction.Axis.Z));
                }
            }
        }
    }

    private static int cornerCut(int dx, int dz, int r, boolean partial, boolean flip) {
        boolean smallCorner = flip
                ? dx > 0 && dz > 0 || dz < 0 && dx < 0
                : dx > 0 && dz < 0 || dz > 0 && dx < 0;
        return smallCorner ? r - 1 : partial ? r + 1 : r;
    }

    private static boolean inRhombus(int r, int absDx, int absDz, int cornerCut, int extra) {
        return absDx + absDz <= r * 2 - (cornerCut + extra);
    }

    private static boolean isPartialRow(int foliageHeight, int y) {
        return foliageHeight - 1 == y || foliageHeight - 2 == y;
    }

    private static BlockState sideways(BlockState log, Direction.Axis axis) {
        return log.hasProperty(RotatedPillarBlock.AXIS) ? log.setValue(RotatedPillarBlock.AXIS, axis) : log;
    }

    private static List<Direction> shuffledHorizontals(RandomSource random) {
        List<Direction> directions = new ArrayList<>(List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST));
        for (int i = directions.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            directions.set(i, directions.set(j, directions.get(i)));
        }
        return directions;
    }
}
