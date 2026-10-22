package com.lotrmod.worldgen.feature;

import com.lotrmod.block.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * {@code lotrmod:aspen} — a port of the original mod's {@code LOTRWorldGenAspen}:
 * a tall bare trunk under a narrow, ragged column of leaves, grown as a few
 * random walks out from the trunk on each layer.
 *
 * <pre>{"type": "lotrmod:aspen", "config": {"min_height": 8, "max_height": 15, "extra_trunk": 0}}</pre>
 *
 * <p>{@code extra_trunk} widens the trunk, so 1 is the original's large aspen,
 * two blocks square.
 */
public class AspenFeature extends Feature<AspenFeature.Config> {

    public record Config(int minHeight, int maxHeight, int extraTrunk) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, 64).fieldOf("min_height").forGetter(Config::minHeight),
                Codec.intRange(1, 64).fieldOf("max_height").forGetter(Config::maxHeight),
                Codec.intRange(0, 2).fieldOf("extra_trunk").forGetter(Config::extraTrunk)
        ).apply(instance, Config::new));
    }

    public AspenFeature(Codec<Config> codec) {
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
        int extraTrunk = config.extraTrunk();

        int height = Mth.randomBetweenInclusive(random, config.minHeight(), Math.max(config.minHeight(), config.maxHeight()));
        int leafMin = j + (3 + random.nextInt(3)) - 1;
        int leafTop = j + height + 1;

        if (j < level.getMinBuildHeight() + 1 || leafTop >= level.getMaxBuildHeight()) return false;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int j1 = j; j1 <= leafTop; j1++) {
            int range = j1 == j ? 0 : j1 >= leafMin ? 2 : 1;
            for (int i1 = i - range; i1 <= i + extraTrunk + range; i1++) {
                for (int k1 = k - range; k1 <= k + extraTrunk + range; k1++) {
                    if (!TreeFeature.validTreePos(level, pos.set(i1, j1, k1))) return false;
                }
            }
        }

        for (int i1 = i; i1 <= i + extraTrunk; i1++) {
            for (int k1 = k; k1 <= k + extraTrunk; k1++) {
                if (!level.getBlockState(pos.set(i1, j - 1, k1)).is(BlockTags.DIRT)) return false;
            }
        }
        for (int i1 = i; i1 <= i + extraTrunk; i1++) {
            for (int k1 = k; k1 <= k + extraTrunk; k1++) {
                pos.set(i1, j - 1, k1);
                if (!level.getBlockState(pos).is(Blocks.DIRT)) level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
            }
        }

        TreeBlocks tree = new TreeBlocks();
        for (int j1 = leafMin; j1 <= leafTop; j1++) {
            int leafWidth = 2;
            if (j1 >= leafTop - 1) {
                leafWidth = 0;
            } else if (j1 >= leafTop - 3 || j1 <= leafMin + 1 || random.nextInt(4) == 0) {
                leafWidth = 1;
            }

            int branches = 4 + random.nextInt(5);
            for (int b = 0; b < branches; b++) {
                int i1 = i;
                int k1 = k;
                if (extraTrunk > 0) {
                    i1 += random.nextInt(extraTrunk + 1);
                    k1 += random.nextInt(extraTrunk + 1);
                }
                int i2 = i1;
                int k2 = k1;
                int length = (4 + random.nextInt(8)) * (extraTrunk + 1);
                for (int l = 0; l < length && Math.abs(i2 - i1) <= leafWidth && Math.abs(k2 - k1) <= leafWidth; l++) {
                    pos.set(i2, j1, k2);
                    if (!tree.hasLeaf(pos) && !TreeFeature.validTreePos(level, pos)) break;
                    tree.leaf(pos);
                    switch (random.nextInt(4)) {
                        case 0 -> i2--;
                        case 1 -> i2++;
                        case 2 -> k2--;
                        default -> k2++;
                    }
                }
            }
        }

        BlockState log = ModBlocks.ASPEN_LOG.get().defaultBlockState();
        for (int j1 = j; j1 < j + height; j1++) {
            for (int i1 = i; i1 <= i + extraTrunk; i1++) {
                for (int k1 = k; k1 <= k + extraTrunk; k1++) {
                    tree.log(pos.set(i1, j1, k1), log);
                }
            }
        }

        tree.place(level, ModBlocks.ASPEN_LEAVES.get().defaultBlockState());
        return true;
    }
}
