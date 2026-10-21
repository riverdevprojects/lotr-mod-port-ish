package com.lotrmod.worldgen;

import com.lotrmod.LOTRMod;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Assigns biomes straight from the region map: one region is one biome.
 *
 * <p>Ocean is decided first, by the landmask rather than the region map, on the
 * same coastline the terrain is built around
 * ({@link MiddleEarthClimate#coastDistance}), so the ocean biome follows the
 * sea.
 *
 * <p>Rivers come next, from the river map: a river's channel takes its
 * region's biome ({@code lotrmod:anduin_river}, {@code lotrmod:celduin}).
 *
 * <p>There is deliberately no noise-based sub-selection. A region's colour on
 * the map is the whole answer, which means a biome boundary is exactly a region
 * boundary and nothing else.
 */
public class MiddleEarthBiomeSource extends BiomeSource {
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

    /** Feature size of the region-boundary warp, as a noise input scale. */
    private static final double WARP_SCALE = 1.0 / 40.0;
    /** How far, in blocks, the warp can shift a region lookup. */
    private static final double WARP_BLOCKS = 18.0;

    private final PerlinSimplexNoise regionWarp =
            new PerlinSimplexNoise(RandomSource.create(24680L), List.of(0, 1, 2));

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

        Holder<Biome> river = riverBiome(worldX, worldZ);
        if (river != null) return river;

        Holder<Biome> biome = byRegion.get(getRegion(worldX, worldZ));
        return biome != null ? biome : fallbackBiome;
    }

    /** The column last looked up on this thread; biomes are asked for a column at a time. */
    private final ThreadLocal<RiverColumn> lastRiverColumn = ThreadLocal.withInitial(RiverColumn::new);

    private static final class RiverColumn {
        long column = Long.MIN_VALUE;
        Holder<Biome> biome;
    }

    /** The river's biome if this is in a river channel from the river map, else null. */
    private Holder<Biome> riverBiome(int worldX, int worldZ) {
        long column = ((long) worldX << 32) | (worldZ & 0xFFFFFFFFL);
        RiverColumn last = lastRiverColumn.get();
        if (last.column != column) {
            last.column = column;
            RiverMap.Sample river = RiverMap.sample(worldX, worldZ);
            last.biome = river != null && RiverTerrain.inChannel(river) ? byRegion.get(river.river()) : null;
        }
        return last.biome;
    }

    /** Whether the landmask marks this column as ocean. */
    private boolean isOcean(int worldX, int worldZ) {
        return LandmaskLoader.isLoaded()
                && MiddleEarthClimate.coastDistance(worldX, worldZ) < 0;
    }

    /**
     * The region at a world position. Falls back to Eriador only if the region
     * map failed to load, which also shows up as {@code Region map: false} on
     * the debug screen.
     *
     * <p>The lookup is domain-warped: the region map is one pixel per 16
     * blocks, so sampled straight it draws every region boundary as a
     * staircase of 16x16 squares. Shifting the sample point by a smooth noise
     * offset bends those edges into organic lines. This is the one region
     * lookup for both biomes and surface blocks, so the two always agree.
     */
    public Region getRegion(int worldX, int worldZ) {
        if (!RegionMapLoader.isLoaded()) return Region.ERIADOR;
        int wx = (int) Math.round(regionWarp.getValue(worldX * WARP_SCALE, worldZ * WARP_SCALE, false) * WARP_BLOCKS);
        int wz = (int) Math.round(regionWarp.getValue((worldX + 7919) * WARP_SCALE, (worldZ - 7919) * WARP_SCALE, false) * WARP_BLOCKS);
        return RegionMapLoader.getRegion(worldX + wx, worldZ + wz);
    }
}
