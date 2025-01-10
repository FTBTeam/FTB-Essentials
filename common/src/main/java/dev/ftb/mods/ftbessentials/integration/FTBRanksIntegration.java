package dev.ftb.mods.ftbessentials.integration;

import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import net.minecraft.server.level.ServerPlayer;

public class FTBRanksIntegration implements PermissionsProvider {
	public int getInt(ServerPlayer player, int def, String node) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, node).asInteger().orElse(def), 0);
	}

	public boolean getBool(ServerPlayer player, boolean def, String node) {
		return FTBRanksAPI.getPermissionValue(player, node).asBoolean().orElse(def);
	}
}
