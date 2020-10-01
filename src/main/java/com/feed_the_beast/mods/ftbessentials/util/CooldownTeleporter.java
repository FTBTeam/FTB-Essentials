package com.feed_the_beast.mods.ftbessentials.util;

import net.minecraft.entity.player.ServerPlayerEntity;

import java.util.function.Function;

/**
 * @author LatvianModder
 */
public class CooldownTeleporter
{
	public static String prettyTimeString(long seconds)
	{
		if (seconds <= 0L)
		{
			return "0 seconds";
		}

		StringBuilder builder = new StringBuilder();
		prettyTimeString(builder, seconds, true);
		return builder.toString();
	}

	private static void prettyTimeString(StringBuilder builder, long seconds, boolean addAnother)
	{
		if (seconds <= 0L)
		{
			return;
		}
		else if (!addAnother)
		{
			builder.append(" and ");
		}

		if (seconds < 60L)
		{
			builder.append(seconds);
			builder.append(seconds == 1L ? " second" : " seconds");
		}
		else if (seconds < 3600L)
		{
			builder.append(seconds / 60L);
			builder.append(seconds / 60L == 1L ? " minute" : " minutes");

			if (addAnother)
			{
				prettyTimeString(builder, seconds % 60L, false);
			}
		}
		else if (seconds < 86400L)
		{
			builder.append(seconds / 3600L);
			builder.append(seconds / 3600L == 1L ? " hour" : " hours");

			if (addAnother)
			{
				prettyTimeString(builder, seconds % 3600L, false);
			}
		}
		else
		{
			builder.append(seconds / 86400L);
			builder.append(seconds / 86400L == 1L ? " day" : " days");

			if (addAnother)
			{
				prettyTimeString(builder, seconds % 86400L, false);
			}
		}
	}

	public final FTBEPlayerData playerData;
	public final Function<ServerPlayerEntity, Long> cooldownGetter;
	public long cooldown;

	public CooldownTeleporter(FTBEPlayerData d, Function<ServerPlayerEntity, Long> c)
	{
		playerData = d;
		cooldownGetter = c;
		cooldown = 0L;
	}

	public TeleportPos.TeleportResult checkCooldown()
	{
		long now = System.currentTimeMillis();

		if (now < cooldown)
		{
			return (TeleportPos.CooldownTeleportResult) () -> cooldown - now;
		}

		return TeleportPos.TeleportResult.SUCCESS;
	}

	public TeleportPos.TeleportResult teleport(ServerPlayerEntity player, Function<ServerPlayerEntity, TeleportPos> positionGetter)
	{
		TeleportPos.TeleportResult res0 = checkCooldown();

		if (!res0.isSuccess())
		{
			return res0;
		}

		cooldown = System.currentTimeMillis() + Math.max(0L, cooldownGetter.apply(player));

		TeleportPos p = positionGetter.apply(player);
		TeleportPos currentPos = new TeleportPos(player);

		res0 = p.teleport(player);

		if (!res0.isSuccess())
		{
			return res0;
		}

		if (this != playerData.backTeleporter)
		{
			playerData.addTeleportHistory(player, currentPos);
		}

		return TeleportPos.TeleportResult.SUCCESS;
	}
}