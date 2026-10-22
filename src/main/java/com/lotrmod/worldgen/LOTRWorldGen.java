package com.lotrmod.worldgen;

import com.lotrmod.LOTRMod;
import com.lotrmod.worldgen.feature.AspenFeature;
import com.lotrmod.worldgen.feature.BoulderFeature;
import com.lotrmod.worldgen.feature.PartyTreeFeature;
import com.lotrmod.worldgen.feature.PoplarFeature;
import com.lotrmod.worldgen.feature.ShirePineFeature;
import com.lotrmod.worldgen.structure.HobbitHolePiece;
import com.lotrmod.worldgen.structure.HobbitHoleStructure;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
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

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, LOTRMod.MODID);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, LOTRMod.MODID);

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

    // The Shire's trees and Eriador's boulders, ported from the original mod's generators
    public static final DeferredHolder<Feature<?>, AspenFeature> ASPEN =
            FEATURES.register("aspen", () -> new AspenFeature(AspenFeature.Config.CODEC));
    public static final DeferredHolder<Feature<?>, ShirePineFeature> SHIRE_PINE =
            FEATURES.register("shire_pine", () -> new ShirePineFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, BoulderFeature> BOULDER =
            FEATURES.register("boulder", () -> new BoulderFeature(BoulderFeature.Config.CODEC));
    // Lindon's great birches
    public static final DeferredHolder<Feature<?>, PartyTreeFeature> PARTY_TREE =
            FEATURES.register("party_tree", () -> new PartyTreeFeature(PartyTreeFeature.Config.CODEC));
    // Vanilla 26.3's poplar, for Lindon's woods
    public static final DeferredHolder<Feature<?>, PoplarFeature> POPLAR =
            FEATURES.register("poplar", () -> new PoplarFeature(PoplarFeature.Config.CODEC));

    // The Shire's hobbit holes, ported from the original mod's
    public static final DeferredHolder<StructureType<?>, StructureType<HobbitHoleStructure>> HOBBIT_HOLE =
            STRUCTURE_TYPES.register("hobbit_hole", () -> () -> HobbitHoleStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> HOBBIT_HOLE_PIECE =
            STRUCTURE_PIECES.register("hobbit_hole",
                    () -> (StructurePieceType.ContextlessType) HobbitHolePiece::new);

    public static void register(IEventBus modEventBus) {
        CHUNK_GENERATORS.register(modEventBus);
        BIOME_SOURCES.register(modEventBus);
        DENSITY_FUNCTION_TYPES.register(modEventBus);
        FEATURES.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECES.register(modEventBus);

        LOTRMod.LOGGER.info("Registered LOTR world generation components");
    }
}
