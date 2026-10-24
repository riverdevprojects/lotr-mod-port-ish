package com.lotrmod.worldgen;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.List;

/**
 * The shape of Dale and the Lonely Mountain, and which of Dale's landscapes
 * lies where.
 *
 * <p>The region map has no Erebor of its own, so the Mountain is raised here,
 * in the north of Dale: a lone peak rising to about y 300, far above
 * anything else in the north, over a thousand blocks across at its foot, its slopes scored by gullies running down from the
 * summit, with eight long spurs reaching out from it. The two longest run
 * south and enfold the vale of Dale; at the end of the south-western one
 * stands Ravenhill ({@link #RAVENHILL_X}, {@link #RAVENHILL_Z}), where the
 * Dwarves kept a watch-post. Straight south of the summit a gorge cuts into
 * the Mountain's face, and at its head is the Front Gate ({@link #GATE_X},
 * {@link #GATE_Z}), out of which the River Running flows.
 *
 * <p>The river runs south down the gorge and into the vale, and swings round
 * the town of Dale in a great loop: the town stands on a low, level rise
 * within it ({@link #TOWN_X}, {@link #TOWN_Z}, {@link #TOWN_Y}). Then it
 * winds south to the Long Lake, and out of the lake's foot to join the
 * Celduin, as the river map draws it, at the southern tip of Dale. River and
 * lake are cut below the waterline and filled with water by the chunk
 * generator, as the Bruinen and Lothlórien's streams are.
 *
 * <p>Round all this, the land rolls gently about y 70, rising further from
 * the Mountain into broad downs here and there.
 *
 * <p>The ground is the same in both of Dale's eras ({@code DaleEra}): the
 * Desolation is the same land burnt. {@link #zone} says which landscape a place
 * is, and the biome source picks the biome for it in the world's era.
 *
 * <p>{@link TerrainBlendDensityFunction} fades the terrain over to this inside
 * Dale, by {@link MiddleEarthClimate#daleWeight}.
 */
public final class DaleTerrain {
    private DaleTerrain() {}

    /**
     * Dale's landscapes. Each is a biome of its own, which in the Desolation
     * is another (see {@code MiddleEarthBiomeSource}).
     */
    public enum Zone {
        /** The open fields of the vale, farmed. */
        FIELDS,
        /** Pasture and flower meadow, orchards and lone oaks, further out. */
        MEADOWS,
        /** Woods of oak, pine and fir, on the Mountain's skirts and in patches across the land. */
        WOODS,
        /** The rock of the Mountain and its spurs. */
        MOUNTAIN,
        /** The town of Dale, in the river's loop. */
        TOWN,
        /** The River Running. */
        RIVER,
        /** The Long Lake. */
        LAKE
    }

    // ── where things are ─────────────────────────────────────────────────────

    /** The summit of the Lonely Mountain. */
    public static final int PEAK_X = 3950;
    public static final int PEAK_Z = -12920;
    /** The middle of the town of Dale, and the height its rise is levelled at (the ground under its paving). */
    public static final int TOWN_X = 3920;
    public static final int TOWN_Z = -12230;
    public static final int TOWN_Y = 73;
    /** Radius of the town's rise, and of the ground counted as the town. */
    public static final double TOWN_RADIUS = 118.0;
    /** The Front Gate, at the head of the gorge, facing south down it; the gorge floor beside the river. */
    public static final int GATE_X = PEAK_X;
    public static final int GATE_Z = PEAK_Z + 200;
    public static final int GORGE_FLOOR = 66;
    /** Ravenhill, at the end of the south-western spur. */
    public static final int RAVENHILL_X = (int) Math.round(PEAK_X + Math.cos(Math.toRadians(112)) * 800);
    public static final int RAVENHILL_Z = (int) Math.round(PEAK_Z + Math.sin(Math.toRadians(112)) * 800);
    /** The middle of the Long Lake, and its half-length (north to south) and half-width. */
    private static final double LAKE_X = 3725;
    private static final double LAKE_Z = -11475;
    private static final double LAKE_HALF_LENGTH = 320;
    private static final double LAKE_HALF_WIDTH = 125;

    /**
     * The River Running from just inside the Front Gate, down the gorge,
     * round the town and down to the lake's head; and out of its foot to the
     * Celduin. The gate itself, and the vault the river runs out under, are
     * built by the landmarks feature.
     */
    private static final double[][] RIVER_POINTS = {
            {3950, -12728}, {3951, -12680}, {3952, -12640}, {3965, -12540}, {4010, -12420}, {4062, -12310},
            {4068, -12210}, {4020, -12120}, {3925, -12085}, {3830, -12095}, {3765, -12010},
            {3740, -11900}, {3728, -11790}};
    private static final double[][] OUTFLOW_POINTS = {
            {3722, -11160}, {3735, -11080}, {3728, -11020}, {3720, -10960}};

    // ── heights ──────────────────────────────────────────────────────────────

    /** Height the land rolls about, away from the Mountain. */
    private static final double BASE = 69.0;
    /** The waterline: water is filled up to here, one below sea level. */
    private static final int WATERLINE = 62;
    /** The river's bed in mid-stream, and how much its half-width grows from the gate (7) to the lake (11). */
    private static final double RIVER_BED = 56.0;
    /** Height of the Mountain's summit above the land at its foot, and the radius of its foot. */
    private static final double MOUNTAIN_HEIGHT = 225.0;
    private static final double MOUNTAIN_RADIUS = 540.0;
    /** Spurs: compass angle (0 east, 90 south), length, height where it leaves the Mountain, half-width there. */
    private static final double[][] SPURS = {
            {112, 900, 120, 30}, {64, 880, 115, 30}, {185, 700, 90, 34}, {250, 640, 85, 32},
            {300, 680, 90, 34}, {5, 720, 92, 34}, {150, 560, 60, 30}, {30, 560, 60, 30}};
    /** How high above the land a place must stand to be the Mountain's rock, not its wooded skirts. */
    private static final double ROCK_HEIGHT = 34.0;

    private static final PerlinSimplexNoise SWELLS = noise(81001, 0, 1);
    private static final PerlinSimplexNoise ROLLS = noise(81002, 0, 1);
    private static final PerlinSimplexNoise UNEVEN = noise(81003, 0);
    private static final PerlinSimplexNoise DOWNS = noise(81004, 0, 1);
    private static final PerlinSimplexNoise OUTLINE = noise(81005, 0);
    private static final PerlinSimplexNoise OUTLINE_FINE = noise(81006, 0);
    private static final PerlinSimplexNoise SPUR_BEND = noise(81007, 0);
    private static final PerlinSimplexNoise SPUR_KNOLLS = noise(81008, 0, 1);
    private static final SimplexNoise GULLIES = new SimplexNoise(new LegacyRandomSource(81009));
    private static final SimplexNoise GULLIES_FINE = new SimplexNoise(new LegacyRandomSource(81010));
    private static final PerlinSimplexNoise CRAGS = noise(81011, 0, 1);
    private static final PerlinSimplexNoise SHORE = noise(81012, 0);
    private static final PerlinSimplexNoise WOODS = noise(81013, 0, 1);
    private static final PerlinSimplexNoise SCORCH = noise(81014, 0);

    private static PerlinSimplexNoise noise(long seed, Integer... octaves) {
        return new PerlinSimplexNoise(RandomSource.create(seed), List.of(octaves));
    }

    private static final River RIVER = new River(RIVER_POINTS, 7.0, 11.0);
    private static final River OUTFLOW = new River(OUTFLOW_POINTS, 9.0, 9.0);

    /** Height of Dale's surface here, in blocks. */
    public static double height(int x, int z) {
        return sample(x, z).height;
    }

    /** Which of Dale's landscapes this is. */
    public static Zone zone(int x, int z) {
        Sample s = sample(x, z);
        if (s.lake < 1.0) return Zone.LAKE;
        if (s.riverDistance < s.riverHalfWidth + 1.5) return Zone.RIVER;
        if (townDistance(x, z) < TOWN_RADIUS) return Zone.TOWN;
        if (s.mountain > ROCK_HEIGHT) return Zone.MOUNTAIN;

        double woods = WOODS.getValue(x / 700.0, z / 700.0, false);
        // A belt of wood round the Mountain's foot and up its lower slopes
        if (s.mountain > 6.0 && woods > -0.45) return Zone.WOODS;
        if (woods > 0.32) return Zone.WOODS;

        boolean vale = s.riverDistance < 420.0 || townDistance(x, z) < 650.0 || s.lake < 2.2;
        return vale && s.mountain < 6.0 ? Zone.FIELDS : Zone.MEADOWS;
    }

    /**
     * Whether Smaug's fire reached here: the land within twelve hundred blocks
     * or so of the Mountain, give or take. Further out, in the Desolation,
     * Dale is not burnt but abandoned, its fields and meadows gone wild.
     */
    public static boolean scorched(int x, int z) {
        double d = Math.sqrt(sq(x - PEAK_X) + sq(z - PEAK_Z));
        return d < 1150.0 + SCORCH.getValue(x / 300.0, z / 300.0, false) * 250.0;
    }

    /**
     * How much the summit of the Mountain is to be spared vanilla's top
     * slide, which would otherwise flatten everything above y 250: 1 within
     * two hundred blocks of the summit, fading to 0 by two hundred and sixty
     * ({@link TopSlideDensityFunction}).
     */
    public static double summitShare(int x, int z) {
        double r = Math.sqrt(sq(x - PEAK_X) + sq(z - PEAK_Z));
        return 1.0 - smoothstep((r - 200.0) / 60.0);
    }

    /** Whether ground below the waterline here is under water: the river or the lake. */
    public static boolean isWater(int x, int z) {
        Sample s = sample(x, z);
        return s.lake < 1.0 || s.riverDistance < s.riverHalfWidth + 2.0;
    }

    /** Blocks from the middle of the town of Dale. */
    public static double townDistance(double x, double z) {
        return Math.sqrt(sq(x - TOWN_X) + sq(z - TOWN_Z));
    }

    /** Distance in blocks from here to the River Running, or the stream out of the lake, whichever is nearer. */
    public static double riverDistance(int x, int z) {
        return sample(x, z).riverDistance;
    }

    /** Half the width of the river at its nearest point to here. */
    public static double riverHalfWidth(int x, int z) {
        return sample(x, z).riverHalfWidth;
    }

    /** How far the Mountain and its spurs rise above the land here. */
    public static double mountainRise(int x, int z) {
        return sample(x, z).mountain;
    }

    /** The height of the top solid block of Dale's ground here. */
    public static int groundY(int x, int z) {
        return (int) Math.ceil(height(x, z)) - 1;
    }

    // ── the working ──────────────────────────────────────────────────────────

    /** Everything about a column, worked out once; asked for a column at a time. */
    private static final class Sample {
        int x;
        int z;
        boolean valid;
        double height;
        /** How far the Mountain and its spurs rise above the land here. */
        double mountain;
        double riverDistance;
        double riverHalfWidth;
        /** The lake's distance field: under 1 in the water, 1 on its shore. */
        double lake;
    }

    private static final ThreadLocal<Sample> LAST = ThreadLocal.withInitial(Sample::new);

    private static Sample sample(int x, int z) {
        Sample s = LAST.get();
        if (!s.valid || s.x != x || s.z != z) {
            s.x = x;
            s.z = z;
            s.valid = true;
            compute(s, x, z);
        }
        return s;
    }

    private static void compute(Sample s, int x, int z) {
        double dx = x - PEAK_X;
        double dz = z - PEAK_Z;
        double r = Math.sqrt(dx * dx + dz * dz);

        // The land
        double height = BASE
                + SWELLS.getValue(x / 500.0, z / 500.0, false) * 5.0
                + ROLLS.getValue(x / 130.0, z / 130.0, false) * 2.2
                + UNEVEN.getValue(x / 26.0, z / 26.0, false) * 0.6;
        double downs = Math.pow(Math.max(0.0, DOWNS.getValue(x / 380.0, z / 380.0, false) + 0.05), 1.3) * 26.0;
        height += downs * smoothstep((r - 700.0) / 400.0);

        // The Mountain
        double mountain = r < MOUNTAIN_RADIUS * 2.0 ? mountain(x, z, dx, dz, r) : 0.0;
        height += mountain;
        s.mountain = mountain;

        // The gorge up to the Front Gate, cut straight north into the Mountain's south face
        double gorgeLength = MOUNTAIN_RADIUS * 1.15;
        double along = dz;
        if (along > 198.5 && along < gorgeLength) {
            double across = Math.abs(dx);
            double halfWidth = 13.0 + 0.11 * Math.max(0.0, along - 200.0);
            if (across < halfWidth + 18.0) {
                double wall = smoothstep((halfWidth + 14.0 - across) / 14.0)
                        * smoothstep((along - 198.5) / 2.0)
                        * smoothstep((gorgeLength - along) / 80.0);
                height -= (height - GORGE_FLOOR) * wall;
            }
        }

        // The town's rise, levelled
        double town = smoothstep((TOWN_RADIUS + 10.0 - townDistance(x, z)) / 30.0);
        height += (TOWN_Y - height) * town;

        // The river and its valley
        River.Hit river = RIVER.nearest(x, z);
        River.Hit outflow = OUTFLOW.nearest(x, z);
        if (outflow.distance < river.distance) river = outflow;
        s.riverDistance = river.distance;
        s.riverHalfWidth = river.halfWidth;
        // The river rises inside the Mountain, behind the Front Gate: its
        // valley is cut only in front of the gate, not back into the rock
        double open = z < GATE_Z && Math.abs(x - GATE_X) < 90 ? smoothstep((z - (GATE_Z - 1)) / 3.0) : 1.0;
        double valley = smoothstep((river.halfWidth + 50.0 - river.distance) / 50.0) * open;
        if (valley > 0.0) {
            double bank = 64.0 + (river.distance - river.halfWidth) * 0.12;
            height += (Math.min(height, bank) - height) * valley * (1.0 - town * 0.6);
        }
        if (river.distance < river.halfWidth + 2.0 && open > 0.5) {
            double t = Mth.clamp(river.distance / river.halfWidth, 0.0, 1.0);
            double cut = river.distance < river.halfWidth
                    ? RIVER_BED + t * t * 6.0
                    : WATERLINE - 0.5 + (river.distance - river.halfWidth);
            height = Math.min(height, cut);
        }

        // The lake
        double lx = (x - LAKE_X) / LAKE_HALF_WIDTH;
        double lz = (z - LAKE_Z) / LAKE_HALF_LENGTH;
        double lake = Math.sqrt(lx * lx + lz * lz) * (1.0 + 0.08 * SHORE.getValue(x / 90.0, z / 90.0, false));
        s.lake = lake;
        double shore = smoothstep((1.35 - lake) / 0.35);
        if (shore > 0.0) height += (Math.min(height, WATERLINE - 0.5 + (lake - 1.0) * 30.0) - height) * shore;
        if (lake < 1.0) height = Math.min(height, WATERLINE - 14.0 * Math.pow(Math.max(0.0, 1.0 - lake * lake), 0.7));

        s.height = height;
    }

    /** How far the Mountain, its spurs and Ravenhill rise above the land here. */
    private static double mountain(int x, int z, double dx, double dz, double r) {
        double angle = Math.atan2(dz, dx);
        double ca = Math.cos(angle), sa = Math.sin(angle);
        double radius = MOUNTAIN_RADIUS * (1.0 + 0.13 * OUTLINE.getValue(ca * 1.3, sa * 1.3, false)
                + 0.06 * OUTLINE_FINE.getValue(ca * 4.0, sa * 4.0, false));
        double t = Mth.clamp(1.0 - r / radius, 0.0, 1.0);
        double cone = MOUNTAIN_HEIGHT * (0.45 * Math.pow(t, 1.15) + 0.55 * Math.pow(t, 2.4));

        double spurs = 0.0;
        for (int k = 0; k < SPURS.length; k++) {
            double[] spur = SPURS[k];
            double a = Math.toRadians(spur[0]);
            double ux = Math.cos(a), uz = Math.sin(a);
            double along = dx * ux + dz * uz;
            if (along <= 0.0 || along >= spur[1]) continue;
            double bend = SPUR_BEND.getValue(along / 260.0, k * 7.31, false) * 45.0 * smoothstep(along / 250.0);
            double across = Math.abs(-dx * uz + dz * ux + bend);
            double halfWidth = spur[3] + 0.08 * along;
            if (across >= halfWidth) continue;
            double rise = spur[2] * Math.pow(1.0 - along / spur[1], 0.8)
                    * smoothstep((spur[1] - along) / (0.3 * spur[1]))
                    * (1.0 + 0.3 * SPUR_KNOLLS.getValue(along / 110.0, k * 5.13, false))
                    * smoothstep(along / 120.0);
            double profile = Math.cos(across / halfWidth * Math.PI / 2.0);
            spurs = Math.max(spurs, rise * profile * profile);
        }

        double raven = Math.sqrt(sq(x - RAVENHILL_X) + sq(z - RAVENHILL_Z));
        double ravenhill = raven < 70.0 ? 38.0 * Math.pow(1.0 - sq(raven / 70.0), 1.2) : 0.0;

        double m = Math.max(cone, spurs * (1.0 - t * 0.5)) + ravenhill;

        // Gullies running down from the summit: noise drawn out along the slope,
        // sampled round a circle so it doesn't seam
        if (m > 0.0) {
            double ring = radius / 110.0;
            double g = GULLIES.getValue(ca * ring, sa * ring, r / 200.0) * 0.67
                    + GULLIES_FINE.getValue(ca * ring * 2.0, sa * ring * 2.0, r / 100.0) * 0.33;
            double gully = Math.pow(1.0 - Math.min(1.0, Math.abs(g) * 1.4), 6.0) * 14.0
                    * smoothstep(m / 50.0) * smoothstep((r - 90.0) / 120.0);
            m = Math.max(0.0, m - gully);
            // Crags about the summit
            m += Math.abs(CRAGS.getValue(x / 40.0, z / 40.0, false)) * 10.0 * smoothstep((t - 0.55) / 0.3);
        }
        return m;
    }

    private static double sq(double v) {
        return v * v;
    }

    private static double smoothstep(double t) {
        t = Mth.clamp(t, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * A river as a smooth line: Catmull-Rom through its points, cut into short
     * straight lengths, with its half-width growing from source to mouth.
     */
    private static final class River {
        record Hit(double distance, double halfWidth) {}

        private final double[] xs;
        private final double[] zs;
        /** How far along the river each point is, from 0 at its source to 1 at its mouth. */
        private final double[] along;
        private final double minX, minZ, maxX, maxZ;
        private final double sourceHalfWidth;
        private final double mouthHalfWidth;

        River(double[][] points, double sourceHalfWidth, double mouthHalfWidth) {
            this.sourceHalfWidth = sourceHalfWidth;
            this.mouthHalfWidth = mouthHalfWidth;
            int steps = 24;
            int n = (points.length - 1) * steps + 1;
            xs = new double[n];
            zs = new double[n];
            along = new double[n];
            int i = 0;
            for (int p = 0; p < points.length - 1; p++) {
                double[] p0 = points[Math.max(0, p - 1)], p1 = points[p], p2 = points[p + 1], p3 = points[Math.min(points.length - 1, p + 2)];
                for (int k = 0; k < steps; k++) {
                    double t = k / (double) steps;
                    xs[i] = catmullRom(p0[0], p1[0], p2[0], p3[0], t);
                    zs[i] = catmullRom(p0[1], p1[1], p2[1], p3[1], t);
                    i++;
                }
            }
            xs[i] = points[points.length - 1][0];
            zs[i] = points[points.length - 1][1];
            double length = 0.0;
            for (int k = 1; k < n; k++) {
                length += Math.sqrt(sq(xs[k] - xs[k - 1]) + sq(zs[k] - zs[k - 1]));
                along[k] = length;
            }
            double x0 = Double.MAX_VALUE, z0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE, z1 = -Double.MAX_VALUE;
            for (int k = 0; k < n; k++) {
                along[k] /= length;
                x0 = Math.min(x0, xs[k]);
                z0 = Math.min(z0, zs[k]);
                x1 = Math.max(x1, xs[k]);
                z1 = Math.max(z1, zs[k]);
            }
            minX = x0;
            minZ = z0;
            maxX = x1;
            maxZ = z1;
        }

        private static double catmullRom(double p0, double p1, double p2, double p3, double t) {
            return 0.5 * (2.0 * p1 + (-p0 + p2) * t + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t * t
                    + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t * t * t);
        }

        /** The nearest point of the river to (x, z): how far it is, and how wide the river is there. */
        Hit nearest(double x, double z) {
            // Far from it, say only that it is far: nothing nearer than this needs the exact distance
            double far = 450.0;
            if (x < minX - far || x > maxX + far || z < minZ - far || z > maxZ + far) return new Hit(Math.max(far, 1000.0), sourceHalfWidth);
            double best = Double.MAX_VALUE;
            double bestAlong = 0.0;
            for (int k = 0; k < xs.length - 1; k++) {
                double ax = xs[k], az = zs[k], bx = xs[k + 1], bz = zs[k + 1];
                double ex = bx - ax, ez = bz - az;
                double l2 = ex * ex + ez * ez;
                double t = l2 > 0.0 ? Mth.clamp(((x - ax) * ex + (z - az) * ez) / l2, 0.0, 1.0) : 0.0;
                double d2 = sq(x - (ax + t * ex)) + sq(z - (az + t * ez));
                if (d2 < best) {
                    best = d2;
                    bestAlong = along[k] + (along[k + 1] - along[k]) * t;
                }
            }
            return new Hit(Math.sqrt(best), sourceHalfWidth + (mouthHalfWidth - sourceHalfWidth) * bestAlong);
        }
    }
}
