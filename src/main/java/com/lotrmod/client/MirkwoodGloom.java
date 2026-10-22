package com.lotrmod.client;

import com.lotrmod.LOTRMod;
import com.lotrmod.worldgen.Region;
import com.lotrmod.worldgen.biome.ModBiomes;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Vector3f;

/**
 * Mirkwood's gloom, fading in and out over three seconds as the camera enters
 * and leaves the forest. Everything is lit more dimly ({@link #dimLightmap},
 * called from {@link MiddleEarthEffects}), torchlight least of all, so a lit
 * camp still stands out; a thick fog closes in, nearer than Fangorn's; and the
 * fog darkens to match.
 */
@EventBusSubscriber(modid = LOTRMod.MODID, value = Dist.CLIENT)
public final class MirkwoodGloom {
    private MirkwoodGloom() {}

    private static final ResourceKey<Biome> MIRKWOOD = ModBiomes.getKey(Region.MIRKWOOD);
    private static final float FADE_PER_TICK = 1.0F / 60.0F;
    /** How much of the light is taken away where there is no torchlight. */
    private static final float MAX_DIMMING = 0.6F;
    /** How much darker the fog is drawn. */
    private static final float FOG_DARKENING = 0.45F;

    private static float gloom;
    private static float prevGloom;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        prevGloom = gloom;
        if (minecraft.level == null) {
            gloom = prevGloom = 0.0F;
            return;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        boolean inMirkwood = minecraft.level.getBiome(camera.getBlockPosition()).is(MIRKWOOD);
        gloom = Mth.clamp(gloom + (inMirkwood ? FADE_PER_TICK : -FADE_PER_TICK), 0.0F, 1.0F);
    }

    private static float gloom(float partialTick) {
        return Mth.lerp(partialTick, prevGloom, gloom);
    }

    /**
     * Darkens one entry of the lightmap, {@code blockLight} being its torchlight
     * level from 0 to 15. Sky and moonlight are dimmed the most, torchlight not
     * at all at full strength.
     */
    static void dimLightmap(float partialTick, int blockLight, Vector3f colors) {
        float g = gloom(partialTick);
        if (g <= 0.0F) return;
        float torchlight = blockLight / 15.0F;
        colors.mul(1.0F - g * MAX_DIMMING * (1.0F - torchlight * torchlight));
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        // Water, lava and powder snow keep their own fog
        if (event.getType() != FogType.NONE) return;
        float g = gloom((float) event.getPartialTick());
        if (g <= 0.0F) return;

        float far = event.getFarPlaneDistance();
        float near = event.getNearPlaneDistance();
        float gloomyFar = Math.min(far, 192.0F) * 0.4F;
        // Never thinner than a fog already there, such as Fangorn's or blindness's
        event.setFarPlaneDistance(Math.min(far, Mth.lerp(g, far, gloomyFar)));
        event.setNearPlaneDistance(Math.min(near, Mth.lerp(g, near, 0.0F)));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        float g = gloom((float) event.getPartialTick());
        if (g <= 0.0F) return;
        float f = 1.0F - g * FOG_DARKENING;
        event.setRed(event.getRed() * f);
        event.setGreen(event.getGreen() * f);
        event.setBlue(event.getBlue() * f);
    }
}
