package dev.ftb.mods.ftbessentials.fabric;

import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.api.event.TeleportEvent;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftblibrary.fabric.PlayerDisplayNameCallback;
import dev.ftb.mods.ftblibrary.platform.event.NativeEventPosting;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class FTBEssentialsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        var essentials = new FTBEssentials();

        registerNativeEventPosting();

        ServerLifecycleEvents.SERVER_STARTING.register(essentials.eventHandler::serverAboutToStart);
        ServerLifecycleEvents.SERVER_STOPPED.register(essentials.eventHandler::serverStopped);
        ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> essentials.eventHandler.serverSave(server));

        ServerTickEvents.END_SERVER_TICK.register(essentials.eventHandler::serverTickPost);
        CommandRegistrationCallback.EVENT.register(essentials.eventHandler::registerCommands);

        ServerPlayerEvents.JOIN.register(essentials.eventHandler::playerLoggedIn);
        ServerPlayerEvents.LEAVE.register(essentials.eventHandler::playerLoggedOut);
        // TODO: Not sure about this one
        ServerPlayerEvents.COPY_FROM.register(essentials.eventHandler::onPlayerDeath);
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, ignoredOrigin, ignoredNewLevel) -> essentials.eventHandler.playerChangedDimension(player));

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, ignored, ignored1, amount, blocked) ->
                essentials.eventHandler.onPlayerHurt(entity, amount, blocked)
        );

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(((ignored1, serverPlayer, ignored2) -> {
            var result = essentials.eventHandler.allowChat(serverPlayer);
            return switch (result) {
                case SUCCESS, PASS -> true;
                case FAIL -> false;
            };
        }));

        // Nickname handling
        PlayerDisplayNameCallback.EVENT.register(PlayerDisplayNameCallback.EARLY, (player, oldDisplayName) -> {
            if (player instanceof ServerPlayer sp) {
                FTBEPlayerData data = FTBEPlayerData.getOrCreate(sp).orElse(null);
                if (data != null && !data.getNick().isEmpty()) {
                    return Component.literal(data.getNick());
                }
            }
            return oldDisplayName;
        });

        // Recording status handling
        PlayerDisplayNameCallback.EVENT.register(PlayerDisplayNameCallback.LATE, (player, oldDisplayName) -> {
            if (player instanceof ServerPlayer sp) {
                FTBEPlayerData data = FTBEPlayerData.getOrCreate(sp).orElse(null);
                if (data != null && data.getRecording() != FTBEPlayerData.RecordingStatus.NONE) {
                    return Component.literal("⏺ ").withStyle(data.getRecording().getStyle()).append(oldDisplayName);
                }
            }
            return oldDisplayName;
        });
    }

    private static void registerNativeEventPosting() {
        NativeEventPosting.INSTANCE.registerEventWithResult(TeleportEvent.TYPE,
                data -> FTBEssentialsEvents.TELEPORT.invoker().teleport(data));
    }
}
