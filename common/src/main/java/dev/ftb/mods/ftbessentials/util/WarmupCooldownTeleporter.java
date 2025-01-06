package dev.ftb.mods.ftbessentials.util;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.ftb.mods.ftbessentials.api.event.TeleportEvent;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.TeleportPos.TeleportResult;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public class WarmupCooldownTeleporter {
	private final FTBEPlayerData playerData;
	private final ToIntFunction<ServerPlayer> cooldownConfig;
	private final ToIntFunction<ServerPlayer> warmupConfig;
	private final boolean popHistoryOnTeleport;

	private long lastRun;  // time of the last run of the command (which wasn't on cooldown)

	private static final Map<UUID, Warmup> WARMUPS = new HashMap<>();
	private static final Map<UUID, Warmup> pendingAdditions = new HashMap<>();
	private static final Set<UUID> pendingRemovals = new HashSet<>();

	public WarmupCooldownTeleporter(FTBEPlayerData playerData, ToIntFunction<ServerPlayer> cooldownConfig, ToIntFunction<ServerPlayer> warmupConfig) {
		this(playerData, cooldownConfig, warmupConfig, false);
	}

	public WarmupCooldownTeleporter(FTBEPlayerData playerData, ToIntFunction<ServerPlayer> cooldownConfig, ToIntFunction<ServerPlayer> warmupConfig, boolean popHistoryOnTeleport) {
		this.playerData = playerData;
		this.cooldownConfig = cooldownConfig;
		this.warmupConfig = warmupConfig;
		this.popHistoryOnTeleport = popHistoryOnTeleport;
		this.lastRun = 0L;
	}

	public TeleportResult checkCooldown(ServerPlayer player) {
		long now = System.currentTimeMillis();
		long nextRun = lastRun + Math.max(0L, cooldownConfig.applyAsInt(player) * 1000L);

		if (now < nextRun) {
			return (TeleportPos.CooldownTeleportResult) () -> nextRun - now;
		}

		return TeleportResult.SUCCESS;
	}

	@ExpectPlatform
	private static boolean firePlatformTeleportEvent(ServerPlayer player, Vec3 pos) {
		throw new AssertionError();
	}

	public TeleportResult teleport(ServerPlayer player, Function<ServerPlayer, TeleportPos> positionGetter) {
		TeleportResult cooldownResult = checkCooldown(player);
		if (!cooldownResult.isSuccess()) {
			return cooldownResult;
		}

		TeleportPos pos = positionGetter.apply(player);

		TeleportResult blacklistedResult = pos.checkDimensionBlacklist(player);
		if (!blacklistedResult.isSuccess()) {
			return blacklistedResult;
		}
		
		CompoundEventResult<Component> result = TeleportEvent.TELEPORT.invoker().teleport(player);
		if (result.isFalse()) {
			return TeleportResult.failed(result.object());
		}
		
		if (!firePlatformTeleportEvent(player, Vec3.atBottomCenterOf(pos.getPos()))) {
			return TeleportResult.failed(Component.translatable("ftbessentials.teleport_prevented"));
		}

		int warmupTime = warmupConfig.applyAsInt(player);

		if (warmupTime == 0) {
			// just port immediately
			return teleportNow(player, positionGetter);
		} else {
			// schedule the teleport
			pendingAdditions.put(player.getUUID(), new Warmup(System.currentTimeMillis() + warmupTime * 1000L, this, player.position(), positionGetter));
			return TeleportResult.SUCCESS;
		}
	}

	private TeleportResult teleportNow(ServerPlayer player, Function<ServerPlayer, TeleportPos> positionGetter) {
		lastRun = System.currentTimeMillis();

		TeleportPos teleportPos = positionGetter.apply(player);
		TeleportPos currentPos = new TeleportPos(player);

		TeleportResult res = teleportPos.teleport(player);
		if (res.isSuccess()) {
			if (popHistoryOnTeleport) {
				playerData.popTeleportHistory();
			} else if (!FTBEConfig.BACK_ON_DEATH_ONLY.get()) {
				playerData.addTeleportHistory(player, currentPos);
			}
		}
		return res;
	}

	public static void tickWarmups(MinecraftServer server) {
		WARMUPS.putAll(pendingAdditions);
		pendingAdditions.clear();

		pendingRemovals.forEach(WARMUPS::remove);
		pendingRemovals.clear();

		if (WARMUPS.isEmpty()) {
			return;
		}

		Set<UUID> toRemove = new HashSet<>();

		long now = System.currentTimeMillis();

		for (Map.Entry<UUID,Warmup> entry : WARMUPS.entrySet()) {
			UUID playerId = entry.getKey();
			ServerPlayer player = server.getPlayerList().getPlayer(playerId);
			if (player != null) {
				Warmup warmup = entry.getValue();
				if (warmup.when() <= now) {
					TeleportResult res = warmup.teleporter().teleportNow(player, warmup.positionGetter());
					toRemove.add(playerId);
					res.runCommand(player);
				} else {
					if (player.position().distanceToSqr(warmup.initialPos) > 0.25) {
						// player has moved more than half a block
						toRemove.add(playerId);
						player.displayClientMessage(Component.literal("Teleportation interrupted!").withStyle(ChatFormatting.RED), true);
					} else {
						long seconds = (warmup.when() - now) / 1000L;
						String secStr = seconds == 1 ? "second" : "seconds";
						player.displayClientMessage(Component.literal(String.format("Teleporting in %d %s", seconds, secStr)).withStyle(ChatFormatting.YELLOW), true);
					}
				}
			} else {
				// player has probably just gone offline, just cancel it
				toRemove.add(playerId);
			}
		}

		toRemove.forEach(WARMUPS::remove);
	}

	public static void cancelWarmup(ServerPlayer player) {
		if (WARMUPS.containsKey(player.getUUID())) {
			pendingRemovals.add(player.getUUID());
			player.displayClientMessage(Component.literal("Teleportation interrupted!").withStyle(ChatFormatting.RED), true);
		}
	}

	private record Warmup(
			long when,
			WarmupCooldownTeleporter teleporter,
			Vec3 initialPos,
			Function<ServerPlayer,TeleportPos> positionGetter
	) { }
}
