package dev.ftb.mods.ftbessentials.integration;

import net.minecraft.server.level.ServerPlayer;

public interface PermissionsProvider {
    default int getInt(ServerPlayer player, int def, String node) {
        return def;
    }
    
    default boolean getBool(ServerPlayer player, boolean def, String node) {
        return def;
    }
}
