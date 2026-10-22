package com.lotrmod.worldgen;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;

/**
 * The shape of the Dead Lands, after the original mod's Brown Lands
 * ({@code LOTRBiomeGenBrownLands}, heights 0.2 and 0.2): low, worn ground a
 * few blocks above the water, undulating gently and broken into shallow
 * ledges a block or two high that run along the slopes like contour lines,
 * as the original's 3D noise left gentle ground.
 *
 * <p>{@link TerrainBlendDensityFunction} fades the terrain over to this inside
 * the Dead Lands, by {@link MiddleEarthClimate#deadLandsWeight}.
 */
public final class DeadLandsTerrain {
    private DeadLandsTerrain() {}

    /** Height the ground undulates about, in blocks. */
    private static final double BASE = 70.0;
    /** Lowest the ground gets before it is eased back up, so it stays above the waterline. */
    private static final double FLOOR = 65.0;

    private static final double SWELL_HEIGHT = 6.0;
    private static final double ROLL_HEIGHT = 2.5;
    /** Height of one ledge, in blocks. */
    private static final double LEDGE = 2.0;
    /** How much of the way the ground is pulled onto its ledges: 0 smooth, 1 hard steps. */
    private static final double LEDGE_STRENGTH = 0.75;

    private static final PerlinSimplexNoise SWELLS =
            new PerlinSimplexNoise(RandomSource.create(61200L), List.of(0, 1));
    private static final PerlinSimplexNoise ROLLS =
            new PerlinSimplexNoise(RandomSource.create(61201L), List.of(0, 1));
    private static final PerlinSimplexNoise UNEVEN =
            new PerlinSimplexNoise(RandomSource.create(61202L), List.of(0));
    /** Shifts the ledges up and down so they don't all sit at the same heights. */
    private static final PerlinSimplexNoise LEDGE_SHIFT =
            new PerlinSimplexNoise(RandomSource.create(61203L), List.of(0));

    /** Height of the Dead Lands' surface here, in blocks. */
    public static double height(int x, int z) {
        double swells = SWELLS.getValue(x / 190.0, z / 190.0, false) * SWELL_HEIGHT;
        double rolls = ROLLS.getValue(x / 64.0, z / 64.0, false) * ROLL_HEIGHT;
        double uneven = UNEVEN.getValue(x / 18.0, z / 18.0, false) * 0.8;
        double height = BASE + swells + rolls + uneven;

        // Flat treads with a short riser between them
        double shift = LEDGE_SHIFT.getValue(x / 140.0, z / 140.0, false) * LEDGE;
        double t = (height + shift) / LEDGE;
        double step = Math.floor(t);
        double rise = Mth.clamp((t - step - 0.6) / 0.4, 0.0, 1.0);
        double ledged = (step + rise * rise * (3.0 - 2.0 * rise)) * LEDGE - shift;
        height += (ledged - height) * LEDGE_STRENGTH;

        // Ease the hollows in above the floor rather than flattening them on it
        return height < FLOOR ? FLOOR - (FLOOR - height) * 0.3 : height;
    }
}
