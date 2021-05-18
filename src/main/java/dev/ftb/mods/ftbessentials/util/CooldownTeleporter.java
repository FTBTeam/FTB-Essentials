package dev.ftb.mods.ftbessentials.util;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.Function;

/**
 * @author LatvianModder
 */
public class CooldownTeleporter {
	public final FTBEPlayerData playerData;
	public final Function<ServerPlayer, Long> cooldownGetter;
	public long cooldown;

	public CooldownTeleporter(FTBEPlayerData d, Function<ServerPlayer, Long> c) {
		playerData = d;
		cooldownGetter = c;
		cooldown = 0L;
	}

	public TeleportPos.TeleportResult checkCooldown() {
		long now = System.currentTimeMillis();

		if (now < cooldown) {
			return (TeleportPos.CooldownTeleportResult) () -> cooldown - now;
		}

		return TeleportPos.TeleportResult.SUCCESS;
	}

	public TeleportPos.TeleportResult teleport(ServerPlayer player, Function<ServerPlayer, TeleportPos> positionGetter) {
		TeleportPos.TeleportResult res0 = checkCooldown();

		if (!res0.isSuccess()) {
			return res0;
		}

		cooldown = System.currentTimeMillis() + Math.max(0L, cooldownGetter.apply(player) * 1000L);

		TeleportPos p = positionGetter.apply(player);
		TeleportPos currentPos = new TeleportPos(player);

		res0 = p.teleport(player);

		if (!res0.isSuccess()) {
			return res0;
		}

		if (this != playerData.backTeleporter) {
			playerData.addTeleportHistory(player, currentPos);
		}

		return TeleportPos.TeleportResult.SUCCESS;
	}
}