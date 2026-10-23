package com.lotrmod.worldgen;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;

/**
 * The shape of Rivendell, and which of its five landscapes lies where.
 *
 * <p>Rivendell is a hidden valley. The Bruinen runs down the middle of it from
 * the Misty Mountains in the east and out into Eriador in the west. Along the
 * river lies the valley floor, open meadow ({@link Zone#PLAINS}) giving way
 * to old birch wood ({@link Zone#BIRCH_FOREST}) towards the valley sides. The
 * sides climb in stone terraces, wooded with autumn maple and beech
 * ({@link Zone#MAPLE_FOREST}). Above them a ring of jagged, snow-capped peaks
 * ({@link Zone#PEAKS}) shuts the valley in on every side but the west, with
 * cherry groves ({@link Zone#CHERRY_GROVE}) scattered over their lower slopes.
 * Each landscape is a biome of its own ({@link MiddleEarthBiomeSource}).
 *
 * <p>The valley is laid out around the middle of Rivendell on the region map
 * and scaled to its size ({@link MiddleEarthClimate#rivendellCentreX} and the
 * rest). {@link TerrainBlendDensityFunction} fades the terrain over to this
 * inside Rivendell, by {@link MiddleEarthClimate#rivendellWeight}.
 */
public final class RivendellTerrain {
    private RivendellTerrain() {}

    public enum Zone { PLAINS, BIRCH_FOREST, MAPLE_FOREST, PEAKS, CHERRY_GROVE }

    /*
     * The valley is an ellipse along the river, measured in fractions of
     * Rivendell's radius. Its east end stops short of the mountains; its west
     * end reaches past Rivendell's edge, so the valley opens out into Eriador.
     */
    private static final double VALLEY_HALF_WIDTH = 0.30;
    private static final double VALLEY_EAST = 0.55;
    private static final double VALLEY_WEST = 1.5;
    /** How far the river wanders either side of the valley's middle line. */
    private static final double RIVER_WANDER = 0.07;

    /**
     * Where the terraced sides end and the peaks begin, measured from the
     * valley's middle in valley half-widths: the floor is out to 1.
     */
    private static final double TERRACE_END = 1.8;
    /** How far the peaks' slopes reach out past the terraces before they are at full height. */
    private static final double PEAK_RAMP = 0.6;

    /** Height of the valley floor, rising a little towards its sides. */
    private static final double FLOOR = 66.0;
    private static final double FLOOR_RISE = 5.0;
    /** Lowest dry ground gets before it is eased back up, so it stays above the waterline. */
    private static final double FLOOR_MIN = 64.0;

    /** How high the terraces climb above the edge of the floor, and the height of one terrace. */
    private static final double TERRACE_RISE = 55.0;
    private static final double TERRACE_STEP = 8.0;

    /** How high the peaks' shoulders stand above the top terrace, and how much higher the spires go. */
    private static final double PEAK_SHOULDER = 40.0;
    private static final double PEAK_SPIRES = 75.0;

    /** Below this the peaks' lower slopes can carry cherry groves. */
    private static final double CHERRY_TOP = 170.0;

    /** River bed depth at the middle, below the waterline, and its half-width in blocks. */
    private static final double WATERLINE = 62.0;
    private static final double RIVER_DEPTH = 4.0;
    private static final double RIVER_HALF_WIDTH = 4.0;
    /** Slope of the river banks, in blocks up per block out. */
    private static final double BANK_SLOPE = 0.7;

    private static final PerlinSimplexNoise AXIS =
            new PerlinSimplexNoise(RandomSource.create(81200L), List.of(0, 1));
    private static final PerlinSimplexNoise EDGE =
            new PerlinSimplexNoise(RandomSource.create(81201L), List.of(0, 1));
    private static final PerlinSimplexNoise ROLLS =
            new PerlinSimplexNoise(RandomSource.create(81202L), List.of(0, 1));
    private static final PerlinSimplexNoise UNEVEN =
            new PerlinSimplexNoise(RandomSource.create(81203L), List.of(0));
    private static final PerlinSimplexNoise TERRACE_PHASE =
            new PerlinSimplexNoise(RandomSource.create(81204L), List.of(0, 1));
    private static final PerlinSimplexNoise RIDGES =
            new PerlinSimplexNoise(RandomSource.create(81205L), List.of(0, 1));
    private static final PerlinSimplexNoise SPIRES =
            new PerlinSimplexNoise(RandomSource.create(81206L), List.of(0));
    private static final PerlinSimplexNoise CRAGS =
            new PerlinSimplexNoise(RandomSource.create(81207L), List.of(0));
    private static final PerlinSimplexNoise WOODS =
            new PerlinSimplexNoise(RandomSource.create(81208L), List.of(0, 1));
    private static final PerlinSimplexNoise CHERRIES =
            new PerlinSimplexNoise(RandomSource.create(81209L), List.of(0, 1));
    private static final PerlinSimplexNoise RIVER_WIDTH =
            new PerlinSimplexNoise(RandomSource.create(81210L), List.of(0));

    /** Which of Rivendell's landscapes this is. */
    public static Zone zone(int x, int z) {
        Valley v = valley(x, z);
        if (v.across < 1.0) {
            double woods = WOODS.getValue(x / 150.0, z / 150.0, false) + (v.across - 0.55) * 0.9;
            return woods > 0.0 ? Zone.BIRCH_FOREST : Zone.PLAINS;
        }
        if (v.across < TERRACE_END) return Zone.MAPLE_FOREST;
        boolean cherries = CHERRIES.getValue(x / 90.0, z / 90.0, false) > -0.1;
        return cherries && height(x, z, v) < CHERRY_TOP ? Zone.CHERRY_GROVE : Zone.PEAKS;
    }

    /** Height of Rivendell's surface here, in blocks. */
    public static double height(int x, int z) {
        return height(x, z, valley(x, z));
    }

    /** Whether this is in the river's channel, below the waterline, where it is flooded. */
    public static boolean isRiver(int x, int z) {
        Valley v = valley(x, z);
        return v.across < 1.0 && v.riverDistance < riverHalfWidth(x, z, v);
    }

    private static double height(int x, int z, Valley v) {
        double floorEdge = FLOOR + FLOOR_RISE;
        if (v.across < 1.0) {
            double floor = FLOOR + FLOOR_RISE * v.across * v.across
                    + ROLLS.getValue(x / 90.0, z / 90.0, false) * 2.5
                    + UNEVEN.getValue(x / 18.0, z / 18.0, false) * 0.6;
            if (floor < FLOOR_MIN) floor = FLOOR_MIN - (FLOOR_MIN - floor) * 0.3;
            // Ease the floor into the first terrace
            floor += (floorEdge - floor) * smoothstep(Mth.clamp((v.across - 0.85) / 0.15, 0.0, 1.0));
            return Math.min(floor, river(x, z, v));
        }

        double terraceTop = floorEdge + TERRACE_RISE;
        double t = Math.min(1.0, (v.across - 1.0) / (TERRACE_END - 1.0));
        double raw = floorEdge + t * TERRACE_RISE;
        // Never dipping below the floor's edge, where a step falls short of it
        double terraces = Math.max(floorEdge, terrace(x, z, raw));
        if (v.across < TERRACE_END) return terraces;

        // The peaks rise steeply off the top terrace, and their spires above that
        double p = Math.min(1.0, (v.across - TERRACE_END) / PEAK_RAMP);
        double shoulder = terraceTop + Math.pow(p, 0.8) * PEAK_SHOULDER;
        double ridge = 1.0 - Math.abs(RIDGES.getValue(x / 110.0, z / 110.0, false));
        double spire = 1.0 - Math.abs(SPIRES.getValue(x / 28.0, z / 28.0, false));
        double crags = CRAGS.getValue(x / 6.0, z / 6.0, false);
        double peaks = shoulder
                + p * (Math.pow(ridge, 3.0) * PEAK_SPIRES + Math.pow(spire, 4.0) * 30.0 * ridge + crags * 3.0);
        // The first stretch of slope still shows the terraces' steps
        return Math.max(peaks, terraces);
    }

    /** Ground broken into level ledges with short stone risers between them, the steps wandering a little. */
    private static double terrace(int x, int z, double raw) {
        double phase = TERRACE_PHASE.getValue(x / 60.0, z / 60.0, false) * 0.6;
        double s = raw / TERRACE_STEP + phase;
        double step = Math.floor(s);
        double rise = smoothstep(Mth.clamp((s - step - 0.75) / 0.25, 0.0, 1.0));
        return (step + rise - phase) * TERRACE_STEP;
    }

    /** Height of the river's bed and banks, which only ever cut the floor down. */
    private static double river(int x, int z, Valley v) {
        double halfWidth = riverHalfWidth(x, z, v);
        if (halfWidth <= 0.0) return Double.MAX_VALUE;
        double d = v.riverDistance;
        if (d < halfWidth) {
            double k = d / halfWidth;
            return WATERLINE - RIVER_DEPTH * v.riverStrength * (1.0 - k * k);
        }
        return WATERLINE + (d - halfWidth) * BANK_SLOPE;
    }

    /** Half-width of the river here, narrowing to nothing at its two ends. */
    private static double riverHalfWidth(int x, int z, Valley v) {
        if (v.riverStrength <= 0.0) return 0.0;
        double width = RIVER_HALF_WIDTH + RIVER_WIDTH.getValue(x / 70.0, z / 70.0, false) * 1.5;
        return width * v.riverStrength;
    }

    /**
     * Where a point lies in the valley: {@code across}, from 0 on the valley's
     * middle line to 1 at the edge of its floor and beyond; and
     * {@code riverDistance}, blocks from the river's middle.
     */
    private record Valley(double across, double riverDistance, double riverStrength) {}

    private static Valley valley(int x, int z) {
        double radius = MiddleEarthClimate.rivendellRadius();
        double u = (x - MiddleEarthClimate.rivendellCentreX()) / radius;
        double v = (z - MiddleEarthClimate.rivendellCentreZ()) / radius;
        // The valley's middle line, and the river along it, wander
        double axis = AXIS.getValue(x / 200.0, 0.0, false) * RIVER_WANDER;
        double side = v - axis;
        double length = u > 0.0 ? VALLEY_EAST : VALLEY_WEST;
        double across = Math.sqrt(Mth.square(u / length) + Mth.square(side / VALLEY_HALF_WIDTH));
        // Its edges wander too, so the terraces and peaks don't run in rings
        across *= 1.0 + EDGE.getValue(x / 130.0, z / 130.0, false) * 0.12;

        // The river rises at the valley's east end and runs out west, ending
        // in a pool a little short of Rivendell's edge
        double east = Mth.clamp((VALLEY_EAST * 0.9 - u) / 0.15, 0.0, 1.0);
        double west = Mth.clamp((u + 0.85) / 0.15, 0.0, 1.0);
        double strength = smoothstep(Math.min(east, west));
        return new Valley(across, Math.abs(side) * radius, strength);
    }

    private static double smoothstep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }
}
