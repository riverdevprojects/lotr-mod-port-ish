package com.lotrmod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * {@code lotrmod:copse} — a close stand of trees standing alone in open
 * country, with a floor of its own: the grass under it turned to needle-strewn
 * soil, ferns and the like growing in its shade, and a fallen log or two.
 *
 * <p>The floor is laid first, in a ragged round {@code radius} blocks across,
 * then {@code count} trees go in, crowded towards the middle, then the logs
 * and the undergrowth.
 *
 * <pre>{"type": "lotrmod:copse", "config": {"tree": "...", "count": 6, "radius": 6,
 *   "floor": {...}, "undergrowth": {...}, "fallen_log": "...", "fallen_logs": 1}}</pre>
 */
public class CopseFeature extends Feature<CopseFeature.Config> {

    public record Config(Holder<PlacedFeature> tree, IntProvider count, IntProvider radius,
                         BlockStateProvider floor, BlockStateProvider undergrowth,
                         Holder<PlacedFeature> fallenLog, IntProvider fallenLogs) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PlacedFeature.CODEC.fieldOf("tree").forGetter(Config::tree),
                IntProvider.codec(1, 64).fieldOf("count").forGetter(Config::count),
                IntProvider.codec(1, 12).fieldOf("radius").forGetter(Config::radius),
                BlockStateProvider.CODEC.fieldOf("floor").forGetter(Config::floor),
                BlockStateProvider.CODEC.fieldOf("undergrowth").forGetter(Config::undergrowth),
                PlacedFeature.CODEC.fieldOf("fallen_log").forGetter(Config::fallenLog),
                IntProvider.codec(0, 8).fieldOf("fallen_logs").forGetter(Config::fallenLogs)
        ).apply(instance, Config::new));
    }

    /** Share of the grass under a copse turned to its floor, at the middle. */
    private static final float FLOOR_COVER = 0.85F;

    public CopseFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        ChunkGenerator generator = context.chunkGenerator();
        RandomSource random = context.random();
        Config config = context.config();
        BlockPos origin = context.origin();
        int radius = config.radius().sample(random);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // The floor, thinning out towards a ragged edge
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) + random.nextFloat() * 1.5;
                if (d > radius + 0.5) continue;
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                pos.set(x, ground(level, x, z) - 1, z);
                if (!level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) continue;
                if (random.nextFloat() > FLOOR_COVER * (1.0 - d / (radius + 1.0) * 0.6)) continue;
                level.setBlock(pos, config.floor().getState(random, pos), Block.UPDATE_CLIENTS);
            }
        }

        int count = config.count().sample(random);
        for (int n = 0; n < count; n++) {
            BlockPos at = somewhere(level, random, origin, radius);
            config.tree().value().place(level, generator, random, at);
        }

        int logs = config.fallenLogs().sample(random);
        for (int n = 0; n < logs; n++) {
            config.fallenLog().value().place(level, generator, random, somewhere(level, random, origin, radius));
        }

        for (int n = 0; n < radius * radius; n++) {
            BlockPos at = somewhere(level, random, origin, radius);
            BlockState state = config.undergrowth().getState(random, at);
            if (level.getBlockState(at).isAir() && level.getBlockState(at.below()).is(BlockTags.DIRT)
                    && state.canSurvive(level, at)) {
                level.setBlock(at, state, Block.UPDATE_CLIENTS);
            }
        }
        return true;
    }

    /** A spot on the ground within {@code radius} of {@code origin}, more likely near the middle. */
    private static BlockPos somewhere(WorldGenLevel level, RandomSource random, BlockPos origin, int radius) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = radius * Math.sqrt(random.nextDouble()) * (0.6 + random.nextDouble() * 0.4);
        int x = origin.getX() + Mth.floor(Math.cos(angle) * distance + 0.5);
        int z = origin.getZ() + Mth.floor(Math.sin(angle) * distance + 0.5);
        return new BlockPos(x, ground(level, x, z), z);
    }

    /**
     * The height just above the ground here, looking down past any leaves,
     * logs and plants already standing on it.
     */
    private static int ground(WorldGenLevel level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z), z);
        while (pos.getY() > level.getMinBuildHeight()) {
            BlockState below = level.getBlockState(pos.move(0, -1, 0));
            if (!below.is(BlockTags.LEAVES) && !below.is(BlockTags.LOGS) && !below.canBeReplaced()) {
                return pos.getY() + 1;
            }
        }
        return pos.getY();
    }
}
