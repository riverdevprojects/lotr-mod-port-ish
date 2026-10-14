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
import java.util.Optional;

/**
 * Loads and provides access to the landmask texture that defines where land should generate
 */
public class LandmaskLoader {
    private static BufferedImage landmaskImage;
    private static int imageWidth;
    private static int imageHeight;
    private static boolean loaded = false;

    /**
     * Signed distance from each pixel to the coastline, in blocks: positive on
     * land, negative at sea. Derived from the landmask on load, so terrain can
     * ramp down to the shore over a real distance instead of over the few
     * blocks it takes the (essentially black-and-white) mask to change colour.
     */
    private static float[] shoreDistance;

    public static final int BLOCKS_PER_PIXEL = 16;

    /** Pixel brightness above which the landmask counts as sea. */
    public static final double SEA_BRIGHTNESS = 200.0;

    /** Shore distance reported outside the map: open sea, well offshore. */
    private static final float OUTSIDE_MAP_DISTANCE = -1024.0f;

    public static void loadLandmask(ResourceManager resourceManager) {
        // Try method 1: Load from resource manager (production)
        if (tryLoadFromResourceManager(resourceManager)) {
            return;
        }
        
        // Try method 2: Load from filesystem (development fallback)
        if (tryLoadFromFilesystem()) {
            return;
        }
        
        // Both methods failed - use fallback
        LOTRMod.LOGGER.error("All landmask loading methods failed!");
        createFallbackImage();
    }

    private static boolean tryLoadFromResourceManager(ResourceManager resourceManager) {
        try {
            ResourceLocation landmaskLocation = ResourceLocation.fromNamespaceAndPath(
                LOTRMod.MODID, 
                "maps/middleearth_landmask.png"
            );

            LOTRMod.LOGGER.info("Attempting to load landmask from resource manager...");

            Optional<Resource> resourceOpt = resourceManager.getResource(landmaskLocation);

            if (resourceOpt.isEmpty()) {
                LOTRMod.LOGGER.warn("Landmask not found in resource manager");
                return false;
            }

            Resource resource = resourceOpt.get();
            
            try (InputStream stream = resource.open()) {
                landmaskImage = ImageIO.read(stream);
            }

            if (landmaskImage == null) {
                LOTRMod.LOGGER.error("Failed to read landmask image from resource manager");
                return false;
            }

            imageWidth = landmaskImage.getWidth();
            imageHeight = landmaskImage.getHeight();
            shoreDistance = computeShoreDistance();
            loaded = true;

            LOTRMod.LOGGER.info("========================================");
            LOTRMod.LOGGER.info("LANDMASK LOADED FROM RESOURCE MANAGER!");
            LOTRMod.LOGGER.info("Image size: {}x{} pixels", imageWidth, imageHeight);
            LOTRMod.LOGGER.info("World size: {}x{} blocks", 
                imageWidth * BLOCKS_PER_PIXEL, 
                imageHeight * BLOCKS_PER_PIXEL);
            LOTRMod.LOGGER.info("========================================");
            
            return true;
            
        } catch (Exception e) {
            LOTRMod.LOGGER.warn("Error loading from resource manager: {}", e.getMessage());
            return false;
        }
    }

    private static boolean tryLoadFromFilesystem() {
        try {
            LOTRMod.LOGGER.info("Attempting to load landmask from filesystem (development mode)...");
            
            // Try multiple possible locations
            String[] possiblePaths = {
                "src/main/resources/data/lotrmod/maps/middleearth_landmask.png",
                "../src/main/resources/data/lotrmod/maps/middleearth_landmask.png",
                "../../src/main/resources/data/lotrmod/maps/middleearth_landmask.png"
            };
            
            for (String pathStr : possiblePaths) {
                Path path = Paths.get(pathStr);
                LOTRMod.LOGGER.info("Trying path: {}", path.toAbsolutePath());
                
                if (Files.exists(path)) {
                    LOTRMod.LOGGER.info("Found file at: {}", path.toAbsolutePath());
                    
                    try (InputStream stream = Files.newInputStream(path)) {
                        landmaskImage = ImageIO.read(stream);
                    }
                    
                    if (landmaskImage != null) {
                        imageWidth = landmaskImage.getWidth();
                        imageHeight = landmaskImage.getHeight();
                        shoreDistance = computeShoreDistance();
                        loaded = true;

                        LOTRMod.LOGGER.info("========================================");
                        LOTRMod.LOGGER.info("LANDMASK LOADED FROM FILESYSTEM!");
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
            
            LOTRMod.LOGGER.warn("Landmask file not found in any filesystem location");
            return false;
            
        } catch (Exception e) {
            LOTRMod.LOGGER.error("Error loading from filesystem", e);
            return false;
        }
    }

    private static void createFallbackImage() {
        LOTRMod.LOGGER.warn("========================================");
        LOTRMod.LOGGER.warn("Using fallback landmask (all ocean)");
        LOTRMod.LOGGER.warn("Dimension will generate as ocean only");
        LOTRMod.LOGGER.warn("========================================");
        
        landmaskImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 256; x++) {
            for (int y = 0; y < 256; y++) {
                landmaskImage.setRGB(x, y, 0xFFFFFF);
            }
        }
        imageWidth = 256;
        imageHeight = 256;
        shoreDistance = null;
        loaded = false;
    }

    public static boolean isLand(int worldX, int worldZ) {
        if (!loaded || landmaskImage == null) {
            return false;
        }

        int pixelX = (worldX / BLOCKS_PER_PIXEL) + (imageWidth / 2);
        int pixelZ = (worldZ / BLOCKS_PER_PIXEL) + (imageHeight / 2);

        if (pixelX < 0 || pixelX >= imageWidth || pixelZ < 0 || pixelZ >= imageHeight) {
            return false;
        }

        int rgb = landmaskImage.getRGB(pixelX, pixelZ);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        int brightness = (red + green + blue) / 3;

        return brightness < 128;
    }

    public static int getBrightness(int worldX, int worldZ) {
        if (!loaded || landmaskImage == null) {
            return 255;
        }

        int pixelX = (worldX / BLOCKS_PER_PIXEL) + (imageWidth / 2);
        int pixelZ = (worldZ / BLOCKS_PER_PIXEL) + (imageHeight / 2);

        if (pixelX < 0 || pixelX >= imageWidth || pixelZ < 0 || pixelZ >= imageHeight) {
            return 255;
        }

        int rgb = landmaskImage.getRGB(pixelX, pixelZ);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        return (red + green + blue) / 3;
    }

    /**
     * Get interpolated brightness at a world position using bilinear interpolation.
     * This eliminates the 16x16 chunk artifacts by smoothly blending between pixels.
     *
     * @param worldX The X coordinate in world space
     * @param worldZ The Z coordinate in world space
     * @return Interpolated brightness value (0-255)
     */
    public static double getInterpolatedBrightness(int worldX, int worldZ) {
        if (!loaded || landmaskImage == null) {
            return 255.0;
        }

        // CRITICAL: Use floating-point division for sub-pixel sampling
        // This allows us to sample BETWEEN pixels, not just at pixel centers
        double exactPixelX = worldX / (double) BLOCKS_PER_PIXEL;
        double exactPixelZ = worldZ / (double) BLOCKS_PER_PIXEL;

        // Convert to image coordinates (centered at 0,0)
        exactPixelX += imageWidth / 2.0;
        exactPixelZ += imageHeight / 2.0;

        // Get the 4 surrounding pixel coordinates for bilinear interpolation
        int x0 = (int) Math.floor(exactPixelX);
        int z0 = (int) Math.floor(exactPixelZ);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        // Get fractional parts for interpolation weights
        // fx and fz range from 0.0 to 1.0 representing position between pixels
        double fx = exactPixelX - x0;
        double fz = exactPixelZ - z0;

        // Sample 4 surrounding pixels (with bounds checking)
        double b00 = getPixelBrightness(x0, z0); // top-left
        double b10 = getPixelBrightness(x1, z0); // top-right
        double b01 = getPixelBrightness(x0, z1); // bottom-left
        double b11 = getPixelBrightness(x1, z1); // bottom-right

        // Bilinear interpolation:
        // 1. Interpolate along the top edge (between b00 and b10)
        double b0 = b00 * (1.0 - fx) + b10 * fx;

        // 2. Interpolate along the bottom edge (between b01 and b11)
        double b1 = b01 * (1.0 - fx) + b11 * fx;

        // 3. Interpolate between the two edge values
        double brightness = b0 * (1.0 - fz) + b1 * fz;

        return brightness;
    }

    /**
     * Get brightness at a specific pixel coordinate with bounds checking.
     * Returns 255 (white/ocean) for out-of-bounds pixels.
     *
     * @param pixelX The X pixel coordinate
     * @param pixelZ The Z pixel coordinate
     * @return Brightness value (0-255)
     */
    private static double getPixelBrightness(int pixelX, int pixelZ) {
        // Bounds checking - return white (ocean) for pixels outside the image
        if (pixelX < 0 || pixelX >= imageWidth || pixelZ < 0 || pixelZ >= imageHeight) {
            return 255.0;
        }

        // Sample the pixel and calculate brightness
        int rgb = landmaskImage.getRGB(pixelX, pixelZ);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        return (red + green + blue) / 3.0;
    }

    /**
     * Signed distance to the coastline at a world position, in blocks: positive
     * on land, negative at sea, zero on the shoreline. Bilinearly interpolated
     * between pixels on the same grid as {@link #getInterpolatedBrightness}.
     */
    public static double getShoreDistance(int worldX, int worldZ) {
        float[] dist = shoreDistance;
        if (!loaded || dist == null) {
            return OUTSIDE_MAP_DISTANCE;
        }

        double exactPixelX = worldX / (double) BLOCKS_PER_PIXEL + imageWidth / 2.0;
        double exactPixelZ = worldZ / (double) BLOCKS_PER_PIXEL + imageHeight / 2.0;
        int x0 = (int) Math.floor(exactPixelX);
        int z0 = (int) Math.floor(exactPixelZ);
        double fx = exactPixelX - x0;
        double fz = exactPixelZ - z0;

        double d00 = pixelShoreDistance(dist, x0,     z0);
        double d10 = pixelShoreDistance(dist, x0 + 1, z0);
        double d01 = pixelShoreDistance(dist, x0,     z0 + 1);
        double d11 = pixelShoreDistance(dist, x0 + 1, z0 + 1);

        double d0 = d00 * (1.0 - fx) + d10 * fx;
        double d1 = d01 * (1.0 - fx) + d11 * fx;
        return d0 * (1.0 - fz) + d1 * fz;
    }

    private static double pixelShoreDistance(float[] dist, int pixelX, int pixelZ) {
        if (pixelX < 0 || pixelX >= imageWidth || pixelZ < 0 || pixelZ >= imageHeight) {
            return OUTSIDE_MAP_DISTANCE;
        }
        return dist[pixelZ * imageWidth + pixelX];
    }

    /**
     * Builds the signed shore-distance field from the current image. Every pixel
     * is classed land or sea by {@link #SEA_BRIGHTNESS}, with everything outside
     * the image counted as sea. A land pixel's value is its distance to the
     * nearest sea pixel and a sea pixel's is minus its distance to the nearest
     * land pixel, each less half a pixel so the zero crossing falls midway
     * between neighbouring land and sea pixels.
     */
    private static float[] computeShoreDistance() {
        int w = imageWidth;
        int h = imageHeight;
        // One pixel of sea padding all round, so land at the image edge still
        // finds a coast.
        int pw = w + 2;
        int ph = h + 2;
        boolean[] land = new boolean[pw * ph];
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                land[(z + 1) * pw + (x + 1)] = getPixelBrightness(x, z) <= SEA_BRIGHTNESS;
            }
        }

        float[] toSea = squaredDistanceTo(land, false, pw, ph);
        float[] toLand = squaredDistanceTo(land, true, pw, ph);

        float[] out = new float[w * h];
        float half = 0.5f;
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                int p = (z + 1) * pw + (x + 1);
                out[z * w + x] = land[p]
                        ? ((float) Math.sqrt(toSea[p]) - half) * BLOCKS_PER_PIXEL
                        : -((float) Math.sqrt(toLand[p]) - half) * BLOCKS_PER_PIXEL;
            }
        }
        return out;
    }

    /**
     * Exact squared Euclidean distance (in pixels) from every pixel to the
     * nearest pixel whose land flag equals {@code target}. Felzenszwalb &
     * Huttenlocher's separable transform: one 1-D pass down each column, then
     * one along each row.
     */
    private static float[] squaredDistanceTo(boolean[] land, boolean target, int w, int h) {
        final double INF = 1e20;
        int n = Math.max(w, h);
        double[] f = new double[n];
        double[] d = new double[n];
        int[] v = new int[n];
        double[] zb = new double[n + 1];

        float[] grid = new float[w * h];
        for (int i = 0; i < grid.length; i++) {
            grid[i] = land[i] == target ? 0f : (float) INF;
        }

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) f[y] = grid[y * w + x];
            distance1d(f, h, d, v, zb);
            for (int y = 0; y < h; y++) grid[y * w + x] = (float) d[y];
        }
        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 0; x < w; x++) f[x] = grid[row + x];
            distance1d(f, w, d, v, zb);
            for (int x = 0; x < w; x++) grid[row + x] = (float) d[x];
        }
        return grid;
    }

    /** 1-D squared distance transform of {@code f[0..n)} into {@code d}. */
    private static void distance1d(double[] f, int n, double[] d, int[] v, double[] zb) {
        int k = 0;
        v[0] = 0;
        zb[0] = Double.NEGATIVE_INFINITY;
        zb[1] = Double.POSITIVE_INFINITY;
        for (int q = 1; q < n; q++) {
            double s = intersect(f, q, v[k]);
            while (s <= zb[k]) {
                k--;
                s = intersect(f, q, v[k]);
            }
            k++;
            v[k] = q;
            zb[k] = s;
            zb[k + 1] = Double.POSITIVE_INFINITY;
        }
        k = 0;
        for (int q = 0; q < n; q++) {
            while (zb[k + 1] < q) k++;
            double dq = q - v[k];
            d[q] = dq * dq + f[v[k]];
        }
    }

    private static double intersect(double[] f, int q, int p) {
        return ((f[q] + (double) q * q) - (f[p] + (double) p * p)) / (2.0 * q - 2.0 * p);
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static int getWorldWidth() {
        return imageWidth * BLOCKS_PER_PIXEL;
    }

    public static int getWorldHeight() {
        return imageHeight * BLOCKS_PER_PIXEL;
    }
}
