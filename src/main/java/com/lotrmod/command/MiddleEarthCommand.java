package com.lotrmod.command;

import com.lotrmod.LOTRMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * {@code /middleearth} — sends the player to the Middle-earth dimension at
 * their current x/z, standing on the surface (or the water, over the sea).
 *
 * <p>Needs permission level 2, the same as {@code /tp}.
 */
public final class MiddleEarthCommand {
    public static final ResourceKey<Level> MIDDLE_EARTH = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(LOTRMod.MODID, "middleearth"));

    private static final SimpleCommandExceptionType ALREADY_THERE =
            new SimpleCommandExceptionType(Component.literal("You are already in Middle-earth"));
    private static final SimpleCommandExceptionType MISSING =
            new SimpleCommandExceptionType(Component.literal("The Middle-earth dimension is not loaded"));

    private MiddleEarthCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("middleearth")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> teleport(ctx.getSource())));
    }

    private static int teleport(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (player.level().dimension() == MIDDLE_EARTH) throw ALREADY_THERE.create();

        ServerLevel middleEarth = source.getServer().getLevel(MIDDLE_EARTH);
        if (middleEarth == null) throw MISSING.create();

        int x = player.getBlockX();
        int z = player.getBlockZ();
        // Level.getHeight reports the bottom of the world for a chunk that isn't
        // loaded, so load (and if need be generate) the target chunk first.
        middleEarth.getChunk(x >> 4, z >> 4);
        int y = middleEarth.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

        player.teleportTo(middleEarth, x + 0.5, y, z + 0.5, player.getYRot(), player.getXRot());
        source.sendSuccess(() -> Component.literal("Welcome to Middle-earth"), false);
        return 1;
    }
}
