package com.lotrmod.worldgen;

/**
 * The cross-section of a river from {@link RiverMap}: a channel below sea level
 * that {@link MiddleEarthChunkGenerator} fills with water, a low bank just above
 * the waterline, and a valley either side that eases back up into the land
 * around.
 *
 * <p>{@link TerrainBlendDensityFunction} lowers the ground toward this shape;
 * it never raises it, so a river running into a lake or the sea leaves the
 * water as deep as it was.
 */
public final class RiverTerrain {
    private RiverTerrain() {}

    private static final double SEA_LEVEL = 63.0;
    /** Width of the bank between the water and the valley sides. */
    private static final double BANK = 6.0;
    /** Height of the bank and valley floor; the bank rises to it from the waterline. */
    private static final double BANK_HEIGHT = SEA_LEVEL + 1.5;
    /** Channel depth at the centre, in blocks: a base plus a share of the half-width. */
    private static final double DEPTH_BASE = 2.0;
    private static final double DEPTH_PER_HALF_WIDTH = 0.2;
    /** The valley either side reaches this far, plus this many times the river's width. */
    private static final double VALLEY_BASE = 48.0;
    private static final double VALLEY_PER_WIDTH = 1.0;

    /** How much of the ground here takes the river's shape, from 1 by the water to 0 beyond its valley. */
    public static double weight(RiverMap.Sample river) {
        double outside = river.distance() - river.width() / 2.0 - BANK;
        if (outside <= 0.0) return 1.0;
        double t = Math.min(1.0, outside / (VALLEY_BASE + VALLEY_PER_WIDTH * river.width()));
        return 1.0 - t * t * (3.0 - 2.0 * t);
    }

    /** Height of the river's ground here: the channel bed, the bank or the valley floor. */
    public static double height(RiverMap.Sample river) {
        double halfWidth = river.width() / 2.0;
        double d = river.distance();
        if (d < halfWidth) {
            double u = d / halfWidth;
            return SEA_LEVEL - (DEPTH_BASE + DEPTH_PER_HALF_WIDTH * halfWidth) * (1.0 - u * u);
        }
        return SEA_LEVEL + (BANK_HEIGHT - SEA_LEVEL) * Math.min(1.0, (d - halfWidth) / BANK);
    }

    /** Whether the river's water reaches here: the channel and its bank. */
    public static boolean isWater(RiverMap.Sample river) {
        return river.distance() < river.width() / 2.0 + BANK;
    }

    /** Whether this is the river itself, for the biome: the water and its edge. */
    public static boolean inChannel(RiverMap.Sample river) {
        return river.distance() < river.width() / 2.0 + 1.5;
    }
}
