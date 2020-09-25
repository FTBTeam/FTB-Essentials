package com.feed_the_beast.mods.ftbessentials;

import net.minecraft.entity.player.ServerPlayerEntity;

/**
 * @author LatvianModder
 */
public class FTBEConfig
{
	public static int maxBack = 10;
	public static int maxHomes = 1;

	public static long backCooldown = 30000L;
	public static long deathCooldown = 0L;
	public static long spawnCooldown = 0L;
	public static long warpCooldown = 0L;
	public static long homeCooldown = 0L;
	public static long tpaCooldown = 0L;
	public static long rtpCooldown = 3600000L;

	public static long getBackCooldown(ServerPlayerEntity player)
	{
		return backCooldown;
	}

	public static long getDeathCooldown(ServerPlayerEntity player)
	{
		return deathCooldown;
	}

	public static long getSpawnCooldown(ServerPlayerEntity player)
	{
		return spawnCooldown;
	}

	public static long getWarpCooldown(ServerPlayerEntity player)
	{
		return warpCooldown;
	}

	public static long getHomeCooldown(ServerPlayerEntity player)
	{
		return homeCooldown;
	}

	public static long getTpaCooldown(ServerPlayerEntity player)
	{
		return tpaCooldown;
	}

	public static long getRtpCooldown(ServerPlayerEntity player)
	{
		return rtpCooldown;
	}
}
