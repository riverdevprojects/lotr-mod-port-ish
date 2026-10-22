package com.lotrmod.client;

import com.lotrmod.LOTRMod;
import com.lotrmod.worldgen.Region;
import com.lotrmod.worldgen.RohanTerrain;
import com.lotrmod.worldgen.biome.ModBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import java.util.List;

/**
 * Rohan's grass colour. A biome's grass colour is one flat tint, and a whole
 * kingdom of grassland that one colour looks painted on, so in Rohan the grass
 * is tinted from the ground instead: straw and tawny gold over the dry swells,
 * in drifts a hundred blocks or so across, turning a dusty sage green down in
 * the swales and hollows, where {@link RohanTerrain#moisture} is high. Grass
 * blocks, short and tall grass and ferns all take it; everywhere else they keep
 * the biome's colour, as vanilla's do.
 */
@EventBusSubscriber(modid = LOTRMod.MODID, value = Dist.CLIENT)
public final class RohanGrassColors {
    private RohanGrassColors() {}

    private static final ResourceKey<Biome> ROHAN = ModBiomes.getKey(Region.ROHAN);

    private static final int STRAW = 0xD1B464;
    private static final int TAWNY = 0xC09A55;
    private static final int SAGE = 0x9AA35A;
    private static final int GREEN = 0x7E9A4C;

    /** Drifts of straw and tawny across the dry ground. */
    private static final PerlinSimplexNoise DRIFTS =
            new PerlinSimplexNoise(RandomSource.create(48130L), List.of(0, 1));

    /** The biome's grass colour, but Rohan's from the ground. */
    public static final ColorResolver GRASS = (biome, x, z) ->
            isRohan(biome) ? rohanGrass(Mth.floor(x), Mth.floor(z)) : biome.getGrassColor(x, z);

    /** The last biome asked about, and whether it was Rohan. Chunks are meshed on several threads. */
    private record Last(Biome biome, boolean rohan) {}
    private static volatile Last last = new Last(null, false);

    @SubscribeEvent
    public static void onRegisterColorResolvers(RegisterColorHandlersEvent.ColorResolvers event) {
        event.register(GRASS);
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        // As vanilla's handlers for these blocks, with GRASS in place of its resolver
        BlockColor grass = (state, level, pos, tintIndex) -> {
            if (level == null || pos == null) return GrassColor.getDefaultColor();
            BlockPos at = state.hasProperty(DoublePlantBlock.HALF)
                    && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
            return level.getBlockTint(at, GRASS);
        };
        event.register(grass, Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.TALL_GRASS,
                Blocks.FERN, Blocks.LARGE_FERN, Blocks.POTTED_FERN);
    }

    /** Rohan's grass colour at a spot. */
    public static int rohanGrass(int x, int z) {
        double drift = DRIFTS.getValue(x / 110.0, z / 110.0, false);
        int dry = lerp(STRAW, TAWNY, Mth.clamp(0.5 + drift * 0.8, 0.0, 1.0));
        double moisture = RohanTerrain.moisture(x, z);
        int damp = lerp(SAGE, GREEN, Mth.clamp((moisture - 0.5) * 2.0, 0.0, 1.0));
        return lerp(dry, damp, smoothstep(Mth.clamp((moisture + 0.1) / 0.7, 0.0, 1.0)));
    }

    private static boolean isRohan(Biome biome) {
        // The resolver is asked for one biome many times over, so remember the last
        Last seen = last;
        if (seen.biome() == biome) return seen.rohan();
        Minecraft minecraft = Minecraft.getInstance();
        boolean rohan = minecraft.level != null && minecraft.level.registryAccess()
                .registryOrThrow(Registries.BIOME).getResourceKey(biome)
                .filter(ROHAN::equals).isPresent();
        last = new Last(biome, rohan);
        return rohan;
    }

    private static double smoothstep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static int lerp(int from, int to, double t) {
        return FastColor.ARGB32.lerp((float) t, from, to) & 0xFFFFFF;
    }
}
