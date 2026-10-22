package com.lotrmod.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * {@code lotrmod:terrain_blend} — vanilla's terrain density, reshaped where
 * the maps call for something vanilla doesn't make:
 *
 * <pre>{"type": "lotrmod:terrain_blend", "argument": <vanilla density>}</pre>
 *
 * <ul>
 *   <li><b>Mountain ranges</b> fade over to {@link MountainTerrain}'s shape,
 *       by {@link MiddleEarthClimate#mountainWeight}.</li>
 *   <li><b>Marshes</b> fade over to {@link MarshTerrain}'s shape, by
 *       {@link MiddleEarthClimate#marshWeight}.</li>
 *   <li><b>The Shire</b> fades over to {@link ShireTerrain}'s rolling downs, by
 *       {@link MiddleEarthClimate#shireWeight}.</li>
 *   <li><b>Rohan</b> fades over to {@link RohanTerrain}'s rolling grassland,
 *       by {@link MiddleEarthClimate#rohanWeight}.</li>
 *   <li><b>Rivers</b> from {@link RiverMap} cut their channel and valley
 *       ({@link RiverTerrain}). They only ever lower the ground.</li>
 * </ul>
 *
 * <p>It wraps the terrain density before vanilla carves caves out of it, so the
 * reshaped ground still gets vanilla's caves, aquifers and ore veins. It also
 * wraps the density vanilla finds the surface from, so surface rules and
 * aquifers see the ground where it really is.
 */
public record TerrainBlendDensityFunction(DensityFunction argument) implements DensityFunction {
    public static final MapCodec<TerrainBlendDensityFunction> DATA_CODEC =
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument")
                    .xmap(TerrainBlendDensityFunction::new, TerrainBlendDensityFunction::argument);
    public static final KeyDispatchDataCodec<TerrainBlendDensityFunction> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    /**
     * Blocks per unit of density around a reshaped surface: solid below, air
     * above, and roughly as steep a change as vanilla's own terrain.
     */
    private static final double BLOCKS_PER_UNIT = 12.0;
    /** Lowest and highest a reshaped density can be, from y -64 to 320. */
    private static final double MIN = (40 - 320) / BLOCKS_PER_UNIT;
    private static final double MAX = (320 + 64) / BLOCKS_PER_UNIT;

    /** The column last looked up on this thread; density is asked for a column at a time. */
    private static final ThreadLocal<Column> LAST = ThreadLocal.withInitial(Column::new);

    @Override
    public double compute(FunctionContext context) {
        double density = argument.compute(context);
        Column column = LAST.get().at(context.blockX(), context.blockZ());
        int y = context.blockY();
        if (column.mountainWeight > 0.0) {
            double mountain = (column.mountainHeight - y) / BLOCKS_PER_UNIT;
            density += (mountain - density) * column.mountainWeight;
        }
        if (column.marshWeight > 0.0) {
            double marsh = (column.marshHeight - y) / BLOCKS_PER_UNIT;
            density += (marsh - density) * column.marshWeight;
        }
        if (column.shireWeight > 0.0) {
            double shire = (column.shireHeight - y) / BLOCKS_PER_UNIT;
            density += (shire - density) * column.shireWeight;
        }
        if (column.rohanWeight > 0.0) {
            double rohan = (column.rohanHeight - y) / BLOCKS_PER_UNIT;
            density += (rohan - density) * column.rohanWeight;
        }
        if (column.riverWeight > 0.0) {
            double river = (column.riverHeight - y) / BLOCKS_PER_UNIT;
            density = Math.min(density, density + (river - density) * column.riverWeight);
        }
        return density;
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new TerrainBlendDensityFunction(argument.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return Math.min(argument.minValue(), MIN);
    }

    @Override
    public double maxValue() {
        return Math.max(argument.maxValue(), MAX);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    private static final class Column {
        private int x;
        private int z;
        private boolean valid;
        private double mountainWeight;
        private double mountainHeight;
        private double marshWeight;
        private double marshHeight;
        private double shireWeight;
        private double shireHeight;
        private double rohanWeight;
        private double rohanHeight;
        private double riverWeight;
        private double riverHeight;

        Column at(int x, int z) {
            if (!valid || x != this.x || z != this.z) {
                this.x = x;
                this.z = z;
                this.valid = true;
                mountainWeight = MiddleEarthClimate.mountainWeight(x, z);
                mountainHeight = mountainWeight > 0.0 ? MountainTerrain.height(x, z) : 0.0;
                marshWeight = MiddleEarthClimate.marshWeight(x, z);
                marshHeight = marshWeight > 0.0 ? MarshTerrain.height(x, z) : 0.0;
                shireWeight = MiddleEarthClimate.shireWeight(x, z);
                shireHeight = shireWeight > 0.0 ? ShireTerrain.height(x, z) : 0.0;
                rohanWeight = MiddleEarthClimate.rohanWeight(x, z);
                rohanHeight = rohanWeight > 0.0 ? RohanTerrain.height(x, z) : 0.0;
                RiverMap.Sample river = RiverMap.sample(x, z);
                riverWeight = river != null ? RiverTerrain.weight(river) : 0.0;
                riverHeight = river != null ? RiverTerrain.height(river) : 0.0;
            }
            return this;
        }
    }
}
