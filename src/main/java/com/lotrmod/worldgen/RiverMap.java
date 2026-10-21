package com.lotrmod.worldgen;

import com.lotrmod.LOTRMod;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Middle-earth's rivers, from {@code maps/middleearth_rivers.png}.
 *
 * <p>The river map is the same size and scale as the region map. Each river is
 * a line one pixel wide, coloured with its region's colour (the Anduin's, the
 * Celduin's), whose alpha is the river's width there in blocks. Everything
 * else is transparent. Lines meet where rivers join and run a pixel or two out
 * into the sea or lake they flow into.
 *
 * <p>On load the lines are traced into chains between their ends and
 * junctions, smoothed so they don't follow the pixel grid, and indexed by
 * area. {@link #sample} then finds the nearest river to a point, through a
 * little noise so the channel meanders.
 */
public final class RiverMap {
    private RiverMap() {}

    private static final int BLOCKS_PER_PIXEL = 16;
    /** Corner-cutting passes over each traced line. */
    private static final int SMOOTHING_PASSES = 3;
    /** Side of an index cell, in blocks. */
    private static final int CELL = 128;

    /** How far from its centreline a river can reach, its valley included. */
    public static final double MAX_REACH = 240.0;

    /** How far, in blocks, the meander can shift a river sideways, and its feature size. */
    private static final double MEANDER_BLOCKS = 22.0;
    private static final double MEANDER_SCALE = 1.0 / 140.0;
    private static final PerlinSimplexNoise MEANDER =
            new PerlinSimplexNoise(RandomSource.create(13579L), List.of(0, 1, 2));

    private record Segment(double ax, double az, double bx, double bz, double widthA, double widthB, Region river) {}

    /** The nearest river to a point. */
    public record Sample(Region river, double distance, double width) {}

    /** Segments near each cell, keyed by cell. Null until the map loads. */
    private static volatile Long2ObjectOpenHashMap<Segment[]> index;

    public static boolean isLoaded() {
        return index != null;
    }

    public static void load(ResourceManager resourceManager) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(LOTRMod.MODID, "maps/middleearth_rivers.png");
        Optional<Resource> resource = resourceManager.getResource(location);
        if (resource.isEmpty()) {
            LOTRMod.LOGGER.warn("River map {} not found; Middle-earth will have no rivers", location);
            index = null;
            return;
        }
        try (InputStream stream = resource.get().open()) {
            BufferedImage image = ImageIO.read(stream);
            long start = System.nanoTime();
            List<Segment> segments = trace(image);
            index = buildIndex(segments);
            LOTRMod.LOGGER.info("River map loaded: {} segments in {} ms",
                    segments.size(), (System.nanoTime() - start) / 1_000_000);
        } catch (Exception e) {
            LOTRMod.LOGGER.error("Failed to load river map", e);
            index = null;
        }
    }

    /** The nearest river within {@link #MAX_REACH} of a point, or null if none is. */
    public static Sample sample(int x, int z) {
        Long2ObjectOpenHashMap<Segment[]> idx = index;
        if (idx == null) return null;

        double px = x + MEANDER.getValue(x * MEANDER_SCALE, z * MEANDER_SCALE, false) * MEANDER_BLOCKS;
        double pz = z + MEANDER.getValue((x - 4271) * MEANDER_SCALE, (z + 4271) * MEANDER_SCALE, false) * MEANDER_BLOCKS;

        Segment[] near = idx.get(key(Math.floorDiv((int) Math.floor(px), CELL), Math.floorDiv((int) Math.floor(pz), CELL)));
        if (near == null) return null;

        Segment best = null;
        double bestDist2 = MAX_REACH * MAX_REACH;
        double bestT = 0.0;
        for (Segment s : near) {
            double dx = s.bx - s.ax;
            double dz = s.bz - s.az;
            double len2 = dx * dx + dz * dz;
            double t = len2 > 0 ? Math.clamp(((px - s.ax) * dx + (pz - s.az) * dz) / len2, 0.0, 1.0) : 0.0;
            double ex = s.ax + dx * t - px;
            double ez = s.az + dz * t - pz;
            double dist2 = ex * ex + ez * ez;
            if (dist2 < bestDist2) {
                bestDist2 = dist2;
                best = s;
                bestT = t;
            }
        }
        if (best == null) return null;
        return new Sample(best.river, Math.sqrt(bestDist2), best.widthA + (best.widthB - best.widthA) * bestT);
    }

    // ── Tracing ──────────────────────────────────────────────────────────────

    private static List<Segment> trace(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();

        // Every river pixel, with its width and river
        Map<Long, int[]> pixels = new HashMap<>();
        Map<Integer, Region> byColour = new HashMap<>();
        for (int pz = 0; pz < h; pz++) {
            for (int px = 0; px < w; px++) {
                int argb = image.getRGB(px, pz);
                int alpha = argb >>> 24;
                if (alpha == 0) continue;
                Region river = byColour.computeIfAbsent(argb & 0xFFFFFF,
                        rgb -> Region.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
                pixels.put(key(px, pz), new int[]{px, pz, alpha, river.ordinal()});
            }
        }

        // Links between neighbouring pixels. A diagonal link is dropped where an
        // orthogonal pair already joins the same two pixels, so a corner in a
        // line isn't read as a junction.
        Map<Long, List<Long>> links = new HashMap<>();
        for (Map.Entry<Long, int[]> e : pixels.entrySet()) {
            int px = e.getValue()[0];
            int pz = e.getValue()[1];
            List<Long> out = new ArrayList<>(2);
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dz == 0) continue;
                    long n = key(px + dx, pz + dz);
                    if (!pixels.containsKey(n)) continue;
                    if (dx != 0 && dz != 0
                            && (pixels.containsKey(key(px + dx, pz)) || pixels.containsKey(key(px, pz + dz)))) continue;
                    out.add(n);
                }
            }
            links.put(e.getKey(), out);
        }

        // Walk chains from every end and junction, then any loops left over
        List<Segment> segments = new ArrayList<>();
        Set<Link> walked = new HashSet<>();
        for (int pass = 0; pass < 2; pass++) {
            for (Map.Entry<Long, List<Long>> e : links.entrySet()) {
                long start = e.getKey();
                if (pass == 0 && e.getValue().size() == 2) continue;
                for (long next : e.getValue()) {
                    if (walked.contains(edge(start, next))) continue;
                    List<int[]> chain = new ArrayList<>();
                    chain.add(pixels.get(start));
                    long prev = start;
                    long cur = next;
                    walked.add(edge(prev, cur));
                    while (true) {
                        chain.add(pixels.get(cur));
                        List<Long> out = links.get(cur);
                        if (out.size() != 2 || cur == start) break;
                        long after = out.get(0) == prev ? out.get(1) : out.get(0);
                        if (walked.contains(edge(cur, after))) break;
                        walked.add(edge(cur, after));
                        prev = cur;
                        cur = after;
                    }
                    addChain(chain, w, h, segments);
                }
            }
        }
        return segments;
    }

    /** Smooths one chain of pixels and adds it as segments in world space. */
    private static void addChain(List<int[]> chain, int imageWidth, int imageHeight, List<Segment> out) {
        int n = chain.size();
        double[] xs = new double[n];
        double[] zs = new double[n];
        double[] ws = new double[n];
        for (int i = 0; i < n; i++) {
            int[] p = chain.get(i);
            // Pixel centres sit on whole pixel coordinates, centred on world 0,0
            xs[i] = (p[0] - imageWidth / 2.0) * BLOCKS_PER_PIXEL;
            zs[i] = (p[1] - imageHeight / 2.0) * BLOCKS_PER_PIXEL;
            ws[i] = p[2];
        }
        // The river most of the chain belongs to: a chain leaving a junction
        // starts on a pixel that may be the other river's colour
        int[] votes = new int[Region.values().length];
        for (int[] p : chain) votes[p[3]]++;
        int best = 0;
        for (int i = 1; i < votes.length; i++) {
            if (votes[i] > votes[best]) best = i;
        }
        Region river = Region.values()[best];

        // Chaikin corner cutting, keeping both ends where they are so chains
        // still meet at their junctions
        for (int pass = 0; pass < SMOOTHING_PASSES && xs.length > 2; pass++) {
            int m = xs.length;
            double[] nx = new double[2 * (m - 1)];
            double[] nz = new double[nx.length];
            double[] nw = new double[nx.length];
            for (int i = 0; i < m - 1; i++) {
                nx[2 * i] = 0.75 * xs[i] + 0.25 * xs[i + 1];
                nz[2 * i] = 0.75 * zs[i] + 0.25 * zs[i + 1];
                nw[2 * i] = 0.75 * ws[i] + 0.25 * ws[i + 1];
                nx[2 * i + 1] = 0.25 * xs[i] + 0.75 * xs[i + 1];
                nz[2 * i + 1] = 0.25 * zs[i] + 0.75 * zs[i + 1];
                nw[2 * i + 1] = 0.25 * ws[i] + 0.75 * ws[i + 1];
            }
            nx[0] = xs[0]; nz[0] = zs[0]; nw[0] = ws[0];
            nx[nx.length - 1] = xs[m - 1]; nz[nz.length - 1] = zs[m - 1]; nw[nw.length - 1] = ws[m - 1];
            xs = nx;
            zs = nz;
            ws = nw;
        }

        for (int i = 0; i < xs.length - 1; i++) {
            out.add(new Segment(xs[i], zs[i], xs[i + 1], zs[i + 1], ws[i], ws[i + 1], river));
        }
    }

    /** Files each segment under every cell within {@link #MAX_REACH} of it. */
    private static Long2ObjectOpenHashMap<Segment[]> buildIndex(List<Segment> segments) {
        Map<Long, List<Segment>> cells = new HashMap<>();
        for (Segment s : segments) {
            int minX = Math.floorDiv((int) Math.floor(Math.min(s.ax, s.bx) - MAX_REACH), CELL);
            int maxX = Math.floorDiv((int) Math.ceil(Math.max(s.ax, s.bx) + MAX_REACH), CELL);
            int minZ = Math.floorDiv((int) Math.floor(Math.min(s.az, s.bz) - MAX_REACH), CELL);
            int maxZ = Math.floorDiv((int) Math.ceil(Math.max(s.az, s.bz) + MAX_REACH), CELL);
            for (int cx = minX; cx <= maxX; cx++) {
                for (int cz = minZ; cz <= maxZ; cz++) {
                    cells.computeIfAbsent(key(cx, cz), k -> new ArrayList<>()).add(s);
                }
            }
        }
        Long2ObjectOpenHashMap<Segment[]> idx = new Long2ObjectOpenHashMap<>(cells.size());
        cells.forEach((k, v) -> idx.put((long) k, v.toArray(new Segment[0])));
        return idx;
    }

    private static long key(int a, int b) {
        return ((long) a << 32) | (b & 0xFFFFFFFFL);
    }

    /** A link between two pixels, the same whichever end it is walked from. */
    private record Link(long low, long high) {}

    private static Link edge(long a, long b) {
        return a < b ? new Link(a, b) : new Link(b, a);
    }
}
