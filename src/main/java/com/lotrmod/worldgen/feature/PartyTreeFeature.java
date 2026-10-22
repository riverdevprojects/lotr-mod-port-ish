package com.lotrmod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * {@code lotrmod:party_tree} — a port of the original mod's
 * {@code LOTRWorldGenPartyTrees}: a great tree on a trunk three blocks square,
 * with thick boughs spreading all round near the top, each ending in branches
 * that carry balls of leaves, and roots running down into the ground.
 *
 * <pre>{"type": "lotrmod:party_tree", "config": {"log": {...}, "wood": {...}, "leaves": {...}}}</pre>
 *
 * <p>{@code wood} is the bark-on-every-side block the boughs, branches and
 * roots are made of, as the original's log with metadata 12 was.
 *
 * <p>The original's crown reaches up to 21 blocks from the trunk, further than a
 * feature may write from its chunk, so anything beyond the chunks around the
 * tree's own is left off.
 */
public class PartyTreeFeature extends Feature<PartyTreeFeature.Config> {

    public record Config(BlockState log, BlockState wood, BlockState leaves) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockState.CODEC.fieldOf("log").forGetter(Config::log),
                BlockState.CODEC.fieldOf("wood").forGetter(Config::wood),
                BlockState.CODEC.fieldOf("leaves").forGetter(Config::leaves)
        ).apply(instance, Config::new));
    }

    private static final int TRUNK_WIDTH = 1;

    public PartyTreeFeature(Codec<Config> codec) {
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

        int height = random.nextInt(12) + 12;
        if (j < level.getMinBuildHeight() + 1 || j + height + 1 >= level.getMaxBuildHeight()) return false;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int j1 = j; j1 <= j + 1 + height; j1++) {
            int range = j1 == j ? TRUNK_WIDTH : TRUNK_WIDTH + 1;
            for (int i1 = i - range; i1 <= i + range; i1++) {
                for (int k1 = k - range; k1 <= k + range; k1++) {
                    if (!TreeFeature.validTreePos(level, pos.set(i1, j1, k1))) return false;
                }
            }
        }
        for (int i1 = i - TRUNK_WIDTH; i1 <= i + TRUNK_WIDTH; i1++) {
            for (int k1 = k - TRUNK_WIDTH; k1 <= k + TRUNK_WIDTH; k1++) {
                if (!level.getBlockState(pos.set(i1, j - 1, k1)).is(BlockTags.DIRT)) return false;
            }
        }
        for (int i1 = i - TRUNK_WIDTH; i1 <= i + TRUNK_WIDTH; i1++) {
            for (int k1 = k - TRUNK_WIDTH; k1 <= k + TRUNK_WIDTH; k1++) {
                pos.set(i1, j - 1, k1);
                if (!level.getBlockState(pos).is(Blocks.DIRT)) level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
            }
        }

        ChunkPos chunk = new ChunkPos(origin);
        Reach reach = new Reach(chunk.getMinBlockX() - 16, chunk.getMinBlockZ() - 16,
                chunk.getMaxBlockX() + 16, chunk.getMaxBlockZ() + 16);

        TreeBlocks tree = new TreeBlocks();
        for (int j1 = 0; j1 < height; j1++) {
            for (int i1 = i - TRUNK_WIDTH; i1 <= i + TRUNK_WIDTH; i1++) {
                for (int k1 = k - TRUNK_WIDTH; k1 <= k + TRUNK_WIDTH; k1++) {
                    tree.log(pos.set(i1, j + j1, k1), config.log());
                }
            }
        }

        int angle = 0;
        while (angle < 360) {
            angle += 20 + random.nextInt(25);
            float angleR = angle / 180.0F * Mth.PI;
            float sin = Mth.sin(angleR);
            float cos = Mth.cos(angleR);
            int boughLength = 6 + random.nextInt(6);
            int boughThickness = Math.round(boughLength / 20.0F * 1.5F);
            int boughBaseHeight = j + Mth.floor(height * (0.75F + random.nextFloat() * 0.25F));
            int boughHeight = 3 + random.nextInt(4);

            for (int l = 0; l < boughLength; l++) {
                int i1 = i + Math.round(sin * l);
                int k1 = k + Math.round(cos * l);
                int j1 = boughBaseHeight + Math.round((float) l / boughLength * boughHeight);
                int range = boughThickness - Math.round((float) l / boughLength * boughThickness * 0.5F);
                for (int i2 = i1 - range; i2 <= i1 + range; i2++) {
                    for (int j2 = j1 - range; j2 <= j1 + range; j2++) {
                        for (int k2 = k1 - range; k2 <= k1 + range; k2++) {
                            woodIfFree(level, reach, tree, pos.set(i2, j2, k2), config.wood());
                        }
                    }
                }

                float branchAngleR = (angle + random.nextInt(360)) / 180.0F * Mth.PI;
                float branchSin = Mth.sin(branchAngleR);
                float branchCos = Mth.cos(branchAngleR);
                int branchLength = 4 + random.nextInt(4);
                int branchHeight = random.nextInt(5);
                int leafRange = 3;

                for (int l1 = 0; l1 < branchLength; l1++) {
                    int i2 = i1 + Math.round(branchSin * l1);
                    int k2 = k1 + Math.round(branchCos * l1);
                    int j2 = j1 + Math.round((float) l1 / branchLength * branchHeight);
                    for (int j3 = j2; j3 >= j2 - 1; j3--) {
                        woodIfFree(level, reach, tree, pos.set(i2, j3, k2), config.wood());
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
                                        pos.set(i3, j3, k3);
                                        if (reach.contains(pos) && TreeFeature.validTreePos(level, pos)) tree.leaf(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        int roots = 5 + random.nextInt(5);
        for (int l = 0; l < roots; l++) {
            int j1 = j + 1 + random.nextInt(5);
            int xDirection = 0;
            int zDirection = 0;
            int rootLength = 2 + random.nextInt(4);
            int i1;
            int k1;
            if (random.nextBoolean()) {
                if (random.nextBoolean()) {
                    i1 = i - (TRUNK_WIDTH + 1);
                    xDirection = -1;
                } else {
                    i1 = i + TRUNK_WIDTH + 1;
                    xDirection = 1;
                }
                k1 = k - (TRUNK_WIDTH + 1) + random.nextInt(TRUNK_WIDTH * 2 + 2);
            } else {
                if (random.nextBoolean()) {
                    k1 = k - (TRUNK_WIDTH + 1);
                    zDirection = -1;
                } else {
                    k1 = k + TRUNK_WIDTH + 1;
                    zDirection = 1;
                }
                i1 = i - (TRUNK_WIDTH + 1) + random.nextInt(TRUNK_WIDTH * 2 + 2);
            }

            for (int l1 = 0; l1 < rootLength; l1++) {
                int rootBlocks = 0;
                for (int j2 = j1; ; j2--) {
                    pos.set(i1, j2, k1);
                    if (level.getBlockState(pos).isSolidRender(level, pos) || !TreeFeature.validTreePos(level, pos)) break;
                    tree.log(pos, config.wood());
                    BlockPos below = pos.below();
                    if (level.getBlockState(below).is(Blocks.GRASS_BLOCK)) {
                        level.setBlock(below, Blocks.DIRT.defaultBlockState(), 2);
                    }
                    if (++rootBlocks > 5) break;
                }
                j1--;
                if (random.nextBoolean()) {
                    if (xDirection == -1) {
                        i1--;
                    } else if (xDirection == 1) {
                        i1++;
                    } else if (zDirection == -1) {
                        k1--;
                    } else if (zDirection == 1) {
                        k1++;
                    }
                }
            }
        }

        tree.place(level, config.leaves());
        return true;
    }

    /** Wood where the world has room for it, as the original's replaceable-or-leaves check. */
    private static void woodIfFree(WorldGenLevel level, Reach reach, TreeBlocks tree, BlockPos pos, BlockState wood) {
        if (reach.contains(pos) && TreeFeature.validTreePos(level, pos)) tree.log(pos, wood);
    }

    /** The columns a feature placed in one chunk may write to: that chunk and the eight around it. */
    private record Reach(int minX, int minZ, int maxX, int maxZ) {
        boolean contains(BlockPos pos) {
            return pos.getX() >= minX && pos.getX() <= maxX && pos.getZ() >= minZ && pos.getZ() <= maxZ;
        }
    }
}
