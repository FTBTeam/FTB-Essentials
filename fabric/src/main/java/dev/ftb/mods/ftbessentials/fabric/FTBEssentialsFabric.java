package dev.ftb.mods.ftbessentials.fabric;

import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftblibrary.fabric.PlayerDisplayNameCallback;
import net.fabricmc.api.ModInitializer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static dev.ftb.mods.ftbessentials.FTBEssentials.RECORDING_STYLE;
import static dev.ftb.mods.ftbessentials.FTBEssentials.STREAMING_STYLE;

public class FTBEssentialsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FTBEssentials.init();

        PlayerDisplayNameCallback.EVENT.register(PlayerDisplayNameCallback.EARLY, (player, oldDisplayName) -> {
            if (player instanceof ServerPlayer sp) {
                FTBEPlayerData data = FTBEPlayerData.get(sp);
                if (data != null && !data.nick.isEmpty()) {
                    return Component.literal(data.nick);
                }
            }
            return oldDisplayName;
        });

        PlayerDisplayNameCallback.EVENT.register(PlayerDisplayNameCallback.LATE, (player, oldDisplayName) -> {
            if (player instanceof ServerPlayer sp) {
                FTBEPlayerData data = FTBEPlayerData.get(sp);
                if (data != null && data.recording > 0) {
                    return Component.literal("\u23FA ").withStyle(data.recording == 1 ? RECORDING_STYLE : STREAMING_STYLE).append(oldDisplayName);
                }
            }
            return oldDisplayName;
        });
    }
}
