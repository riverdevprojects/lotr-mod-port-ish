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

/**
 * Fangorn's thick fog. The original mod marked Fangorn foggy, and drew its fog
 * as vanilla draws the Nether's, from a twentieth of the view distance out to
 * half of it (at most 96 blocks); the biome's fog colour then fills the
 * forest with a green haze. Biome JSON has no such setting, so it is done
 * here, fading in and out over two seconds as the camera enters and leaves.
 */
@EventBusSubscriber(modid = LOTRMod.MODID, value = Dist.CLIENT)
public final class FangornFog {
    private FangornFog() {}

    private static final ResourceKey<Biome> FANGORN = ModBiomes.getKey(Region.FANGORN_FOREST);
    private static final float FADE_PER_TICK = 1.0F / 40.0F;

    private static float fogginess;
    private static float prevFogginess;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        prevFogginess = fogginess;
        if (minecraft.level == null) {
            fogginess = prevFogginess = 0.0F;
            return;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        boolean inFangorn = minecraft.level.getBiome(camera.getBlockPosition()).is(FANGORN);
        fogginess = Mth.clamp(fogginess + (inFangorn ? FADE_PER_TICK : -FADE_PER_TICK), 0.0F, 1.0F);
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        // Water, lava and powder snow keep their own fog
        if (event.getType() != FogType.NONE) return;
        float f = Mth.lerp((float) event.getPartialTick(), prevFogginess, fogginess);
        if (f <= 0.0F) return;

        float far = event.getFarPlaneDistance();
        float near = event.getNearPlaneDistance();
        float foggyFar = Math.min(far, 192.0F) * 0.5F;
        float foggyNear = far * 0.05F;
        // Never thinner than a fog already there, such as blindness's
        event.setFarPlaneDistance(Math.min(far, Mth.lerp(f, far, foggyFar)));
        event.setNearPlaneDistance(Math.min(near, Mth.lerp(f, near, foggyNear)));
        event.setCanceled(true);
    }
}
