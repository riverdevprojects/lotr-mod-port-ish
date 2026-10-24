package com.lotrmod.command;

import com.lotrmod.worldgen.dale.DaleTransformer;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * {@code /dale} says which age Dale is in; {@code /dale transform} turns it to
 * the other, from the green Dale of King Girion to the Desolation of Smaug or
 * back, regenerating the parts of Dale already generated
 * ({@link DaleTransformer}).
 *
 * <p>Needs permission level 2, the same as {@code /middleearth}.
 */
public final class DaleCommand {
    private DaleCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dale")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> DaleTransformer.status(ctx.getSource().getServer()), false);
                    return 1;
                })
                .then(Commands.literal("transform").executes(ctx -> {
                    if (!DaleTransformer.transform(ctx.getSource())) {
                        ctx.getSource().sendFailure(Component.literal("Dale is already being transformed"));
                        return 0;
                    }
                    return 1;
                })));
    }
}
