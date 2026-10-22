package com.lotrmod.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import org.joml.Vector3f;

/**
 * {@code lotrmod:middleearth}, the dimension's sky and lighting: the
 * overworld's in every way, except that the lightmap is darkened in Mirkwood
 * (see {@link MirkwoodGloom}). Biome JSON can tint the sky and fog but can't
 * dim the light, and the lightmap is only reachable from here.
 */
public class MiddleEarthEffects extends DimensionSpecialEffects.OverworldEffects {
    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker,
            float skyLight, int pixelX, int pixelY, Vector3f colors) {
        // pixelX is the block light level, pixelY the sky light level
        MirkwoodGloom.dimLightmap(partialTicks, pixelX, colors);
    }
}
