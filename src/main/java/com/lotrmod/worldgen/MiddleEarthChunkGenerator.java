package com.lotrmod.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;

/**
 * Terrain for the Middle-earth dimension: vanilla's overworld terrain, steered
 * by the maps.
 *
 * <p>Everything about the ground's shape — the height splines, 3D noise, caves,
 * aquifers and ore veins — is vanilla's, from {@code noise_settings/middleearth.json}.
 * The maps come in through two of the climate values vanilla shapes terrain from,
 * continentalness and erosion (see {@link MiddleEarthClimate}): the landmask puts
 * the coast where it draws it, and each region sets how rugged its ground is.
 * The surface rule in the same JSON finishes each region's biome with its own
 * ground.
 *
 * <p>The one thing this class adds is beaches. Vanilla makes beaches out of a
 * beach biome; here every inch of land belongs to a region, so low ground near
 * the landmask's coast is turned to sand after the surface rule has run.
 */
public class MiddleEarthChunkGenerator extends NoiseBasedChunkGenerator {
    public static final MapCodec<MiddleEarthChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(gen -> gen.generatorSettings())
            ).apply(instance, MiddleEarthChunkGenerator::new)
    );

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
    public void createStructures(RegistryAccess registryAccess, ChunkGeneratorStructureState state,
            StructureManager structureManager, ChunkAccess chunk, StructureTemplateManager templateManager) {
        // This dimension generates landscape only — no structures.
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {}

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        super.buildSurface(level, structureManager, random, chunk);
        addBeaches(chunk);
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
        }
        info.add(String.format("Shore distance: %.1f", MiddleEarthClimate.coastDistance(x, z)));
        info.add(String.format("Climate: continentalness %.3f, erosion %.3f ± %.3f",
                MiddleEarthClimate.continentalness(x, z),
                MiddleEarthClimate.erosion(x, z),
                MiddleEarthClimate.erosionSpread(x, z)));
    }
}
