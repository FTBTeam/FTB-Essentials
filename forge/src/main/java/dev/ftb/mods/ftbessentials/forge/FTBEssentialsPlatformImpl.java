package dev.ftb.mods.ftbessentials.forge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@SuppressWarnings("unused")
public class FTBEssentialsPlatformImpl {
    public static void curePotionEffects(ServerPlayer player) {
        player.curePotionEffects(new ItemStack(Items.MILK_BUCKET));
    }
}
