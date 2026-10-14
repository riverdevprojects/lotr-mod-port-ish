package com.lotrmod.worldgen;

import java.awt.Color;

/**
 * Every region of Middle-earth, keyed by its exact RGB colour in the region map.
 *
 * <p>This enum is the single source of truth for the dimension: one region is one
 * biome. The region map decides where a region is, and everything the terrain
 * needs to know about that region — its shape class, its surface blocks, its
 * biome id — hangs off the enum constant.
 *
 * <p>{@link #OCEAN} is the one exception: ocean is decided by the landmask rather
 * than the region map, and it resolves to {@code minecraft:ocean}, so it has no
 * biome of its own.
 */
public enum Region {
    // ──────────────────────────────────────────────────────────────────────────
    //                            colour   display name          shape      surface
    // ──────────────────────────────────────────────────────────────────────────
    LINDON                   (0x3FA9C6, "Lindon",                   Shape.FOREST,   Surface.GRASS),
    BLUE_MOUNTAINS           (0x4B6A88, "Blue Mountains",           Shape.MOUNTAIN, Surface.STONE),
    ERIADOR                  (0x7FA36C, "Eriador",                  Shape.HILLY,    Surface.GRASS),
    ARNOR                    (0x5E7C5A, "Arnor",                    Shape.HILLY,    Surface.GRASS),
    MISTY_MOUNTAINS          (0x8A8F94, "Misty Mountains",          Shape.MOUNTAIN, Surface.STONE),
    GREY_MOUNTAINS           (0x6F7377, "Grey Mountains",           Shape.MOUNTAIN, Surface.STONE),
    WHITE_MOUNTAINS          (0xC9CED3, "White Mountains",          Shape.MOUNTAIN, Surface.STONE),
    MOUNTAINS_OF_SHADOW      (0x2B2B2B, "Mountains of Shadow",      Shape.MOUNTAIN, Surface.STONE),
    GONDOR                   (0x6E8F6A, "Gondor",                   Shape.FLAT,     Surface.GRASS),
    HARAD                    (0xC28A3A, "Harad",                    Shape.FLAT,     Surface.SAND),
    LOTHLORIEN               (0x7FBF8A, "Lothlórien",               Shape.FOREST,   Surface.GRASS),
    MIRKWOOD                 (0x2F5D3A, "Mirkwood",                 Shape.FOREST,   Surface.COARSE),
    DALE                     (0x9C7A4A, "Dale",                     Shape.HILLY,    Surface.GRASS),
    EREBOR                   (0x5A4A3F, "Erebor",                   Shape.MOUNTAIN, Surface.STONE),
    IRON_HILLS               (0x3F3F3F, "Iron Hills",               Shape.MOUNTAIN, Surface.STONE),
    ROHAN                    (0xB6A85E, "Rohan",                    Shape.FLAT,     Surface.GRASS),
    MORDOR                   (0x4A1F1F, "Mordor",                   Shape.MIXED,    Surface.GRAVEL),
    RHUN                     (0x8C4A2F, "Rhûn",                     Shape.FLAT,     Surface.GRASS),
    FANGORN_FOREST           (0x3E6B4E, "Fangorn Forest",           Shape.FOREST,   Surface.GRASS),
    ANDUIN_RIVER             (0x4FA3C7, "Anduin River",             Shape.RIVER,    Surface.GRAVEL),
    VALE_OF_ANDUIN           (0x9FAE9A, "Vale of Anduin",           Shape.FLAT,     Surface.MUD),
    DEAD_LANDS               (0x8B7A5E, "Dead Lands",               Shape.FLAT,     Surface.COARSE),
    CELDUIN                  (0x6FA8C8, "Celduin",                  Shape.RIVER,    Surface.GRAVEL),
    EASTERN_RHOVANIAN_PLAINS (0xA89F7A, "Eastern Rhovanian Plains", Shape.FLAT,     Surface.GRASS),
    SEA_OF_RHUN              (0x5C7F9E, "Sea of Rhûn",              Shape.FLAT,     Surface.GRASS),
    FORODWAITH               (0xB7C3CC, "Forodwaith",               Shape.MIXED,    Surface.SNOW),
    THE_SHIRE                (0x8FBF6A, "The Shire",                Shape.HILLY,    Surface.GRASS),
    RIVENDELL                (0x6FAE9A, "Rivendell",                Shape.MIXED,    Surface.GRASS),
    OCEAN                    (0xFFFFFF, "Ocean",                    Shape.FLAT,     Surface.GRASS);

    /**
     * How a region's terrain is shaped. Drives the noise-layer weights in
     * {@code MiddleEarthChunkGenerator}, which is what makes mountains dramatic
     * and plains gentle out of the same noise fields.
     */
    public enum Shape { MOUNTAIN, RIVER, HILLY, FLAT, FOREST, MIXED }

    /** The ground a region is finished with, above the waterline. */
    public enum Surface { GRASS, STONE, SAND, GRAVEL, MUD, COARSE, SNOW }

    private final Color color;
    private final String displayName;
    private final Shape shape;
    private final Surface surface;
    private final String biomeName;

    Region(int hex, String displayName, Shape shape, Surface surface) {
        this.color = new Color(hex);
        this.displayName = displayName;
        this.shape = shape;
        this.surface = surface;
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

    public Surface getSurface() {
        return surface;
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

    /**
     * Check if a region is a mountain region (affects terrain generation)
     */
    public boolean isMountain() {
        return shape == Shape.MOUNTAIN;
    }

    /**
     * Check if a region is a river (affects terrain generation)
     */
    public boolean isRiver() {
        return shape == Shape.RIVER;
    }

    /**
     * Check if a region is ocean/sea
     */
    public boolean isOcean() {
        return this == OCEAN || this == SEA_OF_RHUN;
    }

    /**
     * Height of this region's mountain spines, in blocks above the baseline.
     * Zero for anything that isn't a mountain region.
     */
    public double getMountainHeightModifier() {
        return switch (this) {
            case BLUE_MOUNTAINS, MISTY_MOUNTAINS, MOUNTAINS_OF_SHADOW -> 110.0; // Huge mountains
            case WHITE_MOUNTAINS, GREY_MOUNTAINS -> 85.0;  // Large mountains
            case IRON_HILLS, EREBOR -> 60.0;               // Medium mountains
            default -> 0.0;                                // Not a mountain
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
