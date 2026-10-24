package com.lotrmod.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.List;

/**
 * Terrain for the Middle-earth dimension: vanilla's overworld terrain, steered
 * by the maps.
 *
 * <p>The ground's shape — the height splines, 3D noise, caves, aquifers and ore
 * veins — is vanilla's, from {@code noise_settings/middleearth.json}, except
 * inside the mountain ranges, which keep their own shape ({@link MountainTerrain}),
 * and along the rivers, which cut their own channels ({@link RiverTerrain}).
 * The maps come in through two of the climate values vanilla shapes terrain from,
 * continentalness and erosion (see {@link MiddleEarthClimate}): the landmask puts
 * the coast where it draws it, and each region sets how rugged its ground is.
 * The surface rule in the same JSON finishes each region's biome with its own
 * ground.
 *
 * <p>This class adds water and beaches. River channels, marsh pools, Gondor's wetland pools and
 * Rivendell's river are filled with water up to the waterline, since vanilla's aquifer only
 * sometimes floods ground cut below sea level.
 *
 * <p>Vanilla makes beaches out of a beach biome; here every inch of land
 * belongs to a region, so low ground near the landmask's coast is turned to
 * sand after the surface rule has run.
 *
 * <p>Structures are vanilla's business. Vanilla only places a structure where
 * the biome source can produce one of its biomes, and none of vanilla's own
 * name a Middle-earth biome, so the only structure here is the mod's own hobbit
 * hole.
 */
public class MiddleEarthChunkGenerator extends NoiseBasedChunkGenerator {
    public static final MapCodec<MiddleEarthChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(gen -> gen.generatorSettings())
            ).apply(instance, MiddleEarthChunkGenerator::new)
    );

    /** Deepest a river or marsh is filled with water, from the waterline down. */
    private static final int FLOOD_MAX_DEPTH = 16;
    /** Marsh pools fill where the ground is at least this much marsh. */
    private static final double MARSH_FLOOD_WEIGHT = 0.5;

    /** Beaches only form on ground at most this far above sea level. */
    private static final int BEACH_MAX_RISE = 3;
    /** ... and no more than this far below it, under the water's edge. */
    private static final int BEACH_MAX_DEPTH = 4;
    /** Depth of sand on a beach; sandstone beneath it, as in vanilla. */
    private static final int BEACH_SAND_DEPTH = 4;
    private static final int BEACH_SANDSTONE_DEPTH = 3;

    public MiddleEarthChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource, settings);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {}

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        // Water first, so the surface rule sees it and gives river beds and
        // marsh pools their underwater ground
        flood(chunk);
        super.buildSurface(level, structureManager, random, chunk);
        addBeaches(chunk);
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager,
            StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        super.applyCarvers(level, seed, random, biomeManager, structureManager, chunk, step);
        // A cave or canyon cut under a river can take its water with it
        flood(chunk);
    }

    /**
     * Fills river channels and marsh pools with water up to the waterline.
     * Vanilla's aquifer only sometimes floods ground cut below sea level, so a
     * river left to it comes out dry in stretches. Only open air is filled, down
     * to the ground, and never more than {@link #FLOOD_MAX_DEPTH} deep, so a cave
     * mouth in a river bed isn't flooded all the way down.
     */
    private void flood(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int waterTop = getSeaLevel() - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState water = Blocks.WATER.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkPos.getMinBlockX() + x;
                int worldZ = chunkPos.getMinBlockZ() + z;
                if (!isWet(worldX, worldZ)) continue;

                for (int depth = 0; depth < FLOOD_MAX_DEPTH; depth++) {
                    pos.set(worldX, waterTop - depth, worldZ);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.isAir()) {
                        chunk.setBlockState(pos, water, false);
                    } else if (!state.getFluidState().is(FluidTags.WATER)) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Whether ground below the waterline here is under water: a river, a marsh, Gondor's wetlands, the Bruinen,
     * or Lothlórien's streams and pools.
     */
    private static boolean isWet(int x, int z) {
        if (MiddleEarthClimate.marshWeight(x, z) > MARSH_FLOOD_WEIGHT) return true;
        double gondor = MiddleEarthClimate.gondorWeight(x, z);
        if (gondor > MARSH_FLOOD_WEIGHT && gondor * GondorTerrain.wetWeight(x, z) > MARSH_FLOOD_WEIGHT) return true;
        if (MiddleEarthClimate.rivendellWeight(x, z) > MARSH_FLOOD_WEIGHT && RivendellTerrain.isRiver(x, z)) return true;
        if (MiddleEarthClimate.lothlorienWeight(x, z) > MARSH_FLOOD_WEIGHT && LothlorienTerrain.isWater(x, z)) return true;
        RiverMap.Sample river = RiverMap.sample(x, z);
        return river != null && RiverTerrain.isWater(river);
    }

    /**
     * Turns low ground along the coast to sand. Only soil is swapped — grass,
     * dirt, gravel and the like — so where the coast is rock, it stays a rocky
     * shore, as vanilla's stony shores do.
     */
    private void addBeaches(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int seaLevel = getSeaLevel();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkPos.getMinBlockX() + x;
                int worldZ = chunkPos.getMinBlockZ() + z;

                double shore = MiddleEarthClimate.coastDistance(worldX, worldZ);
                if (shore < 0 || shore >= MiddleEarthClimate.beachWidth(worldX, worldZ)) continue;

                int top = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                if (top > seaLevel + BEACH_MAX_RISE || top < seaLevel - BEACH_MAX_DEPTH) continue;

                for (int depth = 0; depth < BEACH_SAND_DEPTH + BEACH_SANDSTONE_DEPTH; depth++) {
                    pos.set(worldX, top - depth, worldZ);
                    BlockState state = chunk.getBlockState(pos);
                    boolean soil = state.is(BlockTags.DIRT) || state.is(Blocks.GRAVEL) || state.is(Blocks.SNOW_BLOCK);
                    // Sandstone carries on a little way into the rock beneath
                    if (!soil && !(depth > 0 && state.is(Blocks.STONE))) break;

                    boolean overAir = chunk.getBlockState(pos.below()).isAir();
                    boolean sand = depth < BEACH_SAND_DEPTH && soil && !overAir;
                    chunk.setBlockState(pos, (sand ? Blocks.SAND : Blocks.SANDSTONE).defaultBlockState(), false);
                }
            }
        }
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        super.addDebugScreenInfo(info, random, pos);
        int x = pos.getX();
        int z = pos.getZ();

        info.add("Landmask: " + LandmaskLoader.isLoaded() + ", region map: " + RegionMapLoader.isLoaded());
        if (getBiomeSource() instanceof MiddleEarthBiomeSource src && RegionMapLoader.isLoaded()) {
            Region region = src.getRegion(x, z);
            info.add("Region: " + region.getDisplayName() + " (" + region.getShape() + ")");
            ShireZones.Zone zone = ShireZones.zone(x, z, region);
            if (zone != ShireZones.Zone.NONE) {
                info.add(String.format("Shire: %s, %.0f blocks from the border", zone, ShireZones.borderDistance(x, z)));
            }
        }
        info.add(String.format("Shore distance: %.1f", MiddleEarthClimate.coastDistance(x, z)));
        info.add(String.format("Climate: continentalness %.3f, erosion %.3f ± %.3f",
                MiddleEarthClimate.continentalness(x, z),
                MiddleEarthClimate.erosion(x, z),
                MiddleEarthClimate.erosionSpread(x, z)));
        RiverMap.Sample river = RiverMap.sample(x, z);
        if (river != null) {
            info.add(String.format("River: %s, %.0f blocks away, %.0f wide",
                    river.river().getDisplayName(), river.distance(), river.width()));
        }
        double marsh = MiddleEarthClimate.marshWeight(x, z);
        if (marsh > 0.0) {
            info.add(String.format("Marsh: %.0f%%, height %.1f", marsh * 100.0, MarshTerrain.height(x, z)));
        }
        double shire = MiddleEarthClimate.shireWeight(x, z);
        if (shire > 0.0) {
            info.add(String.format("Downs: %.0f%%, height %.1f", shire * 100.0, ShireTerrain.height(x, z)));
        }
        double rohan = MiddleEarthClimate.rohanWeight(x, z);
        if (rohan > 0.0) {
            info.add(String.format("Plains: %.0f%%, height %.1f, moisture %.2f",
                    rohan * 100.0, RohanTerrain.height(x, z), RohanTerrain.moisture(x, z)));
        }
        double deadLands = MiddleEarthClimate.deadLandsWeight(x, z);
        if (deadLands > 0.0) {
            info.add(String.format("Dead Lands: %.0f%%, height %.1f", deadLands * 100.0, DeadLandsTerrain.height(x, z)));
        }
        double gondor = MiddleEarthClimate.gondorWeight(x, z);
        if (gondor > 0.0) {
            info.add(String.format("Gondor: %.0f%% %s, height %.1f",
                    gondor * 100.0, GondorTerrain.zone(x, z), GondorTerrain.height(x, z)));
        }
        double lothlorien = MiddleEarthClimate.lothlorienWeight(x, z);
        if (lothlorien > 0.0) {
            info.add(String.format("Lothlórien: %.0f%% %s, height %.1f",
                    lothlorien * 100.0, LothlorienTerrain.zone(x, z), LothlorienTerrain.height(x, z)));
        }
        double arnor = MiddleEarthClimate.arnorWeight(x, z);
        if (arnor > 0.0) {
            info.add(String.format("Arnor: %.0f%% %s, height %.1f, ground %.1f",
                    arnor * 100.0, ArnorTerrain.zone(x, z), ArnorTerrain.height(x, z), ArnorTerrain.groundHeight(x, z)));
        }
        double rivendell = MiddleEarthClimate.rivendellWeight(x, z);
        if (rivendell > 0.0) {
            info.add(String.format("Rivendell: %.0f%% %s, height %.1f",
                    rivendell * 100.0, RivendellTerrain.zone(x, z), RivendellTerrain.height(x, z)));
        }
        double mountain = MiddleEarthClimate.mountainWeight(x, z);
        if (mountain > 0.0) {
            info.add(String.format("Mountain: %.0f%%, height %.1f", mountain * 100.0, MountainTerrain.height(x, z)));
        }
    }
}
