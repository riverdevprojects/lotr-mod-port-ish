package com.lotrmod.worldgen;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;

/**
 * The shape of Arnor, the lost northern kingdom: old, empty grassland worn
 * down into long swells and broad, round-topped hills, with here and there a
 * chain of higher downs running across it, like the Weather Hills or the
 * North Downs where Fornost stood.
 *
 * <p>Everything here is smooth and rounded, as if weathered for an age. The
 * hills rise gently and level off at the top rather than coming to peaks, and
 * the rock only shows through on the highest of them, where the surface rule
 * in {@code noise_settings/middleearth.json} lays it bare.
 *
 * <p>{@link TerrainBlendDensityFunction} fades the terrain over to this inside
 * Arnor, by {@link MiddleEarthClimate#arnorWeight}.
 */
public final class ArnorTerrain {
    private ArnorTerrain() {}

    /** Height the grassland rolls about, in blocks. */
    private static final double BASE = 71.0;
    /** Lowest the ground gets before it is eased back up, so it stays above the waterline. */
    private static final double FLOOR = 65.0;

    private static final double SWELL_HEIGHT = 8.0;
    private static final double ROLL_HEIGHT = 3.0;
    /** Height of the tallest of the round hills above the land around them. */
    private static final double HILL_HEIGHT = 28.0;
    /** Height a chain of downs adds along its crest, on top of any hill it crosses. */
    private static final double DOWNS_HEIGHT = 16.0;
    /** Half the width of a chain of downs, in noise units; about sixty blocks. */
    private static final double DOWNS_WIDTH = 0.09;

    private static final PerlinSimplexNoise SWELLS =
            new PerlinSimplexNoise(RandomSource.create(73100L), List.of(0, 1));
    private static final PerlinSimplexNoise ROLLS =
            new PerlinSimplexNoise(RandomSource.create(73101L), List.of(0, 1));
    private static final PerlinSimplexNoise HILLS =
            new PerlinSimplexNoise(RandomSource.create(73102L), List.of(0, 1));
    private static final PerlinSimplexNoise DOWNS =
            new PerlinSimplexNoise(RandomSource.create(73103L), List.of(0));
    /** Where the chains of downs run at all; elsewhere the grassland goes on unbroken. */
    private static final PerlinSimplexNoise DOWNS_MASK =
            new PerlinSimplexNoise(RandomSource.create(73104L), List.of(0));
    private static final PerlinSimplexNoise WANDER =
            new PerlinSimplexNoise(RandomSource.create(73105L), List.of(0));
    private static final PerlinSimplexNoise UNEVEN =
            new PerlinSimplexNoise(RandomSource.create(73106L), List.of(0));

    /** Height of Arnor's surface here, in blocks. */
    public static double height(int x, int z) {
        double swells = SWELLS.getValue(x / 600.0, z / 600.0, false) * SWELL_HEIGHT;
        double rolls = ROLLS.getValue(x / 150.0, z / 150.0, false) * ROLL_HEIGHT;
        double uneven = UNEVEN.getValue(x / 24.0, z / 24.0, false) * 0.7;
        double height = BASE + swells + rolls + hills(x, z) + downs(x, z) + uneven;
        // Ease the hollows in above the floor rather than flattening them on it
        return height < FLOOR ? FLOOR - (FLOOR - height) * 0.3 : height;
    }

    /**
     * Height a round hill adds here, where one rises. Smoothstepped both ends,
     * so a hill swells gently out of the grass and levels off broad at the top.
     */
    private static double hills(int x, int z) {
        double n = HILLS.getValue(x / 420.0, z / 420.0, false);
        double t = Mth.clamp((n - 0.05) / 0.6, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t) * HILL_HEIGHT;
    }

    /**
     * Height a chain of downs adds here: a long, rounded crest along the line
     * where a noise crosses zero, wandered a little so it doesn't run too
     * straight, in the stretches where another noise lets one run.
     */
    private static double downs(int x, int z) {
        double mask = Mth.clamp((DOWNS_MASK.getValue(x / 900.0, z / 900.0, false) - 0.05) * 3.0, 0.0, 1.0);
        if (mask <= 0.0) return 0.0;
        double wx = x + WANDER.getValue(x / 160.0, z / 160.0, false) * 60.0;
        double wz = z + WANDER.getValue((x - 9000) / 160.0, (z + 9000) / 160.0, false) * 60.0;
        double line = Math.abs(DOWNS.getValue(wx / 700.0, wz / 700.0, false));
        double t = Mth.clamp(1.0 - line / DOWNS_WIDTH, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t) * mask * DOWNS_HEIGHT;
    }
}
