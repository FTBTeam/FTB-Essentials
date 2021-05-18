package dev.ftb.mods.ftbessentials;

import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import net.minecraft.server.level.ServerPlayer;

public class FTBRanksIntegration {
	public static int getMaxBack(ServerPlayer player, int def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbessentials.back.max").asInteger().orElse(def), 0);
	}

	public static long getMaxHomes(ServerPlayer player, long def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbessentials.home.max").asLong().orElse(def), 0L);
	}

	public static long getBackCooldown(ServerPlayer player, long def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbessentials.back.cooldown").asLong().orElse(def), 0L);
	}

	public static long getSpawnCooldown(ServerPlayer player, long def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbessentials.spawn.cooldown").asLong().orElse(def), 0L);
	}

	public static long getWarpCooldown(ServerPlayer player, long def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbessentials.warp.cooldown").asLong().orElse(def), 0L);
	}

	public static long getHomeCooldown(ServerPlayer player, long def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbessentials.home.cooldown").asLong().orElse(def), 0L);
	}

	public static long getTpaCooldown(ServerPlayer player, long def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbessentials.tpa.cooldown").asLong().orElse(def), 0L);
	}

	public static long getRtpCooldown(ServerPlayer player, long def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbessentials.rtp.cooldown").asLong().orElse(def), 0L);
	}
}
