package com.lotrmod.worldgen;

import com.lotrmod.LOTRMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads and provides access to the region map that defines where each region is located
 */
public class RegionMapLoader {
    private static BufferedImage regionMapImage;
    private static int imageWidth;
    private static int imageHeight;
    private static boolean loaded = false;

    /**
     * The region of every pixel, by ordinal, row-major. Matching a colour to a
     * region means measuring its distance to every region's colour, far too slow
     * to repeat for each of the thousands of lookups a chunk makes, so it is done
     * once per pixel on load.
     */
    private static byte[] regionIndex;
    private static final Region[] REGIONS = Region.values();

    public static final int BLOCKS_PER_PIXEL = 16; // Same scale as landmask

    public static void loadRegionMap(ResourceManager resourceManager) {
        // Try method 1: Load from resource manager (production)
        if (tryLoadFromResourceManager(resourceManager)) {
            return;
        }

        // Try method 2: Load from filesystem (development fallback)
        if (tryLoadFromFilesystem()) {
            return;
        }

        // Both methods failed - use fallback
        LOTRMod.LOGGER.error("All region map loading methods failed!");
        createFallbackImage();
    }

    private static boolean tryLoadFromResourceManager(ResourceManager resourceManager) {
        try {
            ResourceLocation regionMapLocation = ResourceLocation.fromNamespaceAndPath(
                    LOTRMod.MODID,
                    "maps/middleearth_regions.png"
            );

            LOTRMod.LOGGER.info("Attempting to load region map from resource manager...");

            Optional<Resource> resourceOpt = resourceManager.getResource(regionMapLocation);

            if (resourceOpt.isEmpty()) {
                LOTRMod.LOGGER.warn("Region map not found in resource manager");
                return false;
            }

            Resource resource = resourceOpt.get();

            BufferedImage image;
            try (InputStream stream = resource.open()) {
                image = ImageIO.read(stream);
            }

            if (image == null) {
                LOTRMod.LOGGER.error("Failed to read region map image from resource manager");
                return false;
            }

            setImage(image);

            LOTRMod.LOGGER.info("========================================");
            LOTRMod.LOGGER.info("REGION MAP LOADED FROM RESOURCE MANAGER!");
            LOTRMod.LOGGER.info("Image size: {}x{} pixels", imageWidth, imageHeight);
            LOTRMod.LOGGER.info("World size: {}x{} blocks",
                    imageWidth * BLOCKS_PER_PIXEL,
                    imageHeight * BLOCKS_PER_PIXEL);
            LOTRMod.LOGGER.info("========================================");

            return true;

        } catch (Exception e) {
            LOTRMod.LOGGER.warn("Error loading region map from resource manager: {}", e.getMessage());
            return false;
        }
    }

    private static boolean tryLoadFromFilesystem() {
        try {
            LOTRMod.LOGGER.info("Attempting to load region map from filesystem (development mode)...");

            // Try multiple possible locations
            String[] possiblePaths = {
                    "src/main/resources/data/lotrmod/maps/middleearth_regions.png",
                    "../src/main/resources/data/lotrmod/maps/middleearth_regions.png",
                    "../../src/main/resources/data/lotrmod/maps/middleearth_regions.png"
            };

            for (String pathStr : possiblePaths) {
                Path path = Paths.get(pathStr);
                LOTRMod.LOGGER.info("Trying path: {}", path.toAbsolutePath());

                if (Files.exists(path)) {
                    LOTRMod.LOGGER.info("Found file at: {}", path.toAbsolutePath());

                    BufferedImage image;
                    try (InputStream stream = Files.newInputStream(path)) {
                        image = ImageIO.read(stream);
                    }

                    if (image != null) {
                        setImage(image);

                        LOTRMod.LOGGER.info("========================================");
                        LOTRMod.LOGGER.info("REGION MAP LOADED FROM FILESYSTEM!");
                        LOTRMod.LOGGER.info("Path: {}", path.toAbsolutePath());
                        LOTRMod.LOGGER.info("Image size: {}x{} pixels", imageWidth, imageHeight);
                        LOTRMod.LOGGER.info("World size: {}x{} blocks",
                                imageWidth * BLOCKS_PER_PIXEL,
                                imageHeight * BLOCKS_PER_PIXEL);
                        LOTRMod.LOGGER.info("========================================");

                        return true;
                    }
                }
            }

            LOTRMod.LOGGER.warn("Region map file not found in any filesystem location");
            return false;

        } catch (Exception e) {
            LOTRMod.LOGGER.error("Error loading region map from filesystem", e);
            return false;
        }
    }

    private static void createFallbackImage() {
        LOTRMod.LOGGER.warn("========================================");
        LOTRMod.LOGGER.warn("Using fallback region map (all ocean)");
        LOTRMod.LOGGER.warn("All regions will default to OCEAN");
        LOTRMod.LOGGER.warn("========================================");

        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 256; x++) {
            for (int y = 0; y < 256; y++) {
                image.setRGB(x, y, 0xFFFFFF); // White = Ocean
            }
        }
        setImage(image);
        loaded = false;
    }

    /** Installs a freshly read map and indexes its pixels by region. */
    private static void setImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] index = new byte[width * height];
        Map<Integer, Region> byColour = new HashMap<>();
        int[] row = new int[width];
        for (int z = 0; z < height; z++) {
            image.getRGB(0, z, width, 1, row, 0, width);
            for (int x = 0; x < width; x++) {
                Region region = byColour.computeIfAbsent(row[x] & 0xFFFFFF,
                        rgb -> Region.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
                index[z * width + x] = (byte) region.ordinal();
            }
        }

        regionMapImage = image;
        imageWidth = width;
        imageHeight = height;
        regionIndex = index;
        loaded = true;
    }

    /**
     * The region at a world position: the nearest pixel's, so a region keeps
     * its exact colour from the map rather than blending into its neighbours.
     *
     * @param worldX The X coordinate in world space
     * @param worldZ The Z coordinate in world space
     * @return The region at this position
     */
    public static Region getRegion(int worldX, int worldZ) {
        if (!loaded || regionIndex == null) {
            return Region.OCEAN;
        }

        // Pixel centres sit on whole pixel coordinates, centred on world 0,0
        int pixelX = (int) Math.floor(worldX / (double) BLOCKS_PER_PIXEL + imageWidth / 2.0 + 0.5);
        int pixelZ = (int) Math.floor(worldZ / (double) BLOCKS_PER_PIXEL + imageHeight / 2.0 + 0.5);
        return getRegionAtPixel(pixelX, pixelZ);
    }

    /**
     * Get the interpolated color at a world position (for debug/visualization)
     * Uses bilinear interpolation for smooth color transitions
     *
     * @param worldX The X coordinate in world space
     * @param worldZ The Z coordinate in world space
     * @return The interpolated RGB color as a packed int (0xRRGGBB)
     */
    public static int getInterpolatedColor(int worldX, int worldZ) {
        if (!loaded || regionMapImage == null) {
            return 0xFFFFFF; // White for ocean
        }

        // Use floating-point division for sub-pixel sampling
        double exactPixelX = worldX / (double) BLOCKS_PER_PIXEL;
        double exactPixelZ = worldZ / (double) BLOCKS_PER_PIXEL;

        // Convert to image coordinates (centered at 0,0)
        exactPixelX += imageWidth / 2.0;
        exactPixelZ += imageHeight / 2.0;

        // Get the 4 surrounding pixel coordinates
        int x0 = (int) Math.floor(exactPixelX);
        int z0 = (int) Math.floor(exactPixelZ);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        // Get fractional parts
        double fx = exactPixelX - x0;
        double fz = exactPixelZ - z0;

        // Sample 4 surrounding pixels
        int[] c00 = getPixelRGB(x0, z0); // top-left
        int[] c10 = getPixelRGB(x1, z0); // top-right
        int[] c01 = getPixelRGB(x0, z1); // bottom-left
        int[] c11 = getPixelRGB(x1, z1); // bottom-right

        // Bilinear interpolation for each color channel
        int r = (int) (
                c00[0] * (1 - fx) * (1 - fz) +
                        c10[0] * fx * (1 - fz) +
                        c01[0] * (1 - fx) * fz +
                        c11[0] * fx * fz
        );

        int g = (int) (
                c00[1] * (1 - fx) * (1 - fz) +
                        c10[1] * fx * (1 - fz) +
                        c01[1] * (1 - fx) * fz +
                        c11[1] * fx * fz
        );

        int b = (int) (
                c00[2] * (1 - fx) * (1 - fz) +
                        c10[2] * fx * (1 - fz) +
                        c01[2] * (1 - fx) * fz +
                        c11[2] * fx * fz
        );

        // Pack RGB into single int
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Get region at a specific pixel coordinate with bounds checking
     *
     * @param pixelX The X pixel coordinate
     * @param pixelZ The Z pixel coordinate
     * @return The region at this pixel, or OCEAN if out of bounds
     */
    static Region getRegionAtPixel(int pixelX, int pixelZ) {
        byte[] index = regionIndex;
        if (index == null || pixelX < 0 || pixelX >= imageWidth || pixelZ < 0 || pixelZ >= imageHeight) {
            return Region.OCEAN;
        }
        return REGIONS[index[pixelZ * imageWidth + pixelX]];
    }

    /**
     * Get RGB components at a specific pixel coordinate with bounds checking
     *
     * @param pixelX The X pixel coordinate
     * @param pixelZ The Z pixel coordinate
     * @return Array of [r, g, b] values (0-255), or white if out of bounds
     */
    private static int[] getPixelRGB(int pixelX, int pixelZ) {
        if (pixelX < 0 || pixelX >= imageWidth || pixelZ < 0 || pixelZ >= imageHeight) {
            return new int[]{255, 255, 255}; // White for out of bounds
        }

        int rgb = regionMapImage.getRGB(pixelX, pixelZ);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        return new int[]{r, g, b};
    }

    public static boolean isLoaded() {
        return loaded;
    }

    static int getImageWidth() {
        return imageWidth;
    }

    static int getImageHeight() {
        return imageHeight;
    }

    public static int getWorldWidth() {
        return imageWidth * BLOCKS_PER_PIXEL;
    }

    public static int getWorldHeight() {
        return imageHeight * BLOCKS_PER_PIXEL;
    }
}
