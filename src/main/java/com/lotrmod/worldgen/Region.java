package com.lotrmod.worldgen;

import java.awt.Color;

/**
 * Every region of Middle-earth, keyed by its exact RGB colour in the region map.
 *
 * <p>The region map decides where a region is; this enum decides what that
 * means. One region is one biome, whose id is the enum name lowercased, and
 * each region has a {@link Shape} that sets the vanilla climate values its
 * terrain is generated from. What the ground is finished with lives with the
 * biome instead, in the surface rule of {@code noise_settings/middleearth.json}.
 *
 * <p>{@link #OCEAN} is the one exception: ocean is decided by the landmask rather
 * than the region map, and it resolves to the dimension's {@code ocean_biome}
 * ({@code lotrmod:ocean}) instead of a per-region biome.
 */
public enum Region {
    // ──────────────────────────────────────────────────────────────────────────
    //                            colour   display name          shape
    // ──────────────────────────────────────────────────────────────────────────
    LINDON                   (0x3FA9C6, "Lindon",                   Shape.FOREST),
    BLUE_MOUNTAINS           (0x4B6A88, "Blue Mountains",           Shape.MOUNTAIN),
    ERIADOR                  (0x7FA36C, "Eriador",                  Shape.HILLY),
    ARNOR                    (0x5E7C5A, "Arnor",                    Shape.HILLY),
    MISTY_MOUNTAINS          (0x8A8F94, "Misty Mountains",          Shape.MOUNTAIN),
    GREY_MOUNTAINS           (0x6F7377, "Grey Mountains",           Shape.MOUNTAIN),
    WHITE_MOUNTAINS          (0xC9CED3, "White Mountains",          Shape.MOUNTAIN),
    MOUNTAINS_OF_SHADOW      (0x2B2B2B, "Mountains of Shadow",      Shape.MOUNTAIN),
    GONDOR                   (0x6E8F6A, "Gondor",                   Shape.FLAT),
    HARAD                    (0xC28A3A, "Harad",                    Shape.DESERT),
    LOTHLORIEN               (0x7FBF8A, "Lothlórien",               Shape.FOREST),
    MIRKWOOD                 (0x2F5D3A, "Mirkwood",                 Shape.FOREST),
    DALE                     (0x9C7A4A, "Dale",                     Shape.HILLY),
    EREBOR                   (0x5A4A3F, "Erebor",                   Shape.MOUNTAIN),
    IRON_HILLS               (0x3F3F3F, "Iron Hills",               Shape.MOUNTAIN),
    ROHAN                    (0xB6A85E, "Rohan",                    Shape.FLAT),
    MORDOR                   (0x4A1F1F, "Mordor",                   Shape.MIXED),
    RHUN                     (0x8C4A2F, "Rhûn",                     Shape.FLAT),
    FANGORN_FOREST           (0x3E6B4E, "Fangorn Forest",           Shape.FOREST),
    ANDUIN_RIVER             (0x4FA3C7, "Anduin River",             Shape.RIVER),
    VALE_OF_ANDUIN           (0x9FAE9A, "Vale of Anduin",           Shape.MARSH),
    DEAD_LANDS               (0x8B7A5E, "Dead Lands",               Shape.FLAT),
    CELDUIN                  (0x6FA8C8, "Celduin",                  Shape.RIVER),
    EASTERN_RHOVANIAN_PLAINS (0xA89F7A, "Eastern Rhovanian Plains", Shape.FLAT),
    SEA_OF_RHUN              (0x5C7F9E, "Sea of Rhûn",              Shape.LAKE),
    FORODWAITH               (0xB7C3CC, "Forodwaith",               Shape.MIXED),
    THE_SHIRE                (0x8FBF6A, "The Shire",                Shape.HILLY),
    RIVENDELL                (0x6FAE9A, "Rivendell",                Shape.MIXED),
    OCEAN                    (0xFFFFFF, "Ocean",                    Shape.FLAT);

    /**
     * How a region's terrain is shaped, as the climate values it feeds vanilla's
     * overworld terrain: <b>continentalness</b>, how far inland the ground sits
     * (higher lifts it), and <b>erosion</b>, from -1 (rugged peaks) to 1 (flat).
     * Erosion wanders by up to {@code erosionSpread} either side of the centre
     * on vanilla's own erosion noise, and vanilla's ridge noise adds the peaks
     * and valleys, so a region is varied inside rather than one uniform shape.
     */
    public enum Shape {
        //          continentalness  erosion  erosion spread
        MOUNTAIN   ( 0.55,           -0.75,   0.10),
        HILLY      ( 0.25,            0.00,   0.25),
        FLAT       ( 0.15,            0.30,   0.15),
        FOREST     ( 0.20,            0.10,   0.25),
        MIXED      ( 0.25,           -0.10,   0.35),
        DESERT     ( 0.20,            0.05,   0.30),
        // Flat and low; the ground itself comes from MarshTerrain
        MARSH      ( 0.15,            0.30,   0.15),
        // Rivers aren't painted on the region map, so these values go unused:
        // a river region's biome is laid along its channel from the river map
        RIVER      (-0.12,            0.60,   0.10),
        // Low enough to drop below sea level and fill as an inland sea
        LAKE       (-0.30,            0.30,   0.10);

        private final double continentalness;
        private final double erosion;
        private final double erosionSpread;

        Shape(double continentalness, double erosion, double erosionSpread) {
            this.continentalness = continentalness;
            this.erosion = erosion;
            this.erosionSpread = erosionSpread;
        }

        public double getContinentalness() {
            return continentalness;
        }

        public double getErosion() {
            return erosion;
        }

        public double getErosionSpread() {
            return erosionSpread;
        }
    }

    private final Color color;
    private final String displayName;
    private final Shape shape;
    private final String biomeName;

    Region(int hex, String displayName, Shape shape) {
        this.color = new Color(hex);
        this.displayName = displayName;
        this.shape = shape;
        this.biomeName = name().toLowerCase(java.util.Locale.ROOT);
    }

    public Color getColor() {
        return color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Shape getShape() {
        return shape;
    }

    /**
     * The path of this region's biome id, e.g. {@code the_shire} for
     * {@code lotrmod:the_shire}. Always the enum name lowercased, so the JSON
     * file name and the enum constant can never drift apart.
     */
    public String getBiomeName() {
        return biomeName;
    }

    public int getRed() {
        return color.getRed();
    }

    public int getGreen() {
        return color.getGreen();
    }

    public int getBlue() {
        return color.getBlue();
    }

    /**
     * Find the region that best matches the given RGB colour.
     * Uses color distance matching with tolerance for compression artifacts.
     *
     * @param r Red component (0-255)
     * @param g Green component (0-255)
     * @param b Blue component (0-255)
     * @return The closest matching region
     */
    public static Region fromRGB(int r, int g, int b) {
        Region closestRegion = OCEAN; // Default to ocean
        double closestDistance = Double.MAX_VALUE;

        for (Region region : values()) {
            // Calculate Euclidean distance in RGB color space
            double distance = Math.sqrt(
                    Math.pow(region.getRed() - r, 2) +
                    Math.pow(region.getGreen() - g, 2) +
                    Math.pow(region.getBlue() - b, 2)
            );

            if (distance < closestDistance) {
                closestDistance = distance;
                closestRegion = region;
            }
        }

        return closestRegion;
    }

    /** Continentalness at the heart of this region. */
    public double getContinentalness() {
        return shape.getContinentalness();
    }

    /**
     * Erosion at the heart of this region. Mountain ranges set their own, which
     * is what makes some taller than others: vanilla's terrain puts the highest
     * peaks at the lowest erosion.
     */
    public double getErosion() {
        return switch (this) {
            case BLUE_MOUNTAINS, MISTY_MOUNTAINS, MOUNTAINS_OF_SHADOW -> -0.80; // jagged peaks
            case WHITE_MOUNTAINS, GREY_MOUNTAINS -> -0.65;
            case IRON_HILLS, EREBOR -> -0.45;                                  // high hills
            default -> shape.getErosion();
        };
    }

    /** How far erosion wanders either side of {@link #getErosion()}. */
    public double getErosionSpread() {
        return shape.getErosionSpread();
    }

    /** Whether this region is a marsh, built with {@link MarshTerrain}. */
    public boolean isMarsh() {
        return shape == Shape.MARSH;
    }

    /** Whether this region is a mountain range, built with {@link MountainTerrain}. */
    public boolean isMountain() {
        return shape == Shape.MOUNTAIN;
    }

    /**
     * Height of this range's ridgelines in {@link MountainTerrain}, in blocks
     * above its baseline. Zero for anything that isn't a mountain region.
     */
    public double getRidgeHeight() {
        return switch (this) {
            case BLUE_MOUNTAINS, MISTY_MOUNTAINS, MOUNTAINS_OF_SHADOW -> 110.0;
            case WHITE_MOUNTAINS, GREY_MOUNTAINS -> 85.0;
            case IRON_HILLS, EREBOR -> 60.0;
            default -> 0.0;
        };
    }

    /** Every region that has a biome of its own — i.e. all but {@link #OCEAN}. */
    public static Region[] biomeRegions() {
        Region[] all = values();
        Region[] out = new Region[all.length - 1];
        int i = 0;
        for (Region r : all) {
            if (r != OCEAN) out[i++] = r;
        }
        return out;
    }
}
