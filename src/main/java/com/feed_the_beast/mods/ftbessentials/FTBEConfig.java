package com.feed_the_beast.mods.ftbessentials;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author LatvianModder
 */
public class FTBEConfig
{
	public static int maxBack;
	public static int maxHomes;
	public static long backCooldown;
	public static long spawnCooldown;
	public static long warpCooldown;
	public static long homeCooldown;
	public static long tpaCooldown;
	public static long rtpCooldown;
	public static int rtpMaxTries;
	public static double rtpMinDistance;
	public static double rtpMaxDistance;

	private static Pair<ServerConfig, ForgeConfigSpec> server;

	public static void init()
	{
		FMLJavaModLoadingContext.get().getModEventBus().register(FTBEConfig.class);

		server = new ForgeConfigSpec.Builder().configure(ServerConfig::new);

		ModLoadingContext modLoadingContext = ModLoadingContext.get();
		modLoadingContext.registerConfig(ModConfig.Type.SERVER, server.getRight());
	}

	@SubscribeEvent
	public static void reload(ModConfig.ModConfigEvent event)
	{
		ModConfig config = event.getConfig();

		if (config.getSpec() == server.getRight())
		{
			ServerConfig c = server.getLeft();
			maxBack = c.maxBack.get();
			maxHomes = c.maxHomes.get();
			backCooldown = c.backCooldown.get();
			spawnCooldown = c.spawnCooldown.get();
			warpCooldown = c.warpCooldown.get();
			homeCooldown = c.homeCooldown.get();
			tpaCooldown = c.tpaCooldown.get();
			rtpCooldown = c.rtpCooldown.get();
			rtpMaxTries = c.rtpMaxTries.get();
			rtpMinDistance = c.rtpMinDistance.get();
			rtpMaxDistance = c.rtpMaxDistance.get();
		}
	}

	private static class ServerConfig
	{
		private final ForgeConfigSpec.IntValue maxBack;
		private final ForgeConfigSpec.IntValue maxHomes;
		private final ForgeConfigSpec.LongValue backCooldown;
		private final ForgeConfigSpec.LongValue spawnCooldown;
		private final ForgeConfigSpec.LongValue warpCooldown;
		private final ForgeConfigSpec.LongValue homeCooldown;
		private final ForgeConfigSpec.LongValue tpaCooldown;
		private final ForgeConfigSpec.LongValue rtpCooldown;
		private final ForgeConfigSpec.IntValue rtpMaxTries;
		private final ForgeConfigSpec.DoubleValue rtpMinDistance;
		private final ForgeConfigSpec.DoubleValue rtpMaxDistance;

		private ServerConfig(ForgeConfigSpec.Builder builder)
		{
			maxBack = builder
					.comment("Max number of times you can use /back")
					.defineInRange("maxBack", 10, 0, Integer.MAX_VALUE);

			maxHomes = builder
					.comment("Max homes")
					.defineInRange("maxHomes", 1, 0, Integer.MAX_VALUE);

			backCooldown = builder
					.comment("/back cooldown in seconds")
					.defineInRange("backCooldown", 30L, 0L, 604800L);

			spawnCooldown = builder
					.comment("/spawn cooldown in seconds")
					.defineInRange("spawnCooldown", 10L, 0L, 604800L);

			warpCooldown = builder
					.comment("/warp cooldown in seconds")
					.defineInRange("warpCooldown", 10L, 0L, 604800L);

			homeCooldown = builder
					.comment("/home cooldown in seconds")
					.defineInRange("homeCooldown", 10L, 0L, 604800L);

			tpaCooldown = builder
					.comment("/tpa cooldown in seconds")
					.defineInRange("tpaCooldown", 10L, 0L, 604800L);

			rtpCooldown = builder
					.comment("/rtp cooldown in seconds")
					.defineInRange("rtpCooldown", 600L, 0L, 604800L);

			rtpMaxTries = builder
					.comment("Number of tries before /rtp gives up")
					.defineInRange("rtpMaxTries", 100, 1, 1000);

			rtpMinDistance = builder
					.comment("/rtp min distance from spawn point")
					.defineInRange("rtpMinDistance", 1000D, 0D, 30000000D);

			rtpMaxDistance = builder
					.comment("/rtp max distance from spawn point")
					.defineInRange("rtpMaxDistance", 100000D, 0D, 30000000D);
		}
	}

	/*
	public static int getMaxClaimedChunks(ClaimedChunkPlayerData playerData, ServerPlayerEntity player)
	{
		if (FTBChunks.ranksMod)
		{
			return FTBRanksIntegration.getMaxClaimedChunks(player, maxClaimedChunks) + playerData.getExtraClaimChunks();
		}

		return maxClaimedChunks + playerData.getExtraClaimChunks();
	}
	*/

	public static int getMaxBack(ServerPlayerEntity player)
	{
		return maxBack;
	}

	public static long getMaxHomes(ServerPlayerEntity player)
	{
		return maxHomes;
	}

	public static long getBackCooldown(ServerPlayerEntity player)
	{
		return backCooldown * 1000L;
	}

	public static long getSpawnCooldown(ServerPlayerEntity player)
	{
		return spawnCooldown * 1000L;
	}

	public static long getWarpCooldown(ServerPlayerEntity player)
	{
		return warpCooldown * 1000L;
	}

	public static long getHomeCooldown(ServerPlayerEntity player)
	{
		return homeCooldown * 1000L;
	}

	public static long getTpaCooldown(ServerPlayerEntity player)
	{
		return tpaCooldown * 1000L;
	}

	public static long getRtpCooldown(ServerPlayerEntity player)
	{
		return rtpCooldown * 1000L;
	}
}
