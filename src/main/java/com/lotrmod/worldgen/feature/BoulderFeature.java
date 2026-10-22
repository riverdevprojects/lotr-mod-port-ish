package com.lotrmod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * {@code lotrmod:boulder} — a port of the original mod's {@code LOTRWorldGenBoulder}:
 * a few rough balls of rock, each settled down onto the ground beneath it, on
 * a patch of grass or dirt no steeper than {@code height_check}.
 *
 * <pre>{"type": "lotrmod:boulder", "config": {"state": {...}, "min_width": 1, "max_width": 3, "height_check": 3}}</pre>
 */
public class BoulderFeature extends Feature<BoulderFeature.Config> {

    public record Config(BlockStateProvider state, int minWidth, int maxWidth, int heightCheck) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockStateProvider.CODEC.fieldOf("state").forGetter(Config::state),
                Codec.intRange(0, 8).fieldOf("min_width").forGetter(Config::minWidth),
                Codec.intRange(0, 8).fieldOf("max_width").forGetter(Config::maxWidth),
                Codec.intRange(0, 16).optionalFieldOf("height_check", 3).forGetter(Config::heightCheck)
        ).apply(instance, Config::new));
    }

    public BoulderFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        Config config = context.config();
        BlockPos origin = context.origin();
        int i = origin.getX();
        int k = origin.getZ();
        int maxWidth = Math.max(config.minWidth(), config.maxWidth());

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        if (!isSurface(level, pos.set(i, origin.getY() - 1, k))) return false;

        // The whole footprint must be natural ground, and fairly level
        int width = Mth.randomBetweenInclusive(random, config.minWidth(), maxWidth);
        int highest = origin.getY();
        int lowest = origin.getY();
        for (int i1 = i - width; i1 <= i + width; i1++) {
            for (int k1 = k - width; k1 <= k + width; k1++) {
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, i1, k1);
                if (!isSurface(level, pos.set(i1, top - 1, k1))) return false;
                highest = Math.max(highest, top);
                lowest = Math.min(lowest, top);
            }
        }
        if (highest - lowest > config.heightCheck()) return false;

        int spheres = 1 + random.nextInt(width + 1);
        for (int l = 0; l < spheres; l++) {
            int i1 = i + Mth.randomBetweenInclusive(random, -width, width);
            int k1 = k + Mth.randomBetweenInclusive(random, -width, width);
            int j1 = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, i1, k1);
            int r = Mth.randomBetweenInclusive(random, config.minWidth(), maxWidth);
            for (int i2 = i1 - r; i2 <= i1 + r; i2++) {
                for (int j2 = j1 - r; j2 <= j1 + r; j2++) {
                    for (int k2 = k1 - r; k2 <= k1 + r; k2++) {
                        int di = i2 - i1;
                        int dj = j2 - j1;
                        int dk = k2 - k1;
                        int dist = di * di + dj * dj + dk * dk;
                        // A solid ball with a ragged one-block shell
                        if (dist >= r * r && (dist >= (r + 1) * (r + 1) || random.nextInt(3) != 0)) continue;

                        // Settle each block onto whatever is under it
                        int j3 = j2;
                        while (j3 > level.getMinBuildHeight() && !level.getBlockState(pos.set(i2, j3 - 1, k2)).isSolidRender(level, pos)) {
                            j3--;
                        }
                        pos.set(i2, j3, k2);
                        level.setBlock(pos, config.state().getState(random, pos), Block.UPDATE_CLIENTS);
                        pos.move(0, -1, 0);
                        if (level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) {
                            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        }
        return true;
    }

    private static boolean isSurface(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.DIRT);
    }
}
