package com.lotrmod.worldgen;

import com.lotrmod.LOTRMod;
import com.lotrmod.worldgen.feature.AspenFeature;
import com.lotrmod.worldgen.feature.ShirePineFeature;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registration for custom world generation components
 */
public class LOTRWorldGen {
    // Deferred registers for chunk generators, biome sources and density functions
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, LOTRMod.MODID);

    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, LOTRMod.MODID);

    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, LOTRMod.MODID);

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, LOTRMod.MODID);

    // Register our custom chunk generator
    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<MiddleEarthChunkGenerator>> MIDDLEEARTH_CHUNK_GENERATOR =
            CHUNK_GENERATORS.register("middleearth", () -> MiddleEarthChunkGenerator.CODEC);

    // Register our custom biome source
    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<MiddleEarthBiomeSource>> MIDDLEEARTH_BIOME_SOURCE =
            BIOME_SOURCES.register("middleearth", () -> MiddleEarthBiomeSource.CODEC);

    // The map-driven climate values the noise settings build terrain from
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<ClimateDensityFunction>> CLIMATE =
            DENSITY_FUNCTION_TYPES.register("climate", () -> ClimateDensityFunction.DATA_CODEC);

    // Reshapes the terrain for the mountain ranges and the rivers
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<TerrainBlendDensityFunction>> TERRAIN_BLEND =
            DENSITY_FUNCTION_TYPES.register("terrain_blend", () -> TerrainBlendDensityFunction.DATA_CODEC);

    // The Shire's trees, ported from the original mod's generators
    public static final DeferredHolder<Feature<?>, AspenFeature> ASPEN =
            FEATURES.register("aspen", () -> new AspenFeature(AspenFeature.Config.CODEC));
    public static final DeferredHolder<Feature<?>, ShirePineFeature> SHIRE_PINE =
            FEATURES.register("shire_pine", () -> new ShirePineFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus modEventBus) {
        CHUNK_GENERATORS.register(modEventBus);
        BIOME_SOURCES.register(modEventBus);
        DENSITY_FUNCTION_TYPES.register(modEventBus);
        FEATURES.register(modEventBus);

        LOTRMod.LOGGER.info("Registered LOTR world generation components");
    }
}
