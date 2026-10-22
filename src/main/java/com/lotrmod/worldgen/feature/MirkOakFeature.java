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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code lotrmod:mirk_oak} — a port of the original mod's {@code LOTRWorldGenMirkOak}.
 * A square trunk {@code trunk_width} blocks either side of the centre; a wide
 * one throws boughs out all round its top, each ending in a flat-topped crown
 * that widens as it goes down, and a narrow one carries a single crown. A star
 * of wood props each crown from below, roots spread from the trunk's foot, and
 * vines hang from the leaves.
 *
 * <p>Mirkwood's are gloomier than the original's: {@code leaf_layers} sets how
 * deep each crown is (the original's was 4), leaves droop under its lowest
 * layer half the time rather than a third, and a vine hangs from one leaf side
 * in {@code vine_chance} (the original's was 20).
 *
 * <pre>{"type": "lotrmod:mirk_oak", "config": {"min_height": 12, "max_height": 35,
 *   "trunk_width": 1, "leaf_layers": 5, "vine_chance": 10}}</pre>
 */
public class MirkOakFeature extends Feature<MirkOakFeature.Config> {

    public record Config(int minHeight, int maxHeight, int trunkWidth, int leafLayers, int vineChance)
            implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, 64).fieldOf("min_height").forGetter(Config::minHeight),
                Codec.intRange(1, 64).fieldOf("max_height").forGetter(Config::maxHeight),
                Codec.intRange(0, 2).fieldOf("trunk_width").forGetter(Config::trunkWidth),
                Codec.intRange(1, 6).fieldOf("leaf_layers").forGetter(Config::leafLayers),
                Codec.intRange(1, 1000).fieldOf("vine_chance").forGetter(Config::vineChance)
        ).apply(instance, Config::new));
    }

    /** The original's crown is three blocks across at the top, widening a block each layer down. */
    private static final int CROWN_TOP_RANGE = 3;

    /** A vine hanging from the side of a leaf, as long as there is air below for it. */
    private record Vine(BlockPos top, BlockState state, int length) {}

    public MirkOakFeature(Codec<Config> codec) {
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
        int width = config.trunkWidth();
        int height = Mth.nextInt(random, config.minHeight(), config.maxHeight());

        // Boughs can climb five blocks over the trunk's top, and their crowns six more
        if (j < level.getMinBuildHeight() + 1 || j + height + 11 >= level.getMaxBuildHeight()) return false;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i1 = i - width; i1 <= i + width; i1++) {
            for (int k1 = k - width; k1 <= k + width; k1++) {
                if (!level.getBlockState(pos.set(i1, j - 1, k1)).is(BlockTags.DIRT)) return false;
                for (int j1 = j; j1 < j + height; j1++) {
                    if (!TreeFeature.validTreePos(level, pos.set(i1, j1, k1))) return false;
                }
            }
        }
        for (int i1 = i - width; i1 <= i + width; i1++) {
            for (int k1 = k - width; k1 <= k + width; k1++) {
                pos.set(i1, j - 1, k1);
                if (!level.getBlockState(pos).is(Blocks.DIRT)) level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        BlockState log = ModBlocks.MIRK_OAK_LOG.get().defaultBlockState();
        BlockState wood = ModBlocks.MIRK_OAK_WOOD.get().defaultBlockState();
        TreeBlocks tree = new TreeBlocks();
        List<Vine> vines = new ArrayList<>();

        for (int j1 = 0; j1 < height; j1++) {
            for (int i1 = i - width; i1 <= i + width; i1++) {
                for (int k1 = k - width; k1 <= k + width; k1++) {
                    tree.log(pos.set(i1, j + j1, k1), log);
                }
            }
        }

        if (width >= 1) {
            int deg = 0;
            while (deg < 360) {
                deg += (40 + random.nextInt(30)) / width;
                float angle = (float) Math.toRadians(deg);
                float cos = Mth.cos(angle);
                float sin = Mth.sin(angle);
                float angleY = random.nextFloat() * (float) Math.toRadians(40.0);
                float sinY = Mth.sin(angleY);
                int length = (3 + random.nextInt(6)) * (1 + random.nextInt(width));
                int i1 = i;
                int k1 = k;
                int j1 = j + height - 1 - random.nextInt(5);
                for (int l = 0; l < length; l++) {
                    if (Math.floor(cos * l) != Math.floor(cos * (l - 1))) i1 += (int) Math.signum(cos);
                    if (Math.floor(sin * l) != Math.floor(sin * (l - 1))) k1 += (int) Math.signum(sin);
                    if (Math.floor(sinY * l) != Math.floor(sinY * (l - 1))) j1 += (int) Math.signum(sinY);
                    pos.set(i1, j1, k1);
                    if (!canGrowInto(level, pos)) break;
                    tree.log(pos, wood);
                }
                growCrown(level, random, config, tree, vines, wood, i1, j1, k1);
            }
        } else {
            growCrown(level, random, config, tree, vines, wood, i, j + height - 1, k);
        }

        int roots = 4 + random.nextInt(5 * width + 1);
        for (int l = 0; l < roots; l++) {
            int i1 = i;
            int j1 = j + 1 + random.nextInt(width * 2 + 1);
            int k1 = k;
            int xDirection = 0;
            int zDirection = 0;
            int rootLength = 1 + random.nextInt(4);
            if (random.nextBoolean()) {
                xDirection = random.nextBoolean() ? -1 : 1;
                i1 += xDirection * (width + 1);
                k1 += -(width + 1) + random.nextInt(width * 2 + 2);
            } else {
                zDirection = random.nextBoolean() ? -1 : 1;
                k1 += zDirection * (width + 1);
                i1 += -(width + 1) + random.nextInt(width * 2 + 2);
            }
            for (int l1 = 0; l1 < rootLength; l1++) {
                int rootBlocks = 0;
                pos.set(i1, j1, k1);
                while (rootBlocks <= 5 && canRootInto(level, pos)) {
                    tree.log(pos, wood);
                    rootBlocks++;
                    pos.move(Direction.DOWN);
                }
                j1--;
                if (random.nextBoolean()) {
                    i1 += xDirection;
                    k1 += zDirection;
                }
            }
        }

        tree.place(level, ModBlocks.MIRK_OAK_LEAVES.get().defaultBlockState());

        for (Vine vine : vines) {
            pos.set(vine.top());
            for (int l = 0; l <= vine.length() && level.isEmptyBlock(pos); l++) {
                level.setBlock(pos, vine.state(), Block.UPDATE_CLIENTS);
                pos.move(Direction.DOWN);
            }
        }
        return true;
    }

    /** The original's {@code growLeafCanopy}: a crown of leaves above (i, j, k), and the star of wood holding it up. */
    private static void growCrown(WorldGenLevel level, RandomSource random, Config config, TreeBlocks tree,
            List<Vine> vines, BlockState wood, int i, int j, int k) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int leafTop = j + 5;
        int leafStart = leafTop - config.leafLayers() + 1;
        for (int j1 = leafStart; j1 <= leafTop; j1++) {
            int leafRange = CROWN_TOP_RANGE + (leafTop - j1);
            int leafRangeSq = leafRange * leafRange;
            for (int i1 = i - leafRange; i1 <= i + leafRange; i1++) {
                for (int k1 = k - leafRange; k1 <= k + leafRange; k1++) {
                    int i2 = Math.abs(i1 - i);
                    int k2 = Math.abs(k1 - k);
                    boolean grow = i2 * i2 + k2 * k2 < leafRangeSq;
                    if (i2 == leafRange - 1 || k2 == leafRange - 1) grow &= random.nextInt(4) > 0;
                    if (!grow) continue;

                    // Leaves droop under the lowest layer near the middle
                    int below = j1 == leafStart && i2 <= 3 && k2 <= 3 && random.nextBoolean() ? 1 : 0;
                    for (int j3 = j1; j3 >= j1 - below; j3--) {
                        pos.set(i1, j3, k1);
                        if (!TreeFeature.validTreePos(level, pos)) continue;
                        tree.leaf(pos);
                        for (Direction side : Direction.Plane.HORIZONTAL) {
                            if (random.nextInt(config.vineChance()) == 0) {
                                vines.add(new Vine(pos.relative(side),
                                        ModBlocks.MIRK_VINES.get().defaultBlockState().setValue(
                                                VineBlock.getPropertyForFace(side.getOpposite()), true),
                                        4 + random.nextInt(10)));
                            }
                        }
                    }
                }
            }
        }

        BlockState prop = wood.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        for (int j1 = j; j1 <= j + 2; j1++) {
            int j2 = j1 - j;
            for (int i1 = i - CROWN_TOP_RANGE; i1 <= i + CROWN_TOP_RANGE; i1++) {
                for (int k1 = k - CROWN_TOP_RANGE; k1 <= k + CROWN_TOP_RANGE; k1++) {
                    int i2 = Math.abs(i1 - i);
                    int k2 = Math.abs(k1 - k);
                    boolean centre = i2 == 0 && k2 == 0;
                    boolean diagonal = i2 == k2 && i2 == j2;
                    boolean axis = (i2 == 0 || k2 == 0) && i2 != k2 && (i2 == j2 + 1 || k2 == j2 + 1);
                    if (!centre && !diagonal && !axis) continue;
                    pos.set(i1, j1, k1);
                    if (tree.hasLeaf(pos) || TreeFeature.validTreePos(level, pos)) tree.log(pos, prop);
                }
            }
        }
    }

    /** A bough grows on through air, leaves, plants and other trees' wood, as the original's did. */
    private static boolean canGrowInto(WorldGenLevel level, BlockPos pos) {
        return TreeFeature.validTreePos(level, pos) || level.getBlockState(pos).is(BlockTags.LOGS);
    }

    /** A root runs down until it meets something solid. */
    private static boolean canRootInto(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isSolidRender(level, pos) && state.canBeReplaced() && state.getFluidState().isEmpty();
    }
}
