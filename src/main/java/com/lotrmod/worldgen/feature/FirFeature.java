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
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * {@code lotrmod:fir} — a port of the original mod's {@code LOTRWorldGenFir},
 * the fir of its Iron Hills: a straight bare trunk, 6 to 13 blocks by
 * default, topped by a small cone of needles seven layers deep, widening a
 * block every two layers down to three out, that rises two blocks above the
 * trunk's top.
 *
 * <pre>{"type": "lotrmod:fir", "config": {"min_height": 6, "max_height": 13}}</pre>
 */
public class FirFeature extends Feature<FirFeature.Config> {

    public record Config(int minHeight, int maxHeight) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, 40).optionalFieldOf("min_height", 6).forGetter(Config::minHeight),
                Codec.intRange(1, 40).optionalFieldOf("max_height", 13).forGetter(Config::maxHeight)
        ).apply(instance, Config::new));
    }

    private static final int LEAF_LAYERS = 3;

    public FirFeature(Codec<Config> codec) {
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
        int height = Mth.randomBetweenInclusive(random, config.minHeight(), Math.max(config.minHeight(), config.maxHeight()));

        if (j < level.getMinBuildHeight() + 1 || j + height + 2 >= level.getMaxBuildHeight()) return false;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int j1 = j; j1 <= j + height + 2; j1++) {
            int range = j1 == j ? 0 : j1 >= j + height - 1 ? 2 : 1;
            for (int i1 = i - range; i1 <= i + range; i1++) {
                for (int k1 = k - range; k1 <= k + range; k1++) {
                    if (!TreeFeature.validTreePos(level, pos.set(i1, j1, k1))) return false;
                }
            }
        }

        BlockPos below = origin.below();
        if (!level.getBlockState(below).is(BlockTags.DIRT)) return false;
        if (level.getBlockState(below).is(Blocks.GRASS_BLOCK)) level.setBlock(below, Blocks.DIRT.defaultBlockState(), 2);

        TreeBlocks tree = new TreeBlocks();
        int leafLevel = j + height + 2;
        for (int l = 0; l <= LEAF_LAYERS * 2; l++) {
            int leafRange = l / 2;
            for (int i1 = i - leafRange; i1 <= i + leafRange; i1++) {
                for (int k1 = k - leafRange; k1 <= k + leafRange; k1++) {
                    if (Math.abs(i1 - i) + Math.abs(k1 - k) > leafRange) continue;
                    if (TreeFeature.validTreePos(level, pos.set(i1, leafLevel, k1))) tree.leaf(pos);
                }
            }
            leafLevel--;
        }

        for (int j1 = 0; j1 < height; j1++) {
            tree.log(pos.set(i, j + j1, k), ModBlocks.FIR_LOG.get().defaultBlockState());
        }

        tree.place(level, ModBlocks.FIR_LEAVES.get().defaultBlockState());
        return true;
    }
}
