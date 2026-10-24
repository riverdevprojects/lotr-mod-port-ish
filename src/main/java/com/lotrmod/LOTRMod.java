package com.lotrmod;

import com.lotrmod.block.ModBlockEntities;
import com.lotrmod.block.ModBlocks;
import com.lotrmod.block.ModMenus;
import com.lotrmod.command.DaleCommand;
import com.lotrmod.command.MiddleEarthCommand;
import com.lotrmod.entity.ModEntities;
import com.lotrmod.worldgen.LOTRWorldGen;
import com.lotrmod.worldgen.LandmaskLoader;
import com.lotrmod.worldgen.MiddleEarthClimate;
import com.lotrmod.worldgen.RegionMapLoader;
import com.lotrmod.worldgen.RiverMap;
import com.lotrmod.worldgen.ShireZones;
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
 * landmask PNG, whose biomes come from a region map PNG, one biome per region, and
 * whose rivers come from a river map PNG.
 *
 * <p>Beyond the landscape, the one structure is the Shire's hobbit holes,
 * ported from the original mod, with the blocks they are built from and the
 * hobbits who live in them. Otherwise the only blocks are the plants the
 * landscape needs that vanilla lacks. To visit the dimension, use
 * {@code /middleearth}.
 */
@Mod(LOTRMod.MODID)
public class LOTRMod {
    public static final String MODID = "lotrmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LOTRMod(IEventBus modEventBus, ModContainer modContainer) {
        // The chunk generator, biome source and density function codecs, the
        // trees and the hobbit holes
        LOTRWorldGen.register(modEventBus);
        // The plants, and what hobbit holes are built from and lived in by
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        ModEntities.register(modEventBus);

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
        RiverMap.load(resourceManager);
        MiddleEarthClimate.rebuild();
        ShireZones.rebuild();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        MiddleEarthCommand.register(event.getDispatcher());
        DaleCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        // Reload the maps when resources are reloaded, so editing either PNG
        // does not need a game restart.
        event.addListener((preparationBarrier, resourceManager, profilerFiller, profilerFiller2, executor, executor2) ->
                preparationBarrier.wait(null).thenRunAsync(() -> loadMaps(resourceManager), executor2));
    }
}
