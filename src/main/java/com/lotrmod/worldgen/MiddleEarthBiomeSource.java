package com.lotrmod.worldgen;

import com.lotrmod.LOTRMod;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Assigns biomes straight from the region map: one region is one biome.
 *
 * <p>Ocean is decided first, by the landmask rather than the region map, so the
 * waterline and the ocean biome always agree — the brightness cutoff here is
 * the same 200 that {@code MiddleEarthChunkGenerator} uses to start the seabed.
 *
 * <p>There is deliberately no noise-based sub-selection. A region's colour on
 * the map is the whole answer, which means a biome boundary is exactly a region
 * boundary and nothing else.
 */
public class MiddleEarthBiomeSource extends BiomeSource {
    /** Landmask brightness at or above which a column is ocean. */
    private static final double OCEAN_BRIGHTNESS = 200.0;

    public static final MapCodec<MiddleEarthBiomeSource> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Biome.CODEC.fieldOf("ocean_biome").forGetter((MiddleEarthBiomeSource s) -> s.oceanBiome),
                    Biome.CODEC.fieldOf("fallback_biome").forGetter((MiddleEarthBiomeSource s) -> s.fallbackBiome),
                    Biome.CODEC.listOf().fieldOf("possible_biomes").forGetter((MiddleEarthBiomeSource s) -> s.regionBiomes)
            ).apply(instance, MiddleEarthBiomeSource::new)
    );

    private final Holder<Biome> oceanBiome;
    private final Holder<Biome> fallbackBiome;
    private final List<Holder<Biome>> regionBiomes;

    /** Region to biome holder, resolved once from the dimension JSON. */
    private final Map<Region, Holder<Biome>> byRegion;

    public MiddleEarthBiomeSource(Holder<Biome> oceanBiome, Holder<Biome> fallbackBiome, List<Holder<Biome>> regionBiomes) {
        this.oceanBiome = oceanBiome;
        this.fallbackBiome = fallbackBiome;
        this.regionBiomes = regionBiomes;

        // Holders arrive from the codec already registry-bound, so they are safe
        // to hand back for chunk save and network sync. Match them to regions by
        // biome path, which Region derives from its own enum name.
        this.byRegion = new EnumMap<>(Region.class);
        for (Holder<Biome> holder : regionBiomes) {
            holder.unwrapKey().ifPresent(key -> {
                ResourceLocation id = key.location();
                if (!id.getNamespace().equals(LOTRMod.MODID)) return;
                for (Region region : Region.values()) {
                    if (region.getBiomeName().equals(id.getPath())) {
                        byRegion.put(region, holder);
                        break;
                    }
                }
            });
        }

        LOTRMod.LOGGER.info("MiddleEarthBiomeSource: resolved {} of {} region biomes",
                byRegion.size(), Region.biomeRegions().length);
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(regionBiomes.stream(), Stream.of(oceanBiome, fallbackBiome));
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        // Biome coords are quart-resolution: one step is 4 blocks.
        int worldX = x << 2;
        int worldZ = z << 2;

        if (isOcean(worldX, worldZ)) return oceanBiome;

        Holder<Biome> biome = byRegion.get(getRegion(worldX, worldZ));
        return biome != null ? biome : fallbackBiome;
    }

    /** Whether the landmask marks this column as ocean. */
    private boolean isOcean(int worldX, int worldZ) {
        return LandmaskLoader.isLoaded()
                && LandmaskLoader.getInterpolatedBrightness(worldX, worldZ) > OCEAN_BRIGHTNESS;
    }

    /**
     * The region at a world position. Falls back to Eriador only if the region
     * map failed to load, which also shows up as {@code Region map: false} on
     * the debug screen.
     */
    public Region getRegion(int worldX, int worldZ) {
        if (!RegionMapLoader.isLoaded()) return Region.ERIADOR;
        return RegionMapLoader.getRegion(worldX, worldZ);
    }
}
