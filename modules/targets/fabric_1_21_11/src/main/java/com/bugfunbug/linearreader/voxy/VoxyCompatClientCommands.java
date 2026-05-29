package com.bugfunbug.linearreader.voxy;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public final class VoxyCompatClientCommands {

    private VoxyCompatClientCommands() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                register(dispatcher));
    }

    static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("linearreader")
                .then(ClientCommandManager.literal("voxy-compat")
                        .then(ClientCommandManager.literal("auto")
                                .executes(ctx -> executeAuto(ctx.getSource())))));
    }

    private static int executeAuto(FabricClientCommandSource source) {
        VoxyCompatAutoImporter.StartResult result = VoxyCompatAutoImporter.start();
        switch (result) {
            case STARTED -> {
                source.sendFeedback(Component.literal(
                        "[LinearReader] Voxy auto import started. Progress will be posted in chat."));
                return 1;
            }
            case ALREADY_RUNNING -> {
                source.sendFeedback(Component.literal(VoxyCompatAutoImporter.status()));
                return 1;
            }
            case VOXY_NOT_LOADED -> source.sendError(Component.literal(
                    "[LinearReader] Voxy is not loaded; auto import is unavailable."));
            case NO_SINGLEPLAYER_SERVER -> source.sendError(Component.literal(
                    "[LinearReader] Voxy auto import requires singleplayer, matching /voxy import current."));
            case NO_CLIENT_WORLD -> source.sendError(Component.literal(
                    "[LinearReader] No client world is loaded."));
        }
        return 0;
    }
}
