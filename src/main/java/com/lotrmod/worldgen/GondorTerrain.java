package com.lotrmod.worldgen;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;

/**
 * The shape of Gondor, and which of its five landscapes lies where.
 *
 * <p>Two broad noises lay Gondor out. One runs from dry, rocky ground to wet,
 * low ground: where it is lowest the land heaps up into hills of white
 * marble ({@link Zone#MARBLE_HILLS}), where it is highest it sinks into
 * reedy wetlands of shallow pools and islands ({@link Zone#WETLANDS}), and it
 * leans wet towards the coast. Between the two the other noise says how wooded
 * the land is: open grassy plains ({@link Zone#PLAINS}), then shrubland with
 * patches of trees ({@link Zone#SHRUBLAND}), then tall forest
 * ({@link Zone#FOREST}). Each landscape is a biome of its own
 * ({@link MiddleEarthBiomeSource}), and the hills and wetlands a ground of
 * their own, faded in across the edge so the land doesn't step.
 *
 * <p>{@link TerrainBlendDensityFunction} fades the terrain over to this inside
 * Gondor, by {@link MiddleEarthClimate#gondorWeight}.
 */
public final class GondorTerrain {
    private GondorTerrain() {}

    public enum Zone { PLAINS, FOREST, SHRUBLAND, MARBLE_HILLS, WETLANDS }

    /** Height the plains roll about, in blocks. */
    private static final double BASE = 67.0;
    /** Lowest dry ground gets before it is eased back up, so it stays above the waterline. */
    private static final double FLOOR = 64.0;

    private static final double SWELL_HEIGHT = 4.0;
    private static final double ROLL_HEIGHT = 2.0;
    /** Tallest a marble hill rises above the plain around it. */
    private static final double HILL_HEIGHT = 34.0;
    /** Height of one of the steps the hills are broken into. */
    private static final double CRAG_STEP = 2.0;

    /**
     * Height the wetlands sit at, on average. The water stands at y 62, so
     * ground at 62 or below is a pool and 63 is level with it: rather more
     * water than land.
     */
    private static final double WET_BASE = 61.5;

    /** Where the land is wet (high) or rocky (low), on features about this many blocks across. */
    private static final double MOISTURE_SCALE = 1.0 / 1300.0;
    /** How wooded the land is, on features about this many blocks across. */
    private static final double WOODS_SCALE = 1.0 / 1000.0;

    /** Moisture above the first is wetland, below the second marble hills. */
    private static final double WET_EDGE = 0.36;
    private static final double ROCK_EDGE = -0.30;
    /**
     * How far short of a zone's edge, in moisture, the wetlands' and hills'
     * ground starts to fade in. It is whole at the edge itself: the noise
     * seldom gets far past it, so a fade inside would leave the wetlands
     * half drained and the hills half sunk.
     */
    private static final double FADE = 0.07;
    /** Woods above these are shrubland, then forest. */
    private static final double SHRUB_EDGE = 0.0;
    private static final double FOREST_EDGE = 0.24;
    /** How much wetter the land is at the coast, falling away over {@link #COAST_WET_BLOCKS}. */
    private static final double COAST_WET = 0.2;
    private static final double COAST_WET_BLOCKS = 900.0;

    private static final PerlinSimplexNoise MOISTURE =
            new PerlinSimplexNoise(RandomSource.create(73100L), List.of(0, 1, 2));
    private static final PerlinSimplexNoise WOODS =
            new PerlinSimplexNoise(RandomSource.create(73101L), List.of(0, 1, 2));
    private static final PerlinSimplexNoise SWELLS =
            new PerlinSimplexNoise(RandomSource.create(73102L), List.of(0, 1));
    private static final PerlinSimplexNoise ROLLS =
            new PerlinSimplexNoise(RandomSource.create(73103L), List.of(0, 1));
    private static final PerlinSimplexNoise UNEVEN =
            new PerlinSimplexNoise(RandomSource.create(73104L), List.of(0));
    private static final PerlinSimplexNoise HILLS =
            new PerlinSimplexNoise(RandomSource.create(73105L), List.of(0, 1, 2));
    private static final PerlinSimplexNoise KNOBS =
            new PerlinSimplexNoise(RandomSource.create(73106L), List.of(0, 1));
    private static final PerlinSimplexNoise ROUGH =
            new PerlinSimplexNoise(RandomSource.create(73109L), List.of(0));
    private static final PerlinSimplexNoise POOLS =
            new PerlinSimplexNoise(RandomSource.create(73107L), List.of(0, 1, 2));
    private static final PerlinSimplexNoise HUMMOCKS =
            new PerlinSimplexNoise(RandomSource.create(73108L), List.of(0, 1));

    /** Which of Gondor's landscapes this is. */
    public static Zone zone(int x, int z) {
        double moisture = moisture(x, z);
        if (moisture > WET_EDGE) return Zone.WETLANDS;
        if (moisture < ROCK_EDGE) return Zone.MARBLE_HILLS;
        double woods = WOODS.getValue(x * WOODS_SCALE, z * WOODS_SCALE, false);
        if (woods > FOREST_EDGE) return Zone.FOREST;
        if (woods > SHRUB_EDGE) return Zone.SHRUBLAND;
        return Zone.PLAINS;
    }

    /** Height of Gondor's surface here, in blocks. */
    public static double height(int x, int z) {
        double moisture = moisture(x, z);
        double height = plains(x, z);
        double hills = fade(ROCK_EDGE - moisture);
        if (hills > 0.0) height += (marbleHills(x, z, height) - height) * hills;
        double wet = wetWeight(moisture);
        if (wet > 0.0) height += (wetlands(x, z) - height) * wet;
        return height;
    }

    /** How much of the ground here is wetland, from 0 to 1: where its pools are flooded. */
    public static double wetWeight(int x, int z) {
        return wetWeight(moisture(x, z));
    }

    private static double wetWeight(double moisture) {
        return fade(moisture - WET_EDGE);
    }

    /** 0 a little short of a zone's edge, rising to 1 on it. */
    private static double fade(double past) {
        double t = Mth.clamp((past + FADE) / FADE, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double moisture(int x, int z) {
        double moisture = MOISTURE.getValue(x * MOISTURE_SCALE, z * MOISTURE_SCALE, false);
        double shore = MiddleEarthClimate.coastDistance(x, z);
        return moisture + COAST_WET * Math.exp(-Math.max(shore, 0.0) / COAST_WET_BLOCKS);
    }

    /** Wide, low, gently rolling grassland. */
    private static double plains(int x, int z) {
        double swells = SWELLS.getValue(x / 450.0, z / 450.0, false) * SWELL_HEIGHT;
        double rolls = ROLLS.getValue(x / 130.0, z / 130.0, false) * ROLL_HEIGHT;
        double uneven = UNEVEN.getValue(x / 22.0, z / 22.0, false) * 0.6;
        double height = BASE + swells + rolls + uneven;
        // Ease the hollows in above the floor rather than flattening them on it
        return height < FLOOR ? FLOOR - (FLOOR - height) * 0.3 : height;
    }

    /**
     * Knolls and crags heaped up out of the plain, grassy hollows between them,
     * broken into rough steps a couple of blocks high so they stand in ledges
     * of bare marble.
     */
    private static double marbleHills(int x, int z, double plain) {
        double n = HILLS.getValue(x / 120.0, z / 120.0, false);
        double t = Mth.clamp((n + 0.1) / 0.8, 0.0, 1.0);
        double mound = Math.pow(t, 1.5) * HILL_HEIGHT;
        double knobs = KNOBS.getValue(x / 26.0, z / 26.0, false) * 5.0 * t;
        double rough = ROUGH.getValue(x / 5.0, z / 5.0, false) * 2.0 * t;
        double height = plain + mound + knobs + rough;

        double step = Math.floor(height / CRAG_STEP);
        double rise = Mth.clamp((height / CRAG_STEP - step - 0.55) / 0.45, 0.0, 1.0);
        double stepped = (step + rise) * CRAG_STEP;
        return height + (stepped - height) * Math.min(1.0, t * 2.0) * 0.7;
    }

    /** Ground held about the waterline, so the dips between hummocks lie under it as pools. */
    private static double wetlands(int x, int z) {
        double pools = POOLS.getValue(x / 64.0, z / 64.0, false) * 2.4;
        double hummocks = HUMMOCKS.getValue(x / 10.0, z / 10.0, false) * 0.8;
        return WET_BASE + pools + hummocks;
    }
}
