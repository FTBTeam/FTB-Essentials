package dev.ftb.mods.ftbessentials;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerPlayer;

public class FTBEssentialsPlatform {
    @ExpectPlatform
    public static void curePotionEffects(ServerPlayer player) {
        throw new AssertionError();
    }

}
