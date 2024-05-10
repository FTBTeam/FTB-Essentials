package dev.ftb.mods.ftbessentials.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.EffectCures;

@SuppressWarnings("unused")
public class FTBEssentialsPlatformImpl {
    public static void curePotionEffects(ServerPlayer player) {
        player.removeEffectsCuredBy(EffectCures.MILK);
    }
}
