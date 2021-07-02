package dev.ftb.mods.ftbessentials;

import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import net.minecraft.server.level.ServerPlayer;

public class FTBRanksIntegration {
	public static int getInt(ServerPlayer player, int def, String node) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, node).asInteger().orElse(def), 0);
	}
}
