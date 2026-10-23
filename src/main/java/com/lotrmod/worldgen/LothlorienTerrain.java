package com.lotrmod.worldgen;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;

/**
 * The shape of Lothlórien, the Golden Wood, and which of its three landscapes
 * lies where.
 *
 * <p>One broad noise says how deep in the wood a place is, leaning deeper
 * towards the middle of Lothlórien on the region map and shallower towards
 * its edges, so the oldest trees gather round its heart. Where it is lowest
 * the trees open out into rolling golden fields with a lone mallorn here and
 * there ({@link Zone#PLAINS}); in the middle stands the wood itself, close
 * golden forest ({@link Zone#FOREST}); and where it is highest, the old growth
 * ({@link Zone#OLD_GROWTH}), towering mallorns far older than the rest, over
 * ground broken into low mossy ledges. Each landscape is a biome of its own
 * ({@link MiddleEarthBiomeSource}).
 *
 * <p>Streams wind through all three, in shallow valleys of their own, and
 * here and there a pool lies in a hollow. Both are cut below the waterline and
 * filled with water by {@link MiddleEarthChunkGenerator}.
 *
 * <p>{@link TerrainBlendDensityFunction} fades the terrain over to this inside
 * Lothlórien, by {@link MiddleEarthClimate#lothlorienWeight}.
 */
public final class LothlorienTerrain {
    private LothlorienTerrain() {}

    public enum Zone { PLAINS, FOREST, OLD_GROWTH }

    /** Height the ground rolls about, in blocks. */
    private static final double BASE = 70.0;
    /** Lowest dry ground gets before it is eased back up, so it stays above the waterline. */
    private static final double FLOOR = 64.0;
    private static final double SWELL_HEIGHT = 6.0;
    private static final double ROLL_HEIGHT = 2.5;
    /** How much higher the fields' hills roll than the wood's ground. */
    private static final double HILL_HEIGHT = 9.0;

    /** Height of one of the old growth's ledges, and how sharply they are cut (1 is a sheer step). */
    private static final double LEDGE_STEP = 3.0;
    private static final double LEDGE_SHARPNESS = 0.75;

    /** The water stands at y 62: ground cut lower than this is a stream or pool. */
    private static final double WATERLINE = 62.0;
    private static final double STREAM_DEPTH = 2.5;
    /** Half-width of a stream in blocks, give or take {@link #STREAM_WIDTH_VARIATION}. */
    private static final double STREAM_HALF_WIDTH = 3.5;
    private static final double STREAM_WIDTH_VARIATION = 1.5;
    /** Slope of a stream's valley sides, in blocks up per block out. */
    private static final double STREAM_BANK = 0.45;
    /** Pools: how far above {@link #POOL_EDGE} the noise must rise for the ground to reach the waterline. */
    private static final double POOL_EDGE = 0.52;
    private static final double POOL_DEPTH_PER_UNIT = 110.0;
    private static final double POOL_RIM = 10.0;

    /** How deep in the wood a place is, on features about this many blocks across. */
    private static final double WOODS_SCALE = 1.0 / 800.0;
    /**
     * How much deeper in the wood the middle of Lothlórien is than the noise
     * alone says, and how much shallower its edge, a radius out.
     */
    private static final double HEART_DEPTH = 0.3;
    private static final double EDGE_DEPTH = 0.2;
    /** Woods below the first are open fields; above the second, old growth. */
    private static final double PLAINS_EDGE = -0.16;
    private static final double OLD_GROWTH_EDGE = 0.2;
    /** How far either side of an edge, in woods, the ground fades from one landscape's to the next. */
    private static final double FADE = 0.08;

    private static final PerlinSimplexNoise WOODS =
            new PerlinSimplexNoise(RandomSource.create(91300L), List.of(0, 1, 2));
    private static final PerlinSimplexNoise SWELLS =
            new PerlinSimplexNoise(RandomSource.create(91301L), List.of(0, 1));
    private static final PerlinSimplexNoise ROLLS =
            new PerlinSimplexNoise(RandomSource.create(91302L), List.of(0, 1));
    private static final PerlinSimplexNoise UNEVEN =
            new PerlinSimplexNoise(RandomSource.create(91303L), List.of(0));
    private static final PerlinSimplexNoise HILLS =
            new PerlinSimplexNoise(RandomSource.create(91304L), List.of(0, 1));
    private static final PerlinSimplexNoise LEDGE_PHASE =
            new PerlinSimplexNoise(RandomSource.create(91305L), List.of(0, 1));
    private static final PerlinSimplexNoise STREAMS =
            new PerlinSimplexNoise(RandomSource.create(91306L), List.of(0, 1));
    private static final PerlinSimplexNoise STREAM_ON =
            new PerlinSimplexNoise(RandomSource.create(91307L), List.of(0));
    private static final PerlinSimplexNoise STREAM_WIDTH =
            new PerlinSimplexNoise(RandomSource.create(91308L), List.of(0));
    private static final PerlinSimplexNoise POOLS =
            new PerlinSimplexNoise(RandomSource.create(91309L), List.of(0, 1));

    /** Which of Lothlórien's landscapes this is. */
    public static Zone zone(int x, int z) {
        double woods = woods(x, z);
        if (woods < PLAINS_EDGE) return Zone.PLAINS;
        if (woods > OLD_GROWTH_EDGE) return Zone.OLD_GROWTH;
        return Zone.FOREST;
    }

    /** Height of Lothlórien's surface here, in blocks. */
    public static double height(int x, int z) {
        double woods = woods(x, z);
        double swells = SWELLS.getValue(x / 420.0, z / 420.0, false) * SWELL_HEIGHT;
        double rolls = ROLLS.getValue(x / 110.0, z / 110.0, false) * ROLL_HEIGHT;
        double uneven = UNEVEN.getValue(x / 20.0, z / 20.0, false) * 0.6;
        double height = BASE + swells + rolls + uneven;

        // The fields roll in broad, low hills
        double fields = fade(PLAINS_EDGE - woods);
        if (fields > 0.0) {
            double hill = Math.max(0.0, HILLS.getValue(x / 260.0, z / 260.0, false) + 0.2);
            height += hill * HILL_HEIGHT * fields;
        }

        // The old growth's ground is broken into low ledges
        double old = fade(woods - OLD_GROWTH_EDGE);
        if (old > 0.0) height += (ledges(x, z, height) - height) * old * LEDGE_SHARPNESS;

        if (height < FLOOR) height = FLOOR - (FLOOR - height) * 0.3;
        return Math.min(height, water(x, z));
    }

    /** Whether this is in a stream or pool, below the waterline, where it is flooded. */
    public static boolean isWater(int x, int z) {
        return water(x, z) < WATERLINE;
    }

    private static double woods(int x, int z) {
        double u = (x - MiddleEarthClimate.lothlorienCentreX()) / MiddleEarthClimate.lothlorienRadius();
        double v = (z - MiddleEarthClimate.lothlorienCentreZ()) / MiddleEarthClimate.lothlorienRadius();
        double heart = HEART_DEPTH - (HEART_DEPTH + EDGE_DEPTH) * Math.min(1.5, u * u + v * v);
        return WOODS.getValue(x * WOODS_SCALE, z * WOODS_SCALE, false) + heart;
    }

    /** 0 a little short of a landscape's edge, through 0.5 on it, to 1 a little past it. */
    private static double fade(double past) {
        double t = Mth.clamp((past + FADE) / (2.0 * FADE), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /** Ground stepped into ledges a few blocks high, the steps wandering a little. */
    private static double ledges(int x, int z, double height) {
        double phase = LEDGE_PHASE.getValue(x / 50.0, z / 50.0, false) * 0.7;
        double s = height / LEDGE_STEP + phase;
        double step = Math.floor(s);
        double t = Mth.clamp((s - step - 0.7) / 0.3, 0.0, 1.0);
        return (step + t * t * (3.0 - 2.0 * t) - phase) * LEDGE_STEP;
    }

    /**
     * Height the streams and pools cut the ground down to here, or no lower
     * than {@link Double#MAX_VALUE} where there are none. Streams follow the
     * lines where a noise crosses zero, in stretches where another allows
     * them; pools lie where a third peaks.
     */
    private static double water(int x, int z) {
        double cut = Double.MAX_VALUE;

        double on = Mth.clamp((STREAM_ON.getValue(x / 700.0, z / 700.0, false) + 0.3) / 0.25, 0.0, 1.0);
        if (on > 0.0) {
            // Blocks from the stream's middle line: the noise over its slope
            double s = STREAMS.getValue(x / 300.0, z / 300.0, false);
            double gx = STREAMS.getValue((x + 1) / 300.0, z / 300.0, false) - s;
            double gz = STREAMS.getValue(x / 300.0, (z + 1) / 300.0, false) - s;
            double d = Math.abs(s) / Math.max(Math.sqrt(gx * gx + gz * gz), 1e-4);
            double strength = on * on * (3.0 - 2.0 * on);
            double halfWidth = (STREAM_HALF_WIDTH + STREAM_WIDTH_VARIATION
                    * STREAM_WIDTH.getValue(x / 80.0, z / 80.0, false)) * strength;
            double bed;
            if (d < halfWidth) {
                double k = d / halfWidth;
                bed = WATERLINE - STREAM_DEPTH * (1.0 - k * k);
            } else {
                bed = WATERLINE + 0.5 + (d - halfWidth) * STREAM_BANK;
            }
            // Where the stream gives out, its valley fades back into the ground
            cut = bed + (1.0 - strength) * 40.0;
        }

        double p = POOLS.getValue(x / 90.0, z / 90.0, false);
        double pool = WATERLINE + POOL_RIM - (p - POOL_EDGE) * POOL_DEPTH_PER_UNIT;
        return Math.min(cut, Math.max(pool, WATERLINE - 4.0));
    }
}
