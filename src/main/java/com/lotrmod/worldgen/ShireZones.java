package com.lotrmod.worldgen;

import com.lotrmod.LOTRMod;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.ArrayList;
import java.util.List;

/**
 * The Shire's rings, from its heart out into Eriador, as the original mod laid
 * them out:
 *
 * <ul>
 *   <li>{@link Zone#HEART} — open farmland and downs with scattered trees,
 *       and woody patches filling the gaps: where the hobbits live.</li>
 *   <li>{@link Zone#OUTSKIRTS} — a band just inside the Shire's border, all
 *       woodland: aspen stands and thick mixed woods, no open ground.</li>
 *   <li>{@link Zone#WOODLANDS} — a band of Eriador and Arnor around the
 *       Shire: the Shire Woodlands, thick with shire pines and aspens.</li>
 * </ul>
 *
 * <p>Zones are decided by distance from the Shire's border on the region map,
 * pushed in and out by noise so the rings have ragged edges rather than
 * following the border's outline at a fixed depth.
 */
public final class ShireZones {
    private ShireZones() {}

    public enum Zone { HEART, OUTSKIRTS, WOODLANDS, NONE }

    /** How deep the outskirts reach into the Shire, in blocks, give or take the noise. */
    private static final double OUTSKIRTS_DEPTH = 300.0;
    /** How far the Shire Woodlands reach out into Eriador, in blocks, give or take the noise. */
    private static final double WOODLANDS_REACH = 480.0;
    /** How far the noise moves a ring's edge either way, in blocks. */
    private static final double EDGE_WANDER = 220.0;

    /** Map pixels of margin kept around the Shire, enough to cover the woodlands. */
    private static final int MARGIN = 64;

    private static final PerlinSimplexNoise WANDER =
            new PerlinSimplexNoise(RandomSource.create(55501L), List.of(0, 1, 2));

    /** Signed distance to the border in map pixels around the Shire, + inside. Null until built. */
    private static volatile Field field;

    private record Field(int x0, int z0, int width, int height, float[] distance) {}

    /** Rebuilds the distance field from the region map. Call after the maps (re)load. */
    public static void rebuild() {
        if (!RegionMapLoader.isLoaded()) {
            field = null;
            return;
        }
        long start = System.nanoTime();
        field = build();
        LOTRMod.LOGGER.info("Shire zones built in {} ms", (System.nanoTime() - start) / 1_000_000);
    }

    /** Which ring of the Shire a column in {@code region} belongs to, if any. */
    public static Zone zone(int x, int z, Region region) {
        boolean shire = region == Region.THE_SHIRE;
        if (!shire && region != Region.ERIADOR && region != Region.ARNOR) return Zone.NONE;
        double distance = borderDistance(x, z);
        if (Double.isNaN(distance)) return shire ? Zone.HEART : Zone.NONE;

        double wander = WANDER.getValue(x / 320.0, z / 320.0, false) * EDGE_WANDER;
        if (shire) {
            return Math.max(distance, 0.0) + wander > OUTSKIRTS_DEPTH ? Zone.HEART : Zone.OUTSKIRTS;
        }
        return Math.max(-distance, 0.0) + wander < WOODLANDS_REACH ? Zone.WOODLANDS : Zone.NONE;
    }

    /**
     * Distance from the Shire's border in blocks, positive inside the Shire and
     * negative outside; NaN if the maps aren't loaded or this is too far away
     * from the Shire to matter.
     */
    public static double borderDistance(int x, int z) {
        Field f = field;
        if (f == null) return Double.NaN;
        // Pixel centres on whole numbers, as RegionMapLoader has them
        double px = x / (double) RegionMapLoader.BLOCKS_PER_PIXEL + RegionMapLoader.getImageWidth() / 2.0 - f.x0;
        double pz = z / (double) RegionMapLoader.BLOCKS_PER_PIXEL + RegionMapLoader.getImageHeight() / 2.0 - f.z0;
        if (px < 0 || pz < 0 || px > f.width - 1 || pz > f.height - 1) return Double.NaN;

        int i = Math.min(Mth.floor(px), f.width - 2);
        int j = Math.min(Mth.floor(pz), f.height - 2);
        double tx = px - i;
        double tz = pz - j;
        float[] d = f.distance;
        double top = Mth.lerp(tx, d[j * f.width + i], d[j * f.width + i + 1]);
        double bottom = Mth.lerp(tx, d[(j + 1) * f.width + i], d[(j + 1) * f.width + i + 1]);
        return Mth.lerp(tz, top, bottom) * RegionMapLoader.BLOCKS_PER_PIXEL;
    }

    private static Field build() {
        int imageWidth = RegionMapLoader.getImageWidth();
        int imageHeight = RegionMapLoader.getImageHeight();
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = -1, maxZ = -1;
        for (int pz = 0; pz < imageHeight; pz++) {
            for (int px = 0; px < imageWidth; px++) {
                if (RegionMapLoader.getRegionAtPixel(px, pz) != Region.THE_SHIRE) continue;
                minX = Math.min(minX, px);
                maxX = Math.max(maxX, px);
                minZ = Math.min(minZ, pz);
                maxZ = Math.max(maxZ, pz);
            }
        }
        if (maxX < 0) return null;

        int x0 = Math.max(0, minX - MARGIN);
        int z0 = Math.max(0, minZ - MARGIN);
        int width = Math.min(imageWidth - 1, maxX + MARGIN) - x0 + 1;
        int height = Math.min(imageHeight - 1, maxZ + MARGIN) - z0 + 1;
        boolean[] inside = new boolean[width * height];
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                inside[z * width + x] = RegionMapLoader.getRegionAtPixel(x0 + x, z0 + z) == Region.THE_SHIRE;
            }
        }

        // The nearest pixel of the other side is always one on the border, so
        // only border pixels need measuring to
        List<int[]> borderIn = new ArrayList<>();
        List<int[]> borderOut = new ArrayList<>();
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                boolean in = inside[z * width + x];
                boolean edge = (x > 0 && inside[z * width + x - 1] != in)
                        || (x < width - 1 && inside[z * width + x + 1] != in)
                        || (z > 0 && inside[(z - 1) * width + x] != in)
                        || (z < height - 1 && inside[(z + 1) * width + x] != in);
                if (edge) (in ? borderIn : borderOut).add(new int[]{x, z});
            }
        }

        float[] distance = new float[width * height];
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                boolean in = inside[z * width + x];
                long best = Long.MAX_VALUE;
                for (int[] p : in ? borderOut : borderIn) {
                    long dx = p[0] - x, dz = p[1] - z;
                    best = Math.min(best, dx * dx + dz * dz);
                }
                // The border runs halfway between a pixel and its neighbour
                double d = best == Long.MAX_VALUE ? MARGIN : Math.sqrt(best) - 0.5;
                distance[z * width + x] = (float) (in ? d : -d);
            }
        }
        return new Field(x0, z0, width, height, distance);
    }
}
