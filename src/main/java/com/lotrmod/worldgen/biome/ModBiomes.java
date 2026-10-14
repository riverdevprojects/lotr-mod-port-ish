package com.lotrmod.worldgen.biome;

import com.lotrmod.LOTRMod;
import com.lotrmod.worldgen.Region;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * Resource keys for the region biomes. One region is one biome, so there is no
 * table here — the key is derived from the {@link Region} constant, and the
 * matching JSON lives in {@code data/lotrmod/worldgen/biome/<region>.json}.
 *
 * <p>Deriving the key instead of listing it means a new region needs only an
 * enum constant and a JSON file; there is no third place to forget to update.
 */
public final class ModBiomes {
    private ModBiomes() {}

    /** The biome key for a region, e.g. {@code lotrmod:the_shire}. */
    public static ResourceKey<Biome> getKey(Region region) {
        return ResourceKey.create(
                Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath(LOTRMod.MODID, region.getBiomeName()));
    }
}
