package com.lotrmod.worldgen;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;

/**
 * The shape of Arnor, the lost northern kingdom, and which of its three
 * landscapes lies where.
 *
 * <p>One broad noise lays them out. Where it is lowest lie the fields
 * ({@link Zone#FIELDS}): old, empty grassland in long, low swells. In the
 * middle, the hills ({@link Zone#HILLS}): rolling country of broad, rounded
 * hills, with here and there a chain of higher downs running across it, like
 * the Weather Hills or the North Downs where Fornost stood. Where it is
 * highest, the split ground ({@link Zone#SPLIT_GROUND}): a raised, rocky
 * tableland broken open everywhere by rifts. Great ones, ten to thirty blocks
 * across with sheer walls, run down to near the bottom of the world; lesser
 * ones thirty or forty blocks deep and narrow cracks fifteen to twenty-five
 * deep wind between and across them, so the ground is hard going in any
 * direction. Each landscape is a biome of its own
 * ({@link MiddleEarthBiomeSource}).
 *
 * <p>The rifts follow the lines where a noise crosses zero, several noises at
 * different scales crossing each other. They narrow and grow shallow towards
 * the edge of the split ground, and give out before it ends.
 * {@link #groundHeight} is the ground without them, which is what vanilla is
 * told the surface is ({@code "ravines": false} on the terrain blend in
 * {@code initial_density_without_jaggedness}): so the rifts are cut into the
 * land like vanilla's ravines rather than taken for the surface, are left
 * bare rock by the surface rule rather than turfed, and aren't flooded to sea
 * level.
 *
 * <p>{@link TerrainBlendDensityFunction} fades the terrain over to this inside
 * Arnor, by {@link MiddleEarthClimate#arnorWeight}.
 */
public final class ArnorTerrain {
    private ArnorTerrain() {}

    public enum Zone { FIELDS, HILLS, SPLIT_GROUND }

    /** Height the grassland rolls about, in blocks. */
    private static final double BASE = 70.0;
    /** Lowest the ground gets before it is eased back up, so it stays above the waterline. */
    private static final double FLOOR = 65.0;
    private static final double SWELL_HEIGHT = 7.0;
    private static final double ROLL_HEIGHT = 2.5;

    /** Height of the tallest of the rolling hills, and of the knolls on them. */
    private static final double HILL_HEIGHT = 30.0;
    private static final double KNOLL_HEIGHT = 7.0;
    /** Height a chain of downs adds along its crest, on top of the hills it crosses. */
    private static final double DOWNS_HEIGHT = 16.0;
    /** Half the width of a chain of downs, in noise units; about sixty blocks. */
    private static final double DOWNS_WIDTH = 0.09;

    /** How far the split ground is raised above the fields, and how much it rolls. */
    private static final double TABLELAND_HEIGHT = 10.0;
    private static final double TABLELAND_ROLL = 4.0;

    /** Which landscape a place is, on features about this many blocks across. */
    private static final double ZONE_SCALE = 1.0 / 1000.0;
    /** Zone noise below the first is fields; above the second, split ground. */
    private static final double FIELDS_EDGE = -0.15;
    private static final double SPLIT_EDGE = 0.18;
    /** How far either side of an edge, in zone noise, the ground fades from one landscape's to the next. */
    private static final double FADE = 0.08;

    /**
     * The rifts. Great ones: half as wide as this, give or take the
     * variation, their floors about this high (absolute y).
     */
    private static final double GREAT_HALF_WIDTH = 9.0;
    private static final double GREAT_WIDTH_VARIATION = 5.0;
    private static final double GREAT_FLOOR = 12.0;
    /** Lesser rifts, and how deep they cut below the ground. */
    private static final double LESSER_HALF_WIDTH = 4.0;
    private static final double LESSER_WIDTH_VARIATION = 1.5;
    private static final double LESSER_DEPTH = 34.0;
    /** Cracks, and how deep they cut below the ground. */
    private static final double CRACK_HALF_WIDTH = 2.3;
    private static final double CRACK_WIDTH_VARIATION = 1.0;
    private static final double CRACK_DEPTH = 19.0;
    /** Share of a rift's half-width over which its wall drops from the ground to its floor. */
    private static final double WALL = 0.3;
    /** How far a rift's walls wander in and out, in blocks, to leave them jagged. */
    private static final double JAGGED = 1.2;

    private static final PerlinSimplexNoise ZONES =
            new PerlinSimplexNoise(RandomSource.create(73100L), List.of(0, 1, 2));
    private static final PerlinSimplexNoise SWELLS =
            new PerlinSimplexNoise(RandomSource.create(73101L), List.of(0, 1));
    private static final PerlinSimplexNoise ROLLS =
            new PerlinSimplexNoise(RandomSource.create(73102L), List.of(0, 1));
    private static final PerlinSimplexNoise UNEVEN =
            new PerlinSimplexNoise(RandomSource.create(73103L), List.of(0));
    private static final PerlinSimplexNoise HILLS =
            new PerlinSimplexNoise(RandomSource.create(73104L), List.of(0, 1));
    private static final PerlinSimplexNoise KNOLLS =
            new PerlinSimplexNoise(RandomSource.create(73105L), List.of(0));
    private static final PerlinSimplexNoise DOWNS =
            new PerlinSimplexNoise(RandomSource.create(73106L), List.of(0));
    /** Where the chains of downs run at all; elsewhere the hills roll on unbroken. */
    private static final PerlinSimplexNoise DOWNS_MASK =
            new PerlinSimplexNoise(RandomSource.create(73107L), List.of(0));
    private static final PerlinSimplexNoise WANDER =
            new PerlinSimplexNoise(RandomSource.create(73108L), List.of(0));
    private static final PerlinSimplexNoise TABLELAND =
            new PerlinSimplexNoise(RandomSource.create(73109L), List.of(0, 1));

    private static final PerlinSimplexNoise GREAT_A =
            new PerlinSimplexNoise(RandomSource.create(73120L), List.of(0, 1));
    private static final PerlinSimplexNoise GREAT_B =
            new PerlinSimplexNoise(RandomSource.create(73121L), List.of(0, 1));
    private static final PerlinSimplexNoise LESSER =
            new PerlinSimplexNoise(RandomSource.create(73122L), List.of(0, 1));
    private static final PerlinSimplexNoise CRACKS_A =
            new PerlinSimplexNoise(RandomSource.create(73123L), List.of(0, 1));
    private static final PerlinSimplexNoise CRACKS_B =
            new PerlinSimplexNoise(RandomSource.create(73124L), List.of(0, 1));
    private static final PerlinSimplexNoise CRACKS_C =
            new PerlinSimplexNoise(RandomSource.create(73130L), List.of(0, 1));
    /** Where the cracks run; elsewhere, here and there, only the rifts break the ground. */
    private static final PerlinSimplexNoise CRACKS_ON =
            new PerlinSimplexNoise(RandomSource.create(73125L), List.of(0));
    private static final PerlinSimplexNoise WIDTHS =
            new PerlinSimplexNoise(RandomSource.create(73126L), List.of(0));
    private static final PerlinSimplexNoise DEPTHS =
            new PerlinSimplexNoise(RandomSource.create(73127L), List.of(0));
    private static final PerlinSimplexNoise JAGS =
            new PerlinSimplexNoise(RandomSource.create(73128L), List.of(0));
    private static final PerlinSimplexNoise RUBBLE =
            new PerlinSimplexNoise(RandomSource.create(73129L), List.of(0));

    /** Which of Arnor's landscapes this is. */
    public static Zone zone(int x, int z) {
        double zone = zoneNoise(x, z);
        if (zone < FIELDS_EDGE) return Zone.FIELDS;
        if (zone > SPLIT_EDGE) return Zone.SPLIT_GROUND;
        return Zone.HILLS;
    }

    /** Height of Arnor's surface here, in blocks, rifts and all. */
    public static double height(int x, int z) {
        double ground = groundHeight(x, z);
        double split = fade(zoneNoise(x, z) - SPLIT_EDGE);
        return split > 0.0 ? Math.min(ground, rifts(x, z, ground, split)) : ground;
    }

    /** Height of Arnor's ground here, in blocks, leaving out the rifts that split it. */
    public static double groundHeight(int x, int z) {
        double zone = zoneNoise(x, z);
        double swells = SWELLS.getValue(x / 600.0, z / 600.0, false) * SWELL_HEIGHT;
        double rolls = ROLLS.getValue(x / 150.0, z / 150.0, false) * ROLL_HEIGHT;
        double uneven = UNEVEN.getValue(x / 24.0, z / 24.0, false) * 0.7;
        double height = BASE + swells + rolls + uneven;

        double split = fade(zone - SPLIT_EDGE);
        double hills = fade(zone - FIELDS_EDGE) * (1.0 - split);
        if (hills > 0.0) height += (hills(x, z) + downs(x, z)) * hills;
        if (split > 0.0) {
            height += (TABLELAND_HEIGHT + TABLELAND.getValue(x / 90.0, z / 90.0, false) * TABLELAND_ROLL) * split;
        }

        return height < FLOOR ? FLOOR - (FLOOR - height) * 0.3 : height;
    }

    private static double zoneNoise(int x, int z) {
        return ZONES.getValue(x * ZONE_SCALE, z * ZONE_SCALE, false);
    }

    /** 0 a little short of a landscape's edge, through 0.5 on it, to 1 a little past it. */
    private static double fade(double past) {
        double t = Mth.clamp((past + FADE) / (2.0 * FADE), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * Height the rolling hills add here: broad swells of ground rising and
     * falling one after another, with smaller knolls on them.
     */
    private static double hills(int x, int z) {
        double n = HILLS.getValue(x / 300.0, z / 300.0, false) * 0.5 + 0.5;
        double knolls = Math.max(0.0, KNOLLS.getValue(x / 110.0, z / 110.0, false)) * KNOLL_HEIGHT;
        return Math.pow(Mth.clamp(n, 0.0, 1.0), 1.3) * HILL_HEIGHT + knolls;
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

    /**
     * Height the rifts cut the ground down to here, or {@code ground} where
     * there are none; {@code split} is how far into the split ground this is,
     * and narrows and shallows them towards its edge.
     */
    private static double rifts(int x, int z, double ground, double split) {
        double jag = JAGS.getValue(x / 6.0, z / 6.0, false) * JAGGED;
        double width = WIDTHS.getValue(x / 150.0, z / 150.0, false);
        double depth = DEPTHS.getValue(x / 180.0, z / 180.0, false);
        double rubble = RUBBLE.getValue(x / 8.0, z / 8.0, false) * 2.0;
        double cut = ground;

        double greatHalf = (GREAT_HALF_WIDTH + GREAT_WIDTH_VARIATION * width + jag) * split;
        if (greatHalf > 0.5) {
            double share = Math.max(profile(distance(GREAT_A, x, z, 520.0), greatHalf),
                    profile(distance(GREAT_B, x, z, 450.0), greatHalf));
            if (share > 0.0) {
                double floor = Math.max(4.0, GREAT_FLOOR + depth * 8.0 + rubble);
                cut = Math.min(cut, ground - (ground - floor) * split * share);
            }
        }

        double lesserHalf = (LESSER_HALF_WIDTH + LESSER_WIDTH_VARIATION * width + jag) * split;
        if (lesserHalf > 0.5) {
            double share = profile(distance(LESSER, x, z, 240.0), lesserHalf);
            if (share > 0.0) {
                double floorDepth = (LESSER_DEPTH + depth * 8.0 + rubble) * split;
                cut = Math.min(cut, ground - floorDepth * share);
            }
        }

        double on = Mth.clamp((CRACKS_ON.getValue(x / 260.0, z / 260.0, false) + 0.6) / 0.3, 0.0, 1.0);
        double crackHalf = (CRACK_HALF_WIDTH + CRACK_WIDTH_VARIATION * width + jag * 0.5) * split * on;
        if (crackHalf > 0.5) {
            double share = Math.max(profile(distance(CRACKS_A, x, z, 110.0), crackHalf),
                    Math.max(profile(distance(CRACKS_B, x, z, 85.0), crackHalf),
                            profile(distance(CRACKS_C, x, z, 65.0), crackHalf)));
            if (share > 0.0) {
                double floorDepth = (CRACK_DEPTH + depth * 5.0 + rubble) * split * on;
                cut = Math.min(cut, ground - floorDepth * share);
            }
        }
        return cut;
    }

    /** Blocks from here to the nearest line where {@code noise} crosses zero: its value over its slope. */
    private static double distance(PerlinSimplexNoise noise, int x, int z, double scale) {
        double s = noise.getValue(x / scale, z / scale, false);
        double gx = noise.getValue((x + 1) / scale, z / scale, false) - s;
        double gz = noise.getValue(x / scale, (z + 1) / scale, false) - s;
        return Math.abs(s) / Math.max(Math.sqrt(gx * gx + gz * gz), 1e-6);
    }

    /**
     * Share of its depth a rift cuts {@code d} blocks from its middle line:
     * none past its half-width, all of it on its floor, and between them a
     * wall dropping over {@link #WALL} of the half-width, or a block at least.
     */
    private static double profile(double d, double halfWidth) {
        if (d >= halfWidth) return 0.0;
        double t = Mth.clamp((halfWidth - d) / Math.max(1.0, halfWidth * WALL), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }
}
