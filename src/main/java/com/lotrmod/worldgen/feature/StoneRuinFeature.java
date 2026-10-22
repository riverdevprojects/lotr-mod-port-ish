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
 * {@code lotrmod:stone_ruin} — a port of the original mod's {@code LOTRWorldGenStoneRuin}:
 * two times in three a broken column of brick one to three wide standing
 * alone, and around it a scatter of fallen slabs and stubs of wall one to
 * three blocks high, half buried in the ground.
 *
 * <pre>{"type": "lotrmod:stone_ruin", "config": {"brick": {...}, "slab": {...}, "min_width": 1, "max_width": 3}}</pre>
 */
public class StoneRuinFeature extends Feature<StoneRuinFeature.Config> {

    public record Config(BlockStateProvider brick, BlockStateProvider slab, int minWidth, int maxWidth)
            implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockStateProvider.CODEC.fieldOf("brick").forGetter(Config::brick),
                BlockStateProvider.CODEC.fieldOf("slab").forGetter(Config::slab),
                Codec.intRange(1, 6).fieldOf("min_width").forGetter(Config::minWidth),
                Codec.intRange(1, 6).fieldOf("max_width").forGetter(Config::maxWidth)
        ).apply(instance, Config::new));
    }

    public StoneRuinFeature(Codec<Config> codec) {
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
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int width = Mth.randomBetweenInclusive(random, config.minWidth(), Math.max(config.minWidth(), config.maxWidth()));
        boolean generateColumn = random.nextInt(3) > 0;
        if (generateColumn) {
            int minHeight = 0;
            int maxHeight = 0;
            int columnX = -width / 2;
            int columnZ = -width / 2;
            check:
            for (int i1 = columnX; i1 < columnX + width; i1++) {
                for (int k1 = columnZ; k1 < columnZ + width; k1++) {
                    int j1 = top(level, i + i1, k + k1) - j;
                    minHeight = Math.min(minHeight, j1);
                    maxHeight = Math.max(maxHeight, j1);
                    if (maxHeight - minHeight > 8 || !isSurface(level, pos.set(i + i1, j + j1 - 1, k + k1))) {
                        generateColumn = false;
                        break check;
                    }
                }
            }
            if (generateColumn) {
                int baseHeight = 4 + random.nextInt(4) + random.nextInt(width * 3);
                for (int i1 = columnX; i1 < columnX + width; i1++) {
                    for (int k1 = columnZ; k1 < columnZ + width; k1++) {
                        int height = (int) (baseHeight * (1.0f + random.nextFloat()));
                        for (int j1 = height; j1 >= minHeight; j1--) {
                            pos.set(i + i1, j + j1, k + k1);
                            level.setBlock(pos, config.brick().getState(random, pos), Block.UPDATE_CLIENTS);
                            grassToDirt(level, pos.move(0, -1, 0));
                        }
                    }
                }
            }
        }

        int radius = width * 2;
        int ruinParts = 2 + random.nextInt(4) + random.nextInt(width * 3);
        for (int l = 0; l < ruinParts; l++) {
            int i1 = i + Mth.randomBetweenInclusive(random, -radius * 2, radius * 2);
            int k1 = k + Mth.randomBetweenInclusive(random, -radius * 2, radius * 2);
            int j1 = top(level, i1, k1);
            if (!isSurface(level, pos.set(i1, j1 - 1, k1))) continue;
            int randomFeature = random.nextInt(4);
            boolean flag = true;
            if (randomFeature == 0) {
                if (!isOpaque(level, pos.set(i1, j1, k1))) {
                    level.setBlock(pos, config.slab().getState(random, pos), Block.UPDATE_CLIENTS);
                }
            } else {
                for (int j2 = j1; j2 < j1 + randomFeature && flag; j2++) {
                    flag = !isOpaque(level, pos.set(i1, j2, k1));
                }
                if (flag) {
                    for (int j2 = j1; j2 < j1 + randomFeature; j2++) {
                        pos.set(i1, j2, k1);
                        level.setBlock(pos, config.brick().getState(random, pos), Block.UPDATE_CLIENTS);
                    }
                }
            }
            if (flag) grassToDirt(level, pos.set(i1, j1 - 1, k1));
        }
        return true;
    }

    private static int top(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
    }

    private static boolean isSurface(WorldGenLevel level, BlockPos pos) {
        return (level.getBlockState(pos).is(BlockTags.DIRT) || level.getBlockState(pos).is(BlockTags.SAND))
                && level.getFluidState(pos.above()).isEmpty();
    }

    private static boolean isOpaque(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).isSolidRender(level, pos);
    }

    private static void grassToDirt(WorldGenLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
