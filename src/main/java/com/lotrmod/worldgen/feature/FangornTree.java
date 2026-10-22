package com.lotrmod.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * One of Fangorn's giant trees: a port of the original mod's
 * {@code LOTRWorldGenFangornTrees}. A trunk up to 40 blocks tall and 17 across
 * at the foot, narrowing and leaning as it rises, with boughs spreading all
 * round its top, each carrying side branches that end in balls of leaves hung
 * with vines.
 *
 * <p>The crown reaches some 45 blocks from where the trunk stands, too far for
 * a feature, so the tree is grown by a structure ({@code lotrmod:fangorn_tree})
 * one chunk at a time. That needs the same tree in every chunk, so it is laid
 * out from its seed alone, without looking at the world, and the world is only
 * consulted when each block is written. The original rolled a root's depth
 * each step down and only rolled for a vine on a leaf it had placed; here each
 * is rolled once, up front.
 */
public final class FangornTree {
    private final TreeBlocks blocks = new TreeBlocks();
    private final List<Vine> vines = new ArrayList<>();
    private final List<Root> roots = new ArrayList<>();
    private final BlockState log;
    private final BlockState leaves;
    private final BoundingBox bounds;

    /** A vine hanging from the side of a leaf, as long as there is air below for it. */
    private record Vine(BlockPos top, BlockState state, int length) {}

    /** Wood running down under the trunk's foot until it meets solid ground. */
    private record Root(BlockPos top, int depth) {}

    /**
     * @param log    the trunk
     * @param wood   the boughs and branches, bark on every side
     * @param leaves the leaves
     */
    public FangornTree(BlockPos origin, RandomSource random, BlockState log, BlockState wood, BlockState leaves) {
        this.log = log;
        this.leaves = leaves;
        int i = origin.getX();
        int j = origin.getY();
        int k = origin.getZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        float f = 0.5F + random.nextFloat() * 0.5F;
        int height = (int) (f * 40.0F);
        int trunkRadiusMin = (int) (f * 5.0F);
        int trunkRadiusMax = trunkRadiusMin + 4;
        int xSlope = 4 + random.nextInt(7);
        if (random.nextBoolean()) xSlope = -xSlope;
        int zSlope = 4 + random.nextInt(7);
        if (random.nextBoolean()) zSlope = -zSlope;

        for (int j1 = 0; j1 < height; j1++) {
            int width = trunkRadiusMax - (int) ((float) j1 / height * (trunkRadiusMax - trunkRadiusMin));
            for (int i1 = i - width; i1 <= i + width; i1++) {
                for (int k1 = k - width; k1 <= k + width; k1++) {
                    int i2 = i1 - i;
                    int k2 = k1 - k;
                    if (i2 * i2 + k2 * k2 < width * width) {
                        blocks.log(pos.set(i1, j + j1, k1), log);
                        if (j1 == 0) roots.add(new Root(new BlockPos(i1, j - 1, k1), 6 + random.nextInt(5)));
                    }
                }
            }

            // The trunk leans a block every few blocks up
            if (j1 % xSlope == 0) i += Integer.signum(xSlope);
            if (j1 % zSlope == 0) k += Integer.signum(zSlope);
        }

        int angle = 0;
        while (angle < 360) {
            angle += 10 + random.nextInt(20);
            float angleR = angle / 180.0F * Mth.PI;
            float sin = Mth.sin(angleR);
            float cos = Mth.cos(angleR);
            int boughLength = 12 + random.nextInt(10);
            int boughThickness = Math.round(boughLength / 25.0F * 1.5F);
            int boughBaseHeight = j + Mth.floor(height * (0.9F + random.nextFloat() * 0.1F));
            int boughHeight = 3 + random.nextInt(4);

            for (int l = 0; l < boughLength; l++) {
                int i1 = i + Math.round(sin * l);
                int k1 = k + Math.round(cos * l);
                int j1 = boughBaseHeight + Math.round((float) l / boughLength * boughHeight);
                int range = boughThickness - Math.round((float) l / boughLength * boughThickness * 0.5F);
                for (int i2 = i1 - range; i2 <= i1 + range; i2++) {
                    for (int j2 = j1 - range; j2 <= j1 + range; j2++) {
                        for (int k2 = k1 - range; k2 <= k1 + range; k2++) {
                            blocks.log(pos.set(i2, j2, k2), wood);
                        }
                    }
                }

                float branchAngleR = (angle + random.nextInt(360)) / 180.0F * Mth.PI;
                float branchSin = Mth.sin(branchAngleR);
                float branchCos = Mth.cos(branchAngleR);
                int branchLength = 7 + random.nextInt(6);
                int branchHeight = random.nextInt(6);
                int leafRange = 3;

                for (int l1 = 0; l1 < branchLength; l1++) {
                    int i2 = i1 + Math.round(branchSin * l1);
                    int k2 = k1 + Math.round(branchCos * l1);
                    int j2 = j1 + Math.round((float) l1 / branchLength * branchHeight);
                    for (int j3 = j2; j3 >= j2 - 1; j3--) {
                        blocks.log(pos.set(i2, j3, k2), wood);
                    }

                    if (l1 == branchLength - 1) {
                        for (int i3 = i2 - leafRange; i3 <= i2 + leafRange; i3++) {
                            for (int j3 = j2 - leafRange; j3 <= j2 + leafRange; j3++) {
                                for (int k3 = k2 - leafRange; k3 <= k2 + leafRange; k3++) {
                                    int i4 = i3 - i2;
                                    int j4 = j3 - j2;
                                    int k4 = k3 - k2;
                                    int dist = i4 * i4 + j4 * j4 + k4 * k4;
                                    if (dist < (leafRange - 1) * (leafRange - 1)
                                            || dist < leafRange * leafRange && random.nextInt(3) != 0) {
                                        blocks.leaf(pos.set(i3, j3, k3));
                                        for (Direction side : Direction.Plane.HORIZONTAL) {
                                            if (random.nextInt(40) == 0) {
                                                vines.add(new Vine(pos.relative(side),
                                                        Blocks.VINE.defaultBlockState().setValue(
                                                                VineBlock.getPropertyForFace(side.getOpposite()), true),
                                                        4 + random.nextInt(12)));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        BoundingBox box = blocks.bounds();
        for (Root root : roots) box.encapsulate(root.top().below(root.depth()));
        for (Vine vine : vines) box.encapsulate(vine.top().below(vine.length()));
        this.bounds = box;
    }

    /** Everything the tree might write to. */
    public BoundingBox bounds() {
        return bounds;
    }

    /** The part of the tree inside {@code box}. */
    public void place(WorldGenLevel level, BoundingBox box) {
        blocks.place(level, leaves, box);

        BlockState rootState = log.hasProperty(RotatedPillarBlock.AXIS)
                ? log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y) : log;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (Root root : roots) {
            if (!box.isInside(root.top())) continue;
            pos.set(root.top());
            for (int l = 0; l < root.depth() && pos.getY() > level.getMinBuildHeight(); l++) {
                if (level.getBlockState(pos).isSolidRender(level, pos)) break;
                level.setBlock(pos, rootState, Block.UPDATE_CLIENTS);
                pos.move(Direction.DOWN);
            }
        }

        for (Vine vine : vines) {
            if (!box.isInside(vine.top())) continue;
            pos.set(vine.top());
            for (int l = 0; l <= vine.length() && level.isEmptyBlock(pos); l++) {
                level.setBlock(pos, vine.state(), Block.UPDATE_CLIENTS);
                pos.move(Direction.DOWN);
            }
        }
    }
}
