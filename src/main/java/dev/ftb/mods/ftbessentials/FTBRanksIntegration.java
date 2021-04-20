package dev.ftb.mods.ftbessentials;

import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import net.minecraft.entity.player.ServerPlayerEntity;

public class FTBRanksIntegration {
	public static int getMaxBack(ServerPlayerEntity player, int def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbessentials.back.max").asInteger().orElse(def), 0);
	}

	public static long getMaxHomes(ServerPlayerEntity player, long def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbessentials.home.max").asLong().orElse(def), 0L);
	}

	public static long getBackCooldown(ServerPlayerEntity player, long def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbessentials.back.cooldown").asLong().orElse(def), 0L);
	}

	public static long getSpawnCooldown(ServerPlayerEntity player, long def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbessentials.spawn.cooldown").asLong().orElse(def), 0L);
	}

	public static long getWarpCooldown(ServerPlayerEntity player, long def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbessentials.warp.cooldown").asLong().orElse(def), 0L);
	}

	public static long getHomeCooldown(ServerPlayerEntity player, long def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbessentials.home.cooldown").asLong().orElse(def), 0L);
	}

	public static long getTpaCooldown(ServerPlayerEntity player, long def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbessentials.tpa.cooldown").asLong().orElse(def), 0L);
	}

	public static long getRtpCooldown(ServerPlayerEntity player, long def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbessentials.rtp.cooldown").asLong().orElse(def), 0L);
	}
}
