package com.lotrmod.worldgen;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;

/**
 * The shape of Middle-earth's marshes (the Vale of Anduin): ground that never
 * strays more than a block or two from the waterline, so the dips between low
 * hummocks lie just under it and {@link MiddleEarthChunkGenerator} fills them
 * as shallow pools.
 *
 * <p>{@link TerrainBlendDensityFunction} fades the terrain over to this inside
 * marsh regions, by {@link MiddleEarthClimate#marshWeight}.
 */
public final class MarshTerrain {
    private MarshTerrain() {}

    /**
     * Height the ground sits at, on average. The water stands at y 62, so a
     * surface height of 62 or less is a pool and 63 is ground level with it.
     */
    private static final double BASE = 63.0;

    private static final PerlinSimplexNoise POOLS =
            new PerlinSimplexNoise(RandomSource.create(24601L), List.of(0, 1, 2));
    private static final PerlinSimplexNoise HUMMOCKS =
            new PerlinSimplexNoise(RandomSource.create(24602L), List.of(0, 1));

    /** Height of the marsh surface here, in blocks. */
    public static double height(int x, int z) {
        // Broad pools and channels, broken up by small hummocks
        double pools = POOLS.getValue(x / 36.0, z / 36.0, false) * 2.6;
        double hummocks = HUMMOCKS.getValue(x / 9.0, z / 9.0, false) * 0.9;
        return BASE + pools + hummocks;
    }
}
