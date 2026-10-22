package com.lotrmod.worldgen;

import com.lotrmod.LOTRMod;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;

/**
 * The climate values that steer vanilla's overworld terrain across Middle-earth.
 *
 * <p>The dimension's terrain is vanilla's own: its noise router, height splines,
 * caves and aquifers, copied into {@code data/lotrmod/worldgen}. Vanilla decides
 * how high and how rugged the ground is from a handful of climate values, and
 * this class supplies the two that matter for geography from the maps instead
 * of from noise:
 *
 * <ul>
 *   <li><b>Continentalness</b> comes from the landmask: from deep ocean far
 *       offshore, through vanilla's coast band at the shoreline, up to the
 *       region's own value inland.</li>
 *   <li><b>Erosion</b> comes from the region map: each {@link Region} sets a
 *       centre and a spread, and vanilla's erosion noise wanders between them.</li>
 * </ul>
 *
 * <p>It also says where the mountain ranges, marshes, the Shire and Rohan are,
 * for {@link MountainTerrain}, {@link MarshTerrain}, {@link ShireTerrain} and
 * {@link RohanTerrain}.
 *
 * <p>Region values are blurred over roughly a hundred blocks before use, so
 * mountains rise out of the plains and rivers sink into their valleys instead of
 * stepping at the 16-block pixel edges of the map. {@code ClimateDensityFunction}
 * exposes these values to the noise settings JSON.
 */
public final class MiddleEarthClimate {
    private MiddleEarthClimate() {}

    /**
     * Continentalness at the shoreline. In vanilla's splines this is the coast
     * band, where land meets the water at sea level and beaches form.
     */
    private static final double SHORE = -0.16;
    /** Continentalness of the open ocean far offshore: vanilla's deep ocean. */
    private static final double DEEP_OCEAN = -0.6;
    /** Blocks offshore for the sea to get most of the way to {@link #DEEP_OCEAN}. */
    private static final double OCEAN_FALLOFF = 250.0;
    /** Blocks inland over which land climbs from the coast to its region's value. */
    private static final double COAST_RAMP = 256.0;

    /** Beaches reach this far inland, give or take {@link #BEACH_WIDTH_VARIATION}. */
    private static final double BEACH_WIDTH = 20.0;
    private static final double BEACH_WIDTH_VARIATION = 12.0;

    /**
     * Blurred share of mountain range (or marsh, Shire or Rohan) below which
     * the ground is all vanilla's, and over how much more it becomes all
     * {@link MountainTerrain} (or {@link MarshTerrain}, {@link ShireTerrain} or
     * {@link RohanTerrain}).
     */
    private static final double MOUNTAIN_EDGE = 0.2;
    private static final double MOUNTAIN_FADE = 0.5;
    /** Blocks inland over which mountains fade in from the coast. */
    private static final double MOUNTAIN_COAST_FADE = 32.0;

    /** Each grid cell averages 2x2 map pixels, i.e. 32x32 blocks. */
    private static final int PIXELS_PER_CELL = 2;
    /**
     * Box blur radius in cells. Three passes approximate a Gaussian with a sigma
     * of {@code sqrt(r(r+1))} cells, about 80 blocks.
     */
    private static final int BLUR_RADIUS = 2;
    private static final int BLUR_PASSES = 3;

    /** Wanders the coastline off the 16-block pixel grid. */
    private static final PerlinSimplexNoise COASTLINE =
            new PerlinSimplexNoise(RandomSource.create(98765L), List.of(0, 1, 2));

    /** Region climate, blurred. Null until the maps load. */
    private static volatile Grid grid;

    /** Rebuilds the climate grid from the region map. Call after the maps (re)load. */
    public static void rebuild() {
        if (!RegionMapLoader.isLoaded()) {
            grid = null;
            return;
        }
        long start = System.nanoTime();
        grid = Grid.build();
        LOTRMod.LOGGER.info("Middle-earth climate grid built in {} ms",
                (System.nanoTime() - start) / 1_000_000);
    }

    /**
     * Signed distance to the coast in blocks, positive on land and negative at
     * sea, sampled through a little noise so the coastline wanders off the
     * 16-block pixel grid. Terrain and the ocean biome both use this one line.
     */
    public static double coastDistance(int x, int z) {
        double offX = COASTLINE.getValue(x / 80.0, z / 80.0, false) * 10.0;
        double offZ = COASTLINE.getValue((x + 10000) / 80.0, (z + 10000) / 80.0, false) * 10.0;
        return LandmaskLoader.getShoreDistance(x + (int) offX, z + (int) offZ);
    }

    /**
     * How far inland a beach reaches here. Varied with noise because the
     * shore-distance field is interpolated in straight lines between pixels, so
     * a fixed width would show those lines as the beach's inland edge.
     */
    public static double beachWidth(int x, int z) {
        double n = COASTLINE.getValue((x + 5000) / 30.0, (z - 5000) / 30.0, false);
        return BEACH_WIDTH + n * BEACH_WIDTH_VARIATION;
    }

    /** Continentalness here, before vanilla's continentalness noise is added. */
    public static double continentalness(int x, int z) {
        double shore = coastDistance(x, z);
        if (shore < 0) {
            return SHORE + (DEEP_OCEAN - SHORE) * (1.0 - Math.exp(shore / OCEAN_FALLOFF));
        }
        return SHORE + (regionContinentalness(x, z) - SHORE) * inlandRamp(shore);
    }

    /**
     * 0 at the coast (and at sea) rising to 1 a little inland: how much of
     * vanilla's continentalness noise to let in. Kept out at the coast so the
     * shoreline stays where the landmask draws it.
     */
    public static double inland(int x, int z) {
        double shore = coastDistance(x, z);
        return shore < 0 ? 0.0 : inlandRamp(shore);
    }

    /** Erosion centre here, from the regions nearby. */
    public static double erosion(int x, int z) {
        Grid g = grid;
        return g != null ? g.sample(g.erosion, x, z) : Region.ERIADOR.getErosion();
    }

    /** How far erosion wanders either side of {@link #erosion} here. */
    public static double erosionSpread(int x, int z) {
        Grid g = grid;
        return g != null ? g.sample(g.erosionSpread, x, z) : Region.ERIADOR.getErosionSpread();
    }

    /**
     * How much of the ground here is mountain range, from 0 outside the ranges
     * to 1 inside them: the share of {@link MountainTerrain} in the terrain.
     * Zero at sea and eased in from the coast, so the shoreline stays vanilla's.
     */
    public static double mountainWeight(int x, int z) {
        Grid g = grid;
        if (g == null) return 0.0;
        double shore = coastDistance(x, z);
        if (shore <= 0) return 0.0;
        double core = Mth.clamp((g.sample(g.mountain, x, z) - MOUNTAIN_EDGE) / MOUNTAIN_FADE, 0.0, 1.0);
        double coast = Math.min(1.0, shore / MOUNTAIN_COAST_FADE);
        return smoothstep(core) * smoothstep(coast);
    }

    /**
     * How much of the ground here is marsh, from 0 outside to 1 inside: the
     * share of {@link MarshTerrain} in the terrain. Zero at sea.
     */
    public static double marshWeight(int x, int z) {
        Grid g = grid;
        if (g == null || coastDistance(x, z) <= 0) return 0.0;
        return smoothstep(Mth.clamp((g.sample(g.marsh, x, z) - MOUNTAIN_EDGE) / MOUNTAIN_FADE, 0.0, 1.0));
    }

    /**
     * How much of the ground here is the Shire, from 0 outside to 1 inside: the
     * share of {@link ShireTerrain} in the terrain. Zero at sea.
     */
    public static double shireWeight(int x, int z) {
        Grid g = grid;
        if (g == null || coastDistance(x, z) <= 0) return 0.0;
        return smoothstep(Mth.clamp((g.sample(g.shire, x, z) - MOUNTAIN_EDGE) / MOUNTAIN_FADE, 0.0, 1.0));
    }

    /**
     * How much of the ground here is Rohan, from 0 outside to 1 inside: the
     * share of {@link RohanTerrain} in the terrain. Zero at sea.
     */
    public static double rohanWeight(int x, int z) {
        Grid g = grid;
        if (g == null || coastDistance(x, z) <= 0) return 0.0;
        return smoothstep(Mth.clamp((g.sample(g.rohan, x, z) - MOUNTAIN_EDGE) / MOUNTAIN_FADE, 0.0, 1.0));
    }

    /** Ridgeline height of the mountain ranges nearby, for {@link MountainTerrain}. */
    public static double ridgeHeight(int x, int z) {
        Grid g = grid;
        return g != null ? g.sample(g.ridgeHeight, x, z) : 0.0;
    }

    private static double regionContinentalness(int x, int z) {
        Grid g = grid;
        return g != null ? g.sample(g.continentalness, x, z) : Region.ERIADOR.getContinentalness();
    }

    /** Smoothstep from the shore to {@link #COAST_RAMP} blocks inland. */
    private static double inlandRamp(double shore) {
        return smoothstep(Math.min(1.0, shore / COAST_RAMP));
    }

    private static double smoothstep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * The region climate on a grid of {@link #PIXELS_PER_CELL}-pixel cells,
     * blurred, and sampled with a cubic B-spline so the result has no creases
     * along cell edges for the terrain to pick up.
     */
    private record Grid(int width, int height, double halfImageWidth, double halfImageHeight,
                        float[] continentalness, float[] erosion, float[] erosionSpread,
                        float[] mountain, float[] ridgeHeight, float[] marsh, float[] shire,
                        float[] rohan) {

        static Grid build() {
            int imageWidth = RegionMapLoader.getImageWidth();
            int imageHeight = RegionMapLoader.getImageHeight();
            int width = Mth.positiveCeilDiv(imageWidth, PIXELS_PER_CELL);
            int height = Mth.positiveCeilDiv(imageHeight, PIXELS_PER_CELL);

            // Sea pixels carry no weight, so land near the coast keeps its own
            // region's values instead of being averaged toward the ocean's.
            float[] weight = new float[width * height];
            float[] continentalness = new float[width * height];
            float[] erosion = new float[width * height];
            float[] erosionSpread = new float[width * height];
            float[] mountain = new float[width * height];
            float[] ridgeHeight = new float[width * height];
            float[] marsh = new float[width * height];
            float[] shire = new float[width * height];
            float[] rohan = new float[width * height];
            for (int pz = 0; pz < imageHeight; pz++) {
                for (int px = 0; px < imageWidth; px++) {
                    Region region = RegionMapLoader.getRegionAtPixel(px, pz);
                    if (region == Region.OCEAN) continue;
                    int cell = (pz / PIXELS_PER_CELL) * width + px / PIXELS_PER_CELL;
                    weight[cell] += 1f;
                    continentalness[cell] += (float) region.getContinentalness();
                    erosion[cell] += (float) region.getErosion();
                    erosionSpread[cell] += (float) region.getErosionSpread();
                    if (region.isMarsh()) marsh[cell] += 1f;
                    if (region == Region.THE_SHIRE) shire[cell] += 1f;
                    if (region == Region.ROHAN) rohan[cell] += 1f;
                    if (region.isMountain()) {
                        mountain[cell] += 1f;
                        ridgeHeight[cell] += (float) region.getRidgeHeight();
                    }
                }
            }

            for (float[] field : List.of(weight, continentalness, erosion, erosionSpread, mountain, ridgeHeight, marsh, shire, rohan)) {
                blur(field, width, height);
            }

            // Land that the region map has no region for (an island only the
            // landmask knows about) gets the fallback biome's climate.
            for (int cell = 0; cell < weight.length; cell++) {
                // Ridge height averages over mountain pixels only, so a range
                // keeps its full height out to where it fades into the lowlands.
                float m = mountain[cell];
                ridgeHeight[cell] = m > 1e-6f ? ridgeHeight[cell] / m : 0f;

                float w = weight[cell];
                if (w > 1e-6f) {
                    continentalness[cell] /= w;
                    erosion[cell] /= w;
                    erosionSpread[cell] /= w;
                    mountain[cell] /= w;
                    marsh[cell] /= w;
                    shire[cell] /= w;
                    rohan[cell] /= w;
                } else {
                    continentalness[cell] = (float) Region.ERIADOR.getContinentalness();
                    erosion[cell] = (float) Region.ERIADOR.getErosion();
                    erosionSpread[cell] = (float) Region.ERIADOR.getErosionSpread();
                }
            }
            return new Grid(width, height, imageWidth / 2.0, imageHeight / 2.0,
                    continentalness, erosion, erosionSpread, mountain, ridgeHeight, marsh, shire, rohan);
        }

        double sample(float[] field, int x, int z) {
            // Map pixel coordinates with pixel centres on whole numbers, the same
            // convention RegionMapLoader uses; then cell coordinates likewise.
            double cellX = (x / 16.0 + halfImageWidth - 0.5) / PIXELS_PER_CELL;
            double cellZ = (z / 16.0 + halfImageHeight - 0.5) / PIXELS_PER_CELL;
            int i = Mth.floor(cellX);
            int j = Mth.floor(cellZ);
            double tx = cellX - i;
            double tz = cellZ - j;

            double sum = 0.0;
            for (int b = 0; b < 4; b++) {
                int row = Mth.clamp(j - 1 + b, 0, height - 1) * width;
                double rowSum = 0.0;
                for (int a = 0; a < 4; a++) {
                    rowSum += bSpline(a, tx) * field[row + Mth.clamp(i - 1 + a, 0, width - 1)];
                }
                sum += bSpline(b, tz) * rowSum;
            }
            return sum;
        }

        /** Uniform cubic B-spline weight of the {@code k}th of four neighbouring cells. */
        private static double bSpline(int k, double t) {
            double t2 = t * t;
            double t3 = t2 * t;
            return switch (k) {
                case 0 -> (1.0 - 3.0 * t + 3.0 * t2 - t3) / 6.0;
                case 1 -> (4.0 - 6.0 * t2 + 3.0 * t3) / 6.0;
                case 2 -> (1.0 + 3.0 * t + 3.0 * t2 - 3.0 * t3) / 6.0;
                default -> t3 / 6.0;
            };
        }

        /** Repeated box blur, zero outside the grid. */
        private static void blur(float[] field, int width, int height) {
            float[] tmp = new float[field.length];
            for (int pass = 0; pass < BLUR_PASSES; pass++) {
                blurLines(field, tmp, width, height, 1, width);
                blurLines(tmp, field, height, width, width, 1);
            }
        }

        /**
         * Box blur along one axis: {@code count} lines of {@code length}
         * values, {@code step} apart within a line and {@code lineStep} apart
         * between lines.
         */
        private static void blurLines(float[] src, float[] dst, int length, int count, int step, int lineStep) {
            double norm = 1.0 / (2 * BLUR_RADIUS + 1);
            for (int line = 0; line < count; line++) {
                int base = line * lineStep;
                double acc = 0.0;
                for (int k = 0; k <= Math.min(BLUR_RADIUS, length - 1); k++) {
                    acc += src[base + k * step];
                }
                for (int k = 0; k < length; k++) {
                    dst[base + k * step] = (float) (acc * norm);
                    int enter = k + BLUR_RADIUS + 1;
                    int leave = k - BLUR_RADIUS;
                    if (enter < length) acc += src[base + enter * step];
                    if (leave >= 0) acc -= src[base + leave * step];
                }
            }
        }
    }
}
