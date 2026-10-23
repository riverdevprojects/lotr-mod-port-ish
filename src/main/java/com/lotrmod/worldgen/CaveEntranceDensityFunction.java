package com.lotrmod.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * {@code lotrmod:cave_entrances} — vanilla's cave entrance noise, raised
 * inside Rohan and Gondor so that far fewer of its tunnels break out through
 * their plains:
 *
 * <pre>{"type": "lotrmod:cave_entrances", "argument": "minecraft:overworld/caves/entrances"}</pre>
 *
 * <p>Near the surface vanilla's terrain is the lesser of the ground and five
 * times this noise, so wherever it dips below zero it opens a gash in the
 * ground. Lifting it by {@link #LIFT}, faded in by
 * {@link MiddleEarthClimate#rohanWeight} and
 * {@link MiddleEarthClimate#gondorWeight}, leaves only its deepest dips, and
 * the grassland whole. Caves further down are untouched.
 */
public record CaveEntranceDensityFunction(DensityFunction argument) implements DensityFunction {
    public static final MapCodec<CaveEntranceDensityFunction> DATA_CODEC =
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument")
                    .xmap(CaveEntranceDensityFunction::new, CaveEntranceDensityFunction::argument);
    public static final KeyDispatchDataCodec<CaveEntranceDensityFunction> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    /** How far the entrance noise is lifted inside Rohan and Gondor. */
    private static final double LIFT = 0.3;

    /** The column last looked up on this thread; density is asked for a column at a time. */
    private static final ThreadLocal<Column> LAST = ThreadLocal.withInitial(Column::new);

    @Override
    public double compute(FunctionContext context) {
        double density = argument.compute(context);
        double weight = LAST.get().at(context.blockX(), context.blockZ()).weight;
        return density + weight * LIFT;
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new CaveEntranceDensityFunction(argument.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return argument.minValue();
    }

    @Override
    public double maxValue() {
        return argument.maxValue() + LIFT;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    private static final class Column {
        private int x;
        private int z;
        private boolean valid;
        private double weight;

        Column at(int x, int z) {
            if (!valid || x != this.x || z != this.z) {
                this.x = x;
                this.z = z;
                this.valid = true;
                weight = Math.max(MiddleEarthClimate.rohanWeight(x, z), MiddleEarthClimate.gondorWeight(x, z));
            }
            return this;
        }
    }
}
