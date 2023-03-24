package dev.ftb.mods.ftbessentials.fabric;

import net.minecraft.server.level.ServerPlayer;

@SuppressWarnings("unused")
public class FTBEssentialsPlatformImpl {
    public static void curePotionEffects(ServerPlayer player) {
        player.removeAllEffects();
    }

}
