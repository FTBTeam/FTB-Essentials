package dev.ftb.mods.ftbessentials.neoforge;

import net.minecraft.server.level.ServerPlayer;

@SuppressWarnings("unused")
public class FTBEssentialsPlatformImpl {
    public static void curePotionEffects(ServerPlayer player) {
        player.removeAllEffects();
        // TODO EffectCure is gone from NeoForge for now with no replacement
        // https://github.com/neoforged/NeoForge/pull/1603
//        player.removeEffectsCuredBy(EffectCures.MILK);
    }
}
