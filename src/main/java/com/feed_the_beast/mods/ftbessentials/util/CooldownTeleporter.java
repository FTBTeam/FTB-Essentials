package com.feed_the_beast.mods.ftbessentials.util;

import net.minecraft.entity.player.ServerPlayerEntity;

import java.util.function.Function;

/**
 * @author LatvianModder
 */
public class CooldownTeleporter
{
	public final FTBEPlayerData playerData;
	public final Function<ServerPlayerEntity, Long> cooldownGetter;
	public long cooldown;

	public CooldownTeleporter(FTBEPlayerData d, Function<ServerPlayerEntity, Long> c)
	{
		playerData = d;
		cooldownGetter = c;
		cooldown = 0L;
	}

	public TeleportPos.TeleportResult teleport(ServerPlayerEntity player, Function<ServerPlayerEntity, TeleportPos> positionGetter)
	{
		long now = System.currentTimeMillis();

		if (now < cooldown)
		{
			return (TeleportPos.CooldownTeleportResult) () -> cooldown - now;
		}

		cooldown = now + Math.max(0L, cooldownGetter.apply(player));

		TeleportPos p = positionGetter.apply(player);
		TeleportPos currentPos = new TeleportPos(player);

		TeleportPos.TeleportResult res0 = p.teleport(player);

		if (res0 != TeleportPos.TeleportResult.SUCCESS)
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