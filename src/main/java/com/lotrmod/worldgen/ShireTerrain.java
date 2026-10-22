package com.lotrmod.worldgen;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;

/**
 * The shape of the Shire: soft, rolling downs, never far above or below a
 * comfortable height, with no cliffs, crags or steep valleys. The original mod
 * gave the Shire about the gentlest ground of any land biome, and vanilla's
 * terrain at any erosion is lumpier than that.
 *
 * <p>Broad swells a couple of hundred blocks across, a smaller roll on top,
 * and a little unevenness so no slope is perfectly smooth.
 * {@link TerrainBlendDensityFunction} fades the terrain over to this inside the
 * Shire, by {@link MiddleEarthClimate#shireWeight}.
 */
public final class ShireTerrain {
    private ShireTerrain() {}

    /** Height the downs roll about, in blocks. */
    private static final double BASE = 74.0;
    /** Lowest the ground gets before it is eased back up, so it stays above the waterline. */
    private static final double FLOOR = 67.0;

    private static final PerlinSimplexNoise SWELLS =
            new PerlinSimplexNoise(RandomSource.create(31415L), List.of(0, 1));
    private static final PerlinSimplexNoise ROLLS =
            new PerlinSimplexNoise(RandomSource.create(31416L), List.of(0, 1));
    private static final PerlinSimplexNoise UNEVEN =
            new PerlinSimplexNoise(RandomSource.create(31417L), List.of(0));

    /** Height of the Shire's surface here, in blocks. */
    public static double height(int x, int z) {
        double swells = SWELLS.getValue(x / 260.0, z / 260.0, false) * 10.0;
        double rolls = ROLLS.getValue(x / 85.0, z / 85.0, false) * 4.0;
        double uneven = UNEVEN.getValue(x / 24.0, z / 24.0, false) * 0.8;
        double height = BASE + swells + rolls + uneven;
        // Ease the hollows in above the floor rather than flattening them on it
        return height < FLOOR ? FLOOR - (FLOOR - height) * 0.3 : height;
    }
}
