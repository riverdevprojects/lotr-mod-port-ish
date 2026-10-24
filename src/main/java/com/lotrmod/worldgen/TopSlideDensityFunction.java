package com.lotrmod.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * {@code lotrmod:top_slide} — vanilla's top slide, lifted off the Lonely
 * Mountain:
 *
 * <pre>{"type": "lotrmod:top_slide", "argument": {"type": "minecraft:y_clamped_gradient", "from_y": 240, ...}}</pre>
 *
 * <p>Vanilla fades all terrain out between y 240 and 256, so nothing rises
 * much above 250. That stays true everywhere but over the Lonely Mountain,
 * whose summit stands near y 300: there the slide's gradient is held at 1
 * ({@link DaleTerrain#summitShare}), so the Mountain keeps the shape
 * {@link DaleTerrain} gives it all the way up.
 */
public record TopSlideDensityFunction(DensityFunction argument) implements DensityFunction {
    public static final MapCodec<TopSlideDensityFunction> DATA_CODEC =
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument")
                    .xmap(TopSlideDensityFunction::new, TopSlideDensityFunction::argument);
    public static final KeyDispatchDataCodec<TopSlideDensityFunction> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    @Override
    public double compute(FunctionContext context) {
        double slide = argument.compute(context);
        if (slide >= 1.0) return slide;
        int x = context.blockX(), z = context.blockZ();
        double lift = DaleTerrain.summitShare(x, z);
        if (lift <= 0.0) return slide;
        return Math.max(slide, lift * MiddleEarthClimate.daleWeight(x, z));
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new TopSlideDensityFunction(argument.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return argument.minValue();
    }

    @Override
    public double maxValue() {
        return Math.max(argument.maxValue(), 1.0);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
