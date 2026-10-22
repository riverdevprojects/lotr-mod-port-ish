package com.lotrmod.worldgen.feature;

import com.lotrmod.worldgen.LOTRWorldGen;
import com.lotrmod.worldgen.RohanTerrain;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * {@code lotrmod:rohan_moisture} — passes only where {@link RohanTerrain#moisture}
 * lies between {@code min} and {@code max}, so Rohan's scrub keeps to its
 * swales and hollows and its driest grasses to the tops of the swells.
 *
 * <pre>{"type": "lotrmod:rohan_moisture", "min": 0.4, "max": 1.0}</pre>
 */
public class RohanMoistureFilter extends PlacementFilter {
    public static final MapCodec<RohanMoistureFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("min", -1.0).forGetter(f -> f.min),
            Codec.DOUBLE.optionalFieldOf("max", 1.0).forGetter(f -> f.max)
    ).apply(instance, RohanMoistureFilter::new));

    private final double min;
    private final double max;

    public RohanMoistureFilter(double min, double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        double moisture = RohanTerrain.moisture(pos.getX(), pos.getZ());
        return moisture >= min && moisture <= max;
    }

    @Override
    public PlacementModifierType<?> type() {
        return LOTRWorldGen.ROHAN_MOISTURE.get();
    }
}
