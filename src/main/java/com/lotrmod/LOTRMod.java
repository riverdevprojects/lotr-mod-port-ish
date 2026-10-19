package com.lotrmod;

import com.lotrmod.command.MiddleEarthCommand;
import com.lotrmod.worldgen.LOTRWorldGen;
import com.lotrmod.worldgen.LandmaskLoader;
import com.lotrmod.worldgen.MiddleEarthClimate;
import com.lotrmod.worldgen.RegionMapLoader;
import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

/**
 * Middle-earth landscape only: a custom dimension whose landmass comes from a
 * landmask PNG and whose biomes come from a region map PNG, one biome per region.
 *
 * <p>Nothing else is registered — no blocks, items, entities or structures. To
 * visit the dimension, use {@code /middleearth}.
 */
@Mod(LOTRMod.MODID)
public class LOTRMod {
    public static final String MODID = "lotrmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LOTRMod(IEventBus modEventBus, ModContainer modContainer) {
        // The chunk generator, biome source and climate density function codecs
        // are the only registry entries this mod contributes.
        LOTRWorldGen.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("Middle-earth landscape generator initializing...");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        loadMaps(event.getServer().getResourceManager());
    }

    private static void loadMaps(ResourceManager resourceManager) {
        LandmaskLoader.loadLandmask(resourceManager);
        RegionMapLoader.loadRegionMap(resourceManager);
        MiddleEarthClimate.rebuild();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        MiddleEarthCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        // Reload the maps when resources are reloaded, so editing either PNG
        // does not need a game restart.
        event.addListener((preparationBarrier, resourceManager, profilerFiller, profilerFiller2, executor, executor2) ->
                preparationBarrier.wait(null).thenRunAsync(() -> loadMaps(resourceManager), executor2));
    }
}
