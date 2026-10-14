package com.lotrmod.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Terrain for the Middle-earth dimension.
 *
 * <p>Shape comes from a vanilla-style noise stack (continental / erosion /
 * peaks-valleys / ridges), weighted per region so the same noise fields read as
 * dramatic in the Misty Mountains and gently rolling in the Shire. The landmask
 * PNG then decides what is land and what is sea, and the region map decides
 * which weights apply where.
 */
public class MiddleEarthChunkGenerator extends NoiseBasedChunkGenerator {
    public static final MapCodec<MiddleEarthChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(gen -> gen.generatorSettings())
            ).apply(instance, MiddleEarthChunkGenerator::new)
    );

    // Primary terrain noise layers (multi-octave for natural look)
    private final PerlinSimplexNoise continentalNoise;   // Very large scale shape
    private final PerlinSimplexNoise erosionNoise;       // Erosion / valley carving
    private final PerlinSimplexNoise peaksValleysNoise;  // Local peaks and valleys
    private final PerlinSimplexNoise detailNoise;        // Surface fine detail
    private final PerlinSimplexNoise ridgeNoise;         // Mountain ridgelines

    // Used for region boundary jitter and landmask coastline noise
    private final PerlinSimplexNoise coastlineNoise;

    private static final int SEA_LEVEL = 63;
    // Land/coastal terrain is kept at or above this — level with the water
    // surface — so the beach meets the sea without a ledge; ocean seabed is
    // allowed below sea level (see oceanFloorHeight).
    private static final int MIN_TERRAIN_HEIGHT = SEA_LEVEL;
    // Land this close to the coast and this low is finished as sand beach.
    private static final double BEACH_WIDTH = 20.0;
    private static final int BEACH_MAX_HEIGHT = SEA_LEVEL + 3;

    public MiddleEarthChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource, settings);

        RandomSource r1 = RandomSource.create(12345L);
        this.continentalNoise  = new PerlinSimplexNoise(r1, List.of(0, 1, 2, 3, 4));
        this.erosionNoise      = new PerlinSimplexNoise(r1, List.of(0, 1, 2, 3));
        this.peaksValleysNoise = new PerlinSimplexNoise(r1, List.of(0, 1, 2, 3));
        this.detailNoise       = new PerlinSimplexNoise(r1, List.of(0, 1, 2));
        this.ridgeNoise        = new PerlinSimplexNoise(r1, List.of(0, 1, 2, 3));

        RandomSource r2 = RandomSource.create(98765L);
        this.coastlineNoise = new PerlinSimplexNoise(r2, List.of(0, 1, 2));
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void createStructures(net.minecraft.core.RegistryAccess registryAccess, ChunkGeneratorStructureState state,
            StructureManager structureManager, ChunkAccess chunk,
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager templateManager) {
        // This dimension generates landscape only — no structures.
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;
                int terrainHeight = getTerrainHeight(worldX, worldZ);

                // Same (warped) lookup the biome source uses, so the ground
                // and the biome change over at the same line.
                Region region = getRegionAt(worldX, worldZ);

                for (int y = chunk.getMaxBuildHeight() - 1; y >= chunk.getMinBuildHeight(); y--) {
                    pos.set(startX + x, y, startZ + z);
                    BlockState state = chunk.getBlockState(pos);

                    if (state.is(Blocks.STONE)) {
                        BlockState surfaceBlock;
                        BlockState underBlock;
                        if (terrainHeight < SEA_LEVEL) {
                            // Submerged seabed — sand in the shallows, gravel in
                            // deeper water. Never grass, even over a land region.
                            boolean deep = terrainHeight < SEA_LEVEL - 8;
                            surfaceBlock = (deep ? Blocks.GRAVEL : Blocks.SAND).defaultBlockState();
                            underBlock = surfaceBlock;
                        } else if (terrainHeight <= BEACH_MAX_HEIGHT
                                && coastDistance(worldX, worldZ) < beachWidth(worldX, worldZ)) {
                            // Beach: sand down to the waterline, whatever the region
                            surfaceBlock = Blocks.SAND.defaultBlockState();
                            underBlock = surfaceBlock;
                        } else {
                            // Snow cap on high peaks
                            boolean isHighPeak = terrainHeight >= 160;
                            surfaceBlock = isHighPeak
                                    ? Blocks.SNOW_BLOCK.defaultBlockState()
                                    : getSurfaceBlock(region, terrainHeight);
                            underBlock = getUnderBlock(region);
                        }

                        chunk.setBlockState(pos, surfaceBlock, false);
                        pos.setY(y - 1);
                        chunk.setBlockState(pos, underBlock, false);
                        pos.setY(y - 2);
                        chunk.setBlockState(pos, underBlock, false);
                        pos.setY(y - 3);
                        chunk.setBlockState(pos, underBlock, false);
                        break;
                    }
                }
            }
        }
    }

    /**
     * The block a region is finished with. High mountain ground goes bare stone
     * above the tree line regardless of what the region would otherwise use.
     */
    private BlockState getSurfaceBlock(Region region, int terrainHeight) {
        if (region == null) return Blocks.GRASS_BLOCK.defaultBlockState();

        // Stone-capped mountains (above the tree line)
        if (terrainHeight >= 120 && region.isMountain()) return Blocks.STONE.defaultBlockState();

        return switch (region.getSurface()) {
            case STONE  -> Blocks.STONE.defaultBlockState();
            case SAND   -> Blocks.SAND.defaultBlockState();
            case GRAVEL -> Blocks.GRAVEL.defaultBlockState();
            case MUD    -> Blocks.MUD.defaultBlockState();
            case COARSE -> Blocks.COARSE_DIRT.defaultBlockState();
            case SNOW   -> Blocks.SNOW_BLOCK.defaultBlockState();
            case GRASS  -> Blocks.GRASS_BLOCK.defaultBlockState();
        };
    }

    /** The few blocks directly beneath the surface. */
    private BlockState getUnderBlock(Region region) {
        if (region == null) return Blocks.DIRT.defaultBlockState();

        return switch (region.getSurface()) {
            case STONE  -> Blocks.STONE.defaultBlockState();
            case SAND   -> Blocks.SAND.defaultBlockState();
            case GRAVEL -> Blocks.GRAVEL.defaultBlockState();
            case MUD    -> Blocks.MUD.defaultBlockState();
            // Coarse ground and snowfields are only skin-deep; dirt underneath.
            case COARSE, SNOW, GRASS -> Blocks.DIRT.defaultBlockState();
        };
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {}

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState randomState, BiomeManager biomeManager,
            StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // Vanilla carvers (cave, canyon, extra_underground) defined in biome JSONs.
        // aquifers_enabled=false in noise settings ensures no water spawns in carved voids.
        super.applyCarvers(level, seed, randomState, biomeManager, structureManager, chunk, step);

        // Carvers can gouge the shallow ocean seabed and delete surface water,
        // leaving the water one block low (visible "divots"). Re-flood any air a
        // carver opened up at or below sea level in ocean columns.
        refloodOcean(chunk);
    }

    /**
     * Restores ocean water that carvers removed. For every submerged column,
     * fills contiguous air from sea level downward with water until it reaches
     * the seabed, so the sea surface stays flat at {@link #SEA_LEVEL}.
     */
    private void refloodOcean(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();
        int minY = chunk.getMinBuildHeight();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;

                // Only ocean columns have water to restore
                if (getTerrainHeight(worldX, worldZ) >= SEA_LEVEL) continue;

                for (int y = SEA_LEVEL; y > minY; y--) {
                    pos.set(worldX, y, worldZ);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.isAir()) {
                        chunk.setBlockState(pos, Blocks.WATER.defaultBlockState(), false);
                    } else if (!state.is(Blocks.WATER)) {
                        break; // reached the solid seabed
                    }
                }
            }
        }
    }

    @Override
    public int getGenDepth() { return 384; }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random,
            StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            doFill(chunk);
            return chunk;
        });
    }

    private void doFill(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;
                int terrainTop = getTerrainHeight(worldX, worldZ);

                for (int y = chunk.getMinBuildHeight(); y <= Math.min(terrainTop, chunk.getMaxBuildHeight() - 1); y++) {
                    pos.set(startX + x, y, startZ + z);

                    if (y <= chunk.getMinBuildHeight() + 5) {
                        boolean isBedrock = (y == chunk.getMinBuildHeight())
                                || (y <= chunk.getMinBuildHeight() + 4 && Math.random() < 0.8);
                        chunk.setBlockState(pos,
                                isBedrock ? Blocks.BEDROCK.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState(),
                                false);
                    } else if (y < 0) {
                        chunk.setBlockState(pos, Blocks.DEEPSLATE.defaultBlockState(), false);
                    } else {
                        chunk.setBlockState(pos, Blocks.STONE.defaultBlockState(), false);
                    }
                }
                // Flood ocean water above a submerged seabed up to sea level.
                if (terrainTop < SEA_LEVEL) {
                    for (int y = terrainTop + 1; y <= SEA_LEVEL; y++) {
                        pos.set(startX + x, y, startZ + z);
                        chunk.setBlockState(pos, Blocks.WATER.defaultBlockState(), false);
                    }
                }
            }
        }
    }

    // ======================================================================
    // TERRAIN HEIGHT — vanilla-style noise stack
    // ======================================================================

    private int getTerrainHeight(int worldX, int worldZ) {
        return (int) Math.round(getTerrainHeightDouble(worldX, worldZ));
    }

    /**
     * Vanilla-inspired terrain height using a continental/erosion/peaks-valleys stack.
     *
     * Continental noise sets the broad elevation baseline (like vanilla continentalness).
     * Erosion noise carves valleys and flattens plains (like vanilla erosion).
     * Peaks-and-valleys noise adds local relief on top.
     * Ridge noise shapes mountain spines and passes.
     *
     * The region's shape class modulates how strongly each layer contributes, so
     * mountains are dramatic while plains stay gently rolling — all from the same
     * noise fields.
     */
    private double getTerrainHeightDouble(int worldX, int worldZ) {

        // ── Continental baseline (very large scale, smooth) ──────────────────
        double contRaw = continentalNoise.getValue(worldX / 1400.0, worldZ / 1400.0, false);
        // Shape into 0-1 so we can use it as a multiplier for regional height
        double continental = (contRaw + 1.0) / 2.0; // 0..1

        // ── Erosion (determines how flat/hilly an area feels) ─────────────────
        double erosionRaw = erosionNoise.getValue(worldX / 500.0, worldZ / 500.0, false);
        // 0 = highly eroded (flat valleys), 1 = uneroded (tall terrain)
        double erosion = (erosionRaw + 1.0) / 2.0;

        // ── Peaks and valleys (medium-frequency local relief) ─────────────────
        double pvRaw = peaksValleysNoise.getValue(worldX / 180.0, worldZ / 180.0, false);
        // Sharpen valley floors and peaks with a squishing transform
        double pv = squishPeaksValleys(pvRaw);

        // ── Fine surface detail ───────────────────────────────────────────────
        double detail = detailNoise.getValue(worldX / 45.0, worldZ / 45.0, false);

        // ── Ridge noise for mountain spines ───────────────────────────────────
        double ridgeRaw = ridgeNoise.getValue(worldX / 220.0, worldZ / 220.0, false);
        double ridge = 1.0 - Math.abs(ridgeRaw); // 0 = valley, 1 = ridge crest
        ridge = ridge * ridge; // sharpen

        // ── Get region modifiers at this location ─────────────────────────────
        // Use a 96-block grid + smoothstep to avoid visible grid artefacts
        final int GRID = 96;
        int gx0 = Math.floorDiv(worldX, GRID) * GRID;
        int gz0 = Math.floorDiv(worldZ, GRID) * GRID;
        double fx = smoothstep((double)(worldX - gx0) / GRID);
        double fz = smoothstep((double)(worldZ - gz0) / GRID);

        TerrainProfile m00 = getProfileAt(gx0,        gz0);
        TerrainProfile m10 = getProfileAt(gx0 + GRID, gz0);
        TerrainProfile m01 = getProfileAt(gx0,        gz0 + GRID);
        TerrainProfile m11 = getProfileAt(gx0 + GRID, gz0 + GRID);

        // Interpolate the modifier fields themselves (smooth transition between regions)
        double baseOffset     = bilinearInterp(m00.baseOffset,     m10.baseOffset,     m01.baseOffset,     m11.baseOffset,     fx, fz);
        double continentScale = bilinearInterp(m00.continentScale, m10.continentScale, m01.continentScale, m11.continentScale, fx, fz);
        double erosionScale   = bilinearInterp(m00.erosionScale,   m10.erosionScale,   m01.erosionScale,   m11.erosionScale,   fx, fz);
        double pvScale        = bilinearInterp(m00.pvScale,        m10.pvScale,        m01.pvScale,        m11.pvScale,        fx, fz);
        double ridgeScale     = bilinearInterp(m00.ridgeScale,     m10.ridgeScale,     m01.ridgeScale,     m11.ridgeScale,     fx, fz);
        double detailScale    = bilinearInterp(m00.detailScale,    m10.detailScale,    m01.detailScale,    m11.detailScale,    fx, fz);

        // ── Combine layers ────────────────────────────────────────────────────
        double height = SEA_LEVEL
                + baseOffset
                + continental * continentScale
                + (1.0 - erosion) * erosionScale   // high erosion = flat = subtract less
                + pv * pvScale
                + ridge * ridgeScale
                + detail * detailScale;

        // ── Landmask shapes the broad Middle-earth geography ──────────────────
        // applyLandmask returns the final height already clamped appropriately:
        // land/coast never dips below MIN_TERRAIN_HEIGHT, ocean sits below sea level.
        return applyLandmask(worldX, worldZ, height);
    }

    /**
     * Squishes the peaks-and-valleys noise so valley floors are flat and
     * ridge crests are sharp — mirrors what vanilla's PV spline does.
     */
    private double squishPeaksValleys(double raw) {
        // raw in [-1, 1]; fold negative (valleys) to compress them,
        // amplify positive (peaks) slightly
        if (raw < 0) {
            return raw * 0.5; // valleys are gentle
        } else {
            return raw * 1.2; // peaks are a bit sharper
        }
    }

    private double smoothstep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    /** Blocks inland over which land eases down to the beach. */
    private static final double COAST_RAMP = 128.0;
    /** Blocks offshore over which the seabed shelves down to full depth. */
    private static final double SHELF_WIDTH = 96.0;
    /** Extra height given to land clear of the coast, for prominence. */
    private static final double INLAND_BOOST = 8.0;

    /**
     * Uses the landmask to shape Middle-earth's geography, by distance to the
     * coastline rather than by raw mask brightness: the mask is essentially
     * black and white at 16 blocks a pixel, so brightness swings from land to
     * sea in a few blocks and anything ramped on it comes out as a sheer cliff.
     *
     * Land eases down to a beach just above the waterline over
     * {@link #COAST_RAMP} blocks. Sea gets a submerged seabed below sea level;
     * {@link #doFill} floods water above it up to {@link #SEA_LEVEL}.
     *
     * The shoreline (distance zero) is the same one MiddleEarthBiomeSource uses
     * for the ocean biome, so the water line and the biome stay in agreement.
     */
    private double applyLandmask(int worldX, int worldZ, double height) {
        if (!LandmaskLoader.isLoaded()) return Math.max(MIN_TERRAIN_HEIGHT, height);

        double shoreDist = coastDistance(worldX, worldZ);

        if (shoreDist < 0) {
            // Sea → submerged seabed (water floods above this in doFill)
            return oceanFloorHeight(worldX, worldZ, -shoreDist);
        }

        // Smoothstep is flat at both ends: a level beach at the waterline that
        // rises and then settles into the regional terrain inland.
        double t = smoothstep(Math.min(1.0, shoreDist / COAST_RAMP));
        double shoreTarget = MIN_TERRAIN_HEIGHT; // level with the water at the beach
        double inland = height + INLAND_BOOST;
        return Math.max(MIN_TERRAIN_HEIGHT, shoreTarget + (inland - shoreTarget) * t);
    }

    /**
     * Signed distance to the coast in blocks (positive on land, negative at
     * sea), sampled through a little noise so the coastline wanders off the
     * 16-block pixel grid.
     */
    private double coastDistance(int worldX, int worldZ) {
        double noiseOffX = coastlineNoise.getValue(worldX / 80.0, worldZ / 80.0, false) * 10.0;
        double noiseOffZ = coastlineNoise.getValue((worldX + 10000) / 80.0, (worldZ + 10000) / 80.0, false) * 10.0;
        return LandmaskLoader.getShoreDistance(worldX + (int) noiseOffX, worldZ + (int) noiseOffZ);
    }

    /**
     * How far inland the beach reaches here. Varied with noise because the
     * shore-distance field is interpolated in straight lines between pixels,
     * so a fixed-width beach shows those lines as its inland edge.
     */
    private double beachWidth(int worldX, int worldZ) {
        double n = coastlineNoise.getValue((worldX + 5000) / 30.0, (worldZ - 5000) / 30.0, false);
        return BEACH_WIDTH + n * 12.0;
    }

    /**
     * Height of the ocean floor {@code offshore} blocks out from the coast.
     * Slopes from a shallow shelf just below the surface down to deeper water,
     * with gentle seabed variation. Always below {@link #SEA_LEVEL} so water
     * fills above.
     */
    private double oceanFloorHeight(int worldX, int worldZ, double offshore) {
        // Ease-out rather than smoothstep: the seabed falls away straight from
        // the shore and levels off offshore, instead of lingering in the shallows.
        double u = Math.min(1.0, offshore / SHELF_WIDTH);
        double depthT = 1.0 - (1.0 - u) * (1.0 - u);
        double shelf = SEA_LEVEL - 1.0;   // one block of water at the coast
        double deep  = SEA_LEVEL - 22.0;  // deeper water offshore
        double floor = shelf + (deep - shelf) * depthT;
        // Seabed variation fades in offshore so it can't build shoals at the beach
        double bump  = coastlineNoise.getValue(worldX / 55.0, worldZ / 55.0, false) * 3.0 * depthT;
        // Keep the seabed strictly below the waterline so ocean never dries out
        return Math.min(SEA_LEVEL - 1.0, floor + bump);
    }

    // ======================================================================
    // TERRAIN PROFILES — how strongly each noise layer applies, per region
    // ======================================================================

    private static class TerrainProfile {
        double baseOffset     = 10.0;  // vertical shift above sea level
        double continentScale = 20.0;  // how much continental noise contributes
        double erosionScale   = 15.0;  // how deep erosion carves
        double pvScale        = 18.0;  // peaks-and-valleys amplitude
        double ridgeScale     =  0.0;  // mountain ridges (0 for flat/hilly regions)
        double detailScale    =  4.0;  // surface detail amplitude
    }

    private TerrainProfile getProfileAt(int worldX, int worldZ) {
        TerrainProfile m = new TerrainProfile();
        Region region = getRegionAt(worldX, worldZ);
        if (region == null) return m;

        switch (region.getShape()) {
            case MOUNTAIN -> {
                m.baseOffset     = 25.0;
                m.continentScale = 30.0;
                m.erosionScale   = 10.0;  // mountains resist erosion
                m.pvScale        = 40.0;
                m.ridgeScale     = region.getMountainHeightModifier();
                m.detailScale    =  8.0;
            }
            case RIVER -> {
                // Dry riverbeds: gently lower than surroundings but above sea level
                m.baseOffset     =  4.0;
                m.continentScale = 10.0;
                m.erosionScale   = 20.0;  // heavily eroded = low and flat
                m.pvScale        =  6.0;
                m.ridgeScale     =  0.0;
                m.detailScale    =  3.0;
            }
            case HILLY -> {
                // Rolling hills (the Shire, Eriador, Arnor, Dale)
                m.baseOffset     = 12.0;
                m.continentScale = 18.0;
                m.erosionScale   = 14.0;
                m.pvScale        = 22.0;
                m.ridgeScale     =  0.0;
                m.detailScale    =  5.0;
            }
            case FLAT -> {
                // Plains — still rolling, just less dramatic than hills
                m.baseOffset     = 10.0;
                m.continentScale = 14.0;
                m.erosionScale   = 18.0;  // more erosion = flatter valleys
                m.pvScale        = 12.0;
                m.ridgeScale     =  0.0;
                m.detailScale    =  4.0;
            }
            case FOREST -> {
                // Forest — mid-range variation, terrain defined by trees not shape
                m.baseOffset     = 10.0;
                m.continentScale = 16.0;
                m.erosionScale   = 16.0;
                m.pvScale        = 16.0;
                m.ridgeScale     =  0.0;
                m.detailScale    =  5.0;
            }
            case MIXED -> {
                // Default mixed terrain (Mordor, Forodwaith, Rivendell)
                m.baseOffset     = 10.0;
                m.continentScale = 18.0;
                m.erosionScale   = 16.0;
                m.pvScale        = 18.0;
                m.ridgeScale     =  0.0;
                m.detailScale    =  5.0;
            }
        }

        return m;
    }

    // ======================================================================
    // HELPERS
    // ======================================================================

    private Region getRegionAt(int worldX, int worldZ) {
        if (!(getBiomeSource() instanceof MiddleEarthBiomeSource src)) return null;
        return src.getRegion(worldX, worldZ);
    }

    private double bilinearInterp(double v00, double v10, double v01, double v11, double fx, double fz) {
        double v0 = v00 * (1.0 - fx) + v10 * fx;
        double v1 = v01 * (1.0 - fx) + v11 * fx;
        return v0 * (1.0 - fz) + v1 * fz;
    }

    // ======================================================================
    // OVERRIDES
    // ======================================================================

    @Override
    public int getSeaLevel() { return SEA_LEVEL; }

    @Override
    public int getMinY() { return -64; }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmapType, LevelHeightAccessor level, RandomState random) {
        return getTerrainHeight(x, z);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        int height = getTerrainHeight(x, z);
        BlockState[] states = new BlockState[level.getHeight()];

        for (int i = 0; i < states.length; i++) {
            int y = level.getMinBuildHeight() + i;
            if (y <= height) {
                states[i] = y < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
            } else {
                states[i] = Blocks.AIR.defaultBlockState();
            }
        }

        return new NoiseColumn(level.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("Middle-earth Chunk Generator");
        info.add("Landmask: " + LandmaskLoader.isLoaded());
        info.add("Region map: " + RegionMapLoader.isLoaded());

        if (RegionMapLoader.isLoaded()) {
            Region region = getRegionAt(pos.getX(), pos.getZ());
            info.add("Region: " + region.getDisplayName() + " (" + region.getShape() + ")");
        }

        if (LandmaskLoader.isLoaded()) {
            double brightness = LandmaskLoader.getInterpolatedBrightness(pos.getX(), pos.getZ());
            info.add(String.format("Landmask brightness: %.1f", brightness));
            info.add(String.format("Shore distance: %.1f", LandmaskLoader.getShoreDistance(pos.getX(), pos.getZ())));
        }

        info.add("Terrain height: " + getTerrainHeight(pos.getX(), pos.getZ()));
    }
}
