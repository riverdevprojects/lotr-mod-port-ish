package com.lotrmod.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * {@code lotrmod:climate} — a density function that reads one of
 * {@link MiddleEarthClimate}'s map-driven values, so the noise settings JSON can
 * build Middle-earth's continentalness and erosion out of the maps plus
 * vanilla's own noise:
 *
 * <pre>{"type": "lotrmod:climate", "parameter": "erosion"}</pre>
 *
 * <p>The value depends on x and z only. Wrap it in {@code minecraft:flat_cache},
 * as vanilla does its climate noises, so it is worked out once per column.
 */
public record ClimateDensityFunction(Parameter parameter) implements DensityFunction.SimpleFunction {
    public static final MapCodec<ClimateDensityFunction> DATA_CODEC =
            Parameter.CODEC.fieldOf("parameter").xmap(ClimateDensityFunction::new, ClimateDensityFunction::parameter);
    public static final KeyDispatchDataCodec<ClimateDensityFunction> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX();
        int z = context.blockZ();
        return switch (parameter) {
            case CONTINENTALNESS -> MiddleEarthClimate.continentalness(x, z);
            case INLAND -> MiddleEarthClimate.inland(x, z);
            case EROSION -> MiddleEarthClimate.erosion(x, z);
            case EROSION_SPREAD -> MiddleEarthClimate.erosionSpread(x, z);
        };
    }

    @Override
    public double minValue() {
        return parameter.min;
    }

    @Override
    public double maxValue() {
        return parameter.max;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    public enum Parameter implements StringRepresentable {
        /** Continentalness from the coast and the regions, without noise. */
        CONTINENTALNESS("continentalness", -1.0, 1.0),
        /** 0 at the coast and at sea, rising to 1 inland. */
        INLAND("inland", 0.0, 1.0),
        /** The regions' erosion centre. */
        EROSION("erosion", -1.0, 1.0),
        /** How far erosion wanders either side of its centre. */
        EROSION_SPREAD("erosion_spread", 0.0, 1.0);

        public static final Codec<Parameter> CODEC = StringRepresentable.fromEnum(Parameter::values);

        private final String name;
        private final double min;
        private final double max;

        Parameter(String name, double min, double max) {
            this.name = name;
            this.min = min;
            this.max = max;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
