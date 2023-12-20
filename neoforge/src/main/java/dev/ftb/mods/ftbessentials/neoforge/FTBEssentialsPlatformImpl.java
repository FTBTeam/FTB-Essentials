package dev.ftb.mods.ftbessentials.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.EffectCures;

@SuppressWarnings("unused")
public class FTBEssentialsPlatformImpl {
    public static void curePotionEffects(ServerPlayer player) {
        player.removeEffectsCuredBy(EffectCures.MILK);
    }
}
