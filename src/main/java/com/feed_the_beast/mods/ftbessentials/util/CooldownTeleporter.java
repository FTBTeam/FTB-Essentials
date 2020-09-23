package com.feed_the_beast.mods.ftbessentials.util;

import net.minecraft.entity.player.ServerPlayerEntity;

import java.util.function.Function;

/**
 * @author LatvianModder
 */
public class CooldownTeleporter
{
	public final FTBEPlayerData playerData;

	public CooldownTeleporter(FTBEPlayerData d)
	{
		playerData = d;
	}

	public boolean teleport(ServerPlayerEntity player, Function<ServerPlayerEntity, TeleportPos> positionGetter)
	{
		TeleportPos p = positionGetter.apply(player);
		TeleportPos currentPos = new TeleportPos(player);

		if (!p.teleport(player))
		{
			return false;
		}
		else if (this != playerData.backTeleporter)
		{
			playerData.addTeleportHistory(player, currentPos);
			playerData.save();
		}

		return true;
	}
}