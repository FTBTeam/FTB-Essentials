package dev.ftb.mods.ftbessentials.fabric;

import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftblibrary.fabric.PlayerDisplayNameCallback;
import net.fabricmc.api.ModInitializer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class FTBEssentialsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FTBEssentials.init();

        PlayerDisplayNameCallback.EVENT.register(PlayerDisplayNameCallback.EARLY, (player, oldDisplayName) -> {
            if (player instanceof ServerPlayer sp) {
                FTBEPlayerData data = FTBEPlayerData.getOrCreate(sp).orElse(null);
                if (data != null && !data.getNick().isEmpty()) {
                    return Component.literal(data.getNick());
                }
            }
            return oldDisplayName;
        });

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
}
