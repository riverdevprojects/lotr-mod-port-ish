package com.lotrmod.worldgen;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;

/**
 * The shape of Middle-earth's mountain ranges: the heightfield the dimension
 * used before its terrain moved onto vanilla's, kept for the mountains only.
 *
 * <p>A stack of 2D noise layers (a broad baseline, erosion, peaks and valleys,
 * fine detail) plus a ridge layer whose height each range sets with
 * {@link Region#getRidgeHeight()}. The noises are seeded and built in the same
 * order as before, so the ranges come out where they used to.
 *
 * <p>{@link TerrainBlendDensityFunction} fades the terrain over to this inside
 * the ranges; everywhere else the ground is vanilla's.
 */
public final class MountainTerrain {
    private MountainTerrain() {}

    private static final int SEA_LEVEL = 63;

    // Built in this order from one random source, as they always were
    private static final PerlinSimplexNoise CONTINENTAL;
    private static final PerlinSimplexNoise EROSION;
    private static final PerlinSimplexNoise PEAKS_VALLEYS;
    private static final PerlinSimplexNoise DETAIL;
    private static final PerlinSimplexNoise RIDGE;

    static {
        RandomSource random = RandomSource.create(12345L);
        CONTINENTAL = new PerlinSimplexNoise(random, List.of(0, 1, 2, 3, 4));
        EROSION = new PerlinSimplexNoise(random, List.of(0, 1, 2, 3));
        PEAKS_VALLEYS = new PerlinSimplexNoise(random, List.of(0, 1, 2, 3));
        DETAIL = new PerlinSimplexNoise(random, List.of(0, 1, 2));
        RIDGE = new PerlinSimplexNoise(random, List.of(0, 1, 2, 3));
    }

    // How strongly each layer applies, in blocks
    private static final double BASE_OFFSET = 25.0;
    private static final double CONTINENT_SCALE = 30.0;
    private static final double EROSION_SCALE = 10.0;
    private static final double PEAKS_VALLEYS_SCALE = 40.0;
    private static final double DETAIL_SCALE = 8.0;
    /** Extra height for ground clear of the coast. */
    private static final double INLAND_BOOST = 8.0;
    /** Blocks inland over which the ground eases down to sea level at the coast. */
    private static final double COAST_RAMP = 128.0;

    /** Height of the mountain surface here, in blocks. */
    public static double height(int x, int z) {
        double continental = (CONTINENTAL.getValue(x / 1400.0, z / 1400.0, false) + 1.0) / 2.0;
        double erosion = (EROSION.getValue(x / 500.0, z / 500.0, false) + 1.0) / 2.0;
        double peaksValleys = PEAKS_VALLEYS.getValue(x / 180.0, z / 180.0, false);
        // Gentle valleys, sharper peaks
        peaksValleys *= peaksValleys < 0 ? 0.5 : 1.2;
        double detail = DETAIL.getValue(x / 45.0, z / 45.0, false);
        double ridge = 1.0 - Math.abs(RIDGE.getValue(x / 220.0, z / 220.0, false));
        ridge *= ridge;

        double height = SEA_LEVEL + INLAND_BOOST
                + BASE_OFFSET
                + continental * CONTINENT_SCALE
                + (1.0 - erosion) * EROSION_SCALE
                + peaksValleys * PEAKS_VALLEYS_SCALE
                + ridge * MiddleEarthClimate.ridgeHeight(x, z)
                + detail * DETAIL_SCALE;

        double shore = Math.max(0.0, MiddleEarthClimate.coastDistance(x, z));
        double t = Math.min(1.0, shore / COAST_RAMP);
        t = t * t * (3.0 - 2.0 * t);
        return Math.max(SEA_LEVEL, SEA_LEVEL + (height - SEA_LEVEL) * t);
    }
}
