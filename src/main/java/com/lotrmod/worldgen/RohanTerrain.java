package com.lotrmod.worldgen;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;

/**
 * The shape of Rohan: wide, rolling grassland. Long swells half a thousand
 * blocks across with gentler rolls on them, now and then a broad, low down
 * rising above the plain, and shallow swales winding between them where the
 * water would run.
 *
 * <p>The swales matter beyond the ground's shape. They are the wettest ground
 * in Rohan ({@link #moisture}), so the grass there is greener and the only
 * scrub and trees in Rohan grow along them, in long dark lines across the
 * gold. {@link TerrainBlendDensityFunction} fades the terrain over to this
 * inside Rohan, by {@link MiddleEarthClimate#rohanWeight}.
 */
public final class RohanTerrain {
    private RohanTerrain() {}

    /** Height the plains roll about, in blocks. */
    private static final double BASE = 72.0;
    /** Lowest the ground gets before it is eased back up, so it stays above the waterline. */
    private static final double FLOOR = 66.0;

    private static final double SWELL_HEIGHT = 9.0;
    private static final double ROLL_HEIGHT = 3.5;
    /** Height of the tallest downs above the plain around them. */
    private static final double DOWN_HEIGHT = 20.0;
    /** How far a swale sinks below the ground either side of it. */
    private static final double SWALE_DEPTH = 3.0;
    /** Half the width of a swale, in noise units; about twenty blocks. */
    private static final double SWALE_WIDTH = 0.08;

    private static final PerlinSimplexNoise SWELLS =
            new PerlinSimplexNoise(RandomSource.create(48120L), List.of(0, 1));
    private static final PerlinSimplexNoise ROLLS =
            new PerlinSimplexNoise(RandomSource.create(48121L), List.of(0, 1));
    private static final PerlinSimplexNoise DOWNS =
            new PerlinSimplexNoise(RandomSource.create(48122L), List.of(0, 1));
    private static final PerlinSimplexNoise SWALES =
            new PerlinSimplexNoise(RandomSource.create(48123L), List.of(0));
    /** Where the swales run at all; elsewhere the plain is unbroken. */
    private static final PerlinSimplexNoise SWALE_MASK =
            new PerlinSimplexNoise(RandomSource.create(48124L), List.of(0));
    private static final PerlinSimplexNoise UNEVEN =
            new PerlinSimplexNoise(RandomSource.create(48125L), List.of(0));
    private static final PerlinSimplexNoise DAMP =
            new PerlinSimplexNoise(RandomSource.create(48126L), List.of(0, 1));

    /** Height of Rohan's surface here, in blocks. */
    public static double height(int x, int z) {
        double swells = SWELLS.getValue(x / 520.0, z / 520.0, false) * SWELL_HEIGHT;
        double rolls = ROLLS.getValue(x / 170.0, z / 170.0, false) * ROLL_HEIGHT;
        double uneven = UNEVEN.getValue(x / 26.0, z / 26.0, false) * 0.7;
        double height = BASE + swells + rolls + downs(x, z) + uneven - swale(x, z) * SWALE_DEPTH;
        // Ease the hollows in above the floor rather than flattening them on it
        return height < FLOOR ? FLOOR - (FLOOR - height) * 0.3 : height;
    }

    /**
     * How wet the ground is here, from about -1 on the dry tops of the swells
     * to 1 in the bottom of a swale: low ground is wetter, and the swales
     * wettest of all. Grass colour and where scrub grows both follow it.
     */
    public static double moisture(int x, int z) {
        double low = -(SWELLS.getValue(x / 520.0, z / 520.0, false) * 0.6
                + ROLLS.getValue(x / 170.0, z / 170.0, false) * 0.4);
        double damp = DAMP.getValue(x / 90.0, z / 90.0, false);
        double dry = Mth.clamp(low * 0.45 + damp * 0.25 - downs(x, z) / DOWN_HEIGHT * 0.5, -1.0, 1.0);
        double swale = swale(x, z);
        return dry + (1.0 - dry) * swale;
    }

    /** 1 along the bottom of a swale, falling to 0 at its edges and beyond. */
    public static double swale(int x, int z) {
        double mask = Mth.clamp((SWALE_MASK.getValue(x / 700.0, z / 700.0, false) + 0.25) * 3.0, 0.0, 1.0);
        if (mask <= 0.0) return 0.0;
        // The zero line of a noise winds like a stream across the plain,
        // wandered a little so its bends aren't all the same
        double wx = x + DAMP.getValue(x / 120.0, z / 120.0, false) * 40.0;
        double wz = z + DAMP.getValue((x + 7000) / 120.0, (z - 7000) / 120.0, false) * 40.0;
        double line = Math.abs(SWALES.getValue(wx / 360.0, wz / 360.0, false));
        double t = Mth.clamp(1.0 - line / SWALE_WIDTH, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t) * mask;
    }

    /** Height a broad, low down adds to the plain here, where one rises. */
    private static double downs(int x, int z) {
        double n = DOWNS.getValue(x / 340.0, z / 340.0, false);
        double t = Mth.clamp((n - 0.3) / 0.6, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t) * DOWN_HEIGHT;
    }
}
