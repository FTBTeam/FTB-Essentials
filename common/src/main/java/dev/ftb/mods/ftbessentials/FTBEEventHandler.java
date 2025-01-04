package dev.ftb.mods.ftbessentials;

import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.*;
import dev.ftb.mods.ftbessentials.api.records.TPARequest;
import dev.ftb.mods.ftbessentials.commands.FTBCommands;
import dev.ftb.mods.ftbessentials.commands.impl.teleporting.TPACommand;
import dev.ftb.mods.ftbessentials.kit.KitManager;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.FTBEWorldData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import dev.ftb.mods.ftbessentials.util.WarmupCooldownTeleporter;
import dev.ftb.mods.ftblibrary.util.TimeUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

public class FTBEEventHandler {

	public static void init() {
		LifecycleEvent.SERVER_BEFORE_START.register(FTBEEventHandler::serverAboutToStart);
		LifecycleEvent.SERVER_STOPPED.register(FTBEEventHandler::serverStopped);
		LifecycleEvent.SERVER_LEVEL_SAVE.register(FTBEEventHandler::levelSave);

		TickEvent.SERVER_POST.register(FTBEEventHandler::serverTickPost);
		TickEvent.PLAYER_POST.register(FTBEEventHandler::playerTickPost);

		CommandRegistrationEvent.EVENT.register(FTBEEventHandler::registerCommands);

		PlayerEvent.PLAYER_JOIN.register(FTBEEventHandler::playerLoggedIn);
		PlayerEvent.PLAYER_QUIT.register(FTBEEventHandler::playerLoggedOut);
		PlayerEvent.PLAYER_CLONE.register(FTBEEventHandler::onPlayerDeath);
		PlayerEvent.CHANGE_DIMENSION.register(FTBEEventHandler::playerChangedDimension);

		EntityEvent.LIVING_HURT.register(FTBEEventHandler::playerHurt);

		ChatEvent.RECEIVED.register(FTBEEventHandler::playerChat);
	}

	private static void serverAboutToStart(MinecraftServer minecraftServer) {
		FTBEPlayerData.clear();
		FTBEWorldData.instance = new FTBEWorldData(minecraftServer);
		FTBEWorldData.instance.load();

		Path oldConfigPath = minecraftServer.getWorldPath(LevelResource.ROOT)
				.resolve("serverconfig")
				.resolve(FTBEssentials.CONFIG_FILE);
		if (!Files.exists(oldConfigPath)) {
			// create a placeholder file for where config used to be
			try {
				Files.writeString(oldConfigPath, "# File has moved!\n# FTB Essentials configuration is now in <instance-folder>/config/ftbessentials.snbt\n");
			} catch (IOException e) {
				FTBEssentials.LOGGER.error("can't write {}: {}", oldConfigPath, e.getMessage());
			}
		}
	}

	private static void serverStopped(MinecraftServer minecraftServer) {
		FTBEWorldData.instance = null;
		TPACommand.clearRequests();
	}

	private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection commandSelection) {
		FTBCommands.register(dispatcher);
	}

	private static void levelSave(ServerLevel serverLevel) {
		if (FTBEWorldData.instance != null) {
			FTBEWorldData.instance.saveIfChanged();
			FTBEPlayerData.saveAll();
		}
	}

	private static void playerLoggedIn(ServerPlayer serverPlayer) {
		FTBEPlayerData.getOrCreate(serverPlayer).ifPresent(data -> {
			data.load();
			data.setLastSeenPos(new TeleportPos(serverPlayer));
			data.markDirty();

			FTBEPlayerData.sendPlayerTabs(serverPlayer);

			KitManager.getInstance().allKits().forEach(kit -> {
				if (kit.isAutoGrant()) {
					kit.giveToPlayer(serverPlayer, data, false);
				}
			});
		});
	}

	private static void playerLoggedOut(ServerPlayer serverPlayer) {
		FTBEPlayerData.getOrCreate(serverPlayer).ifPresent(data -> {
			data.setLastSeenPos(new TeleportPos(serverPlayer));
			data.saveIfChanged();
		});
	}

	private static void playerTickPost(Player player) {
		if (!player.level().isClientSide) {
			FTBEPlayerData.getOrCreate(player).ifPresent(data -> {
				var abilities = player.getAbilities();

				if (data.isGod() && !abilities.invulnerable) {
					abilities.invulnerable = true;
					player.onUpdateAbilities();
				}

				if (data.canFly() && !abilities.mayfly) {
					abilities.mayfly = true;
					player.onUpdateAbilities();
				}
			});
		}
	}

	private static void serverTickPost(MinecraftServer server) {
		long now = System.currentTimeMillis();

		Iterator<TPARequest> iterator = TPACommand.requests().values().iterator();

		while (iterator.hasNext()) {
			TPARequest r = iterator.next();

			if (now > r.created() + 60000L) {
				ServerPlayer source = server.getPlayerList().getPlayer(r.source().getUuid());
				ServerPlayer target = server.getPlayerList().getPlayer(r.target().getUuid());

				if (source != null) {
					source.sendSystemMessage(Component.literal("TPA request expired!"));
				}

				if (target != null) {
					target.sendSystemMessage(Component.literal("TPA request expired!"));
				}

				iterator.remove();
			}
		}

		if (server.getTickCount() % 20 == 0) {
			WarmupCooldownTeleporter.tickWarmups(server);
			FTBEWorldData.instance.tickMuteTimeouts(server);
		}
	}

	// FIXME this should run with HIGHEST priority but we can't do that with Arch
	private static EventResult playerChat(@Nullable ServerPlayer serverPlayer, Component component) {
		return FTBEPlayerData.getOrCreate(serverPlayer).map(data -> {
			if (data.isMuted()) {
				// serverPlayer must be non-null if we got the player data
				//noinspection DataFlowIssue
				serverPlayer.displayClientMessage(Component.literal("You can't use chat, you've been muted by an admin!")
						.withStyle(ChatFormatting.RED), false);
				FTBEWorldData.instance.getMuteTimeout(serverPlayer).ifPresent(expiry -> {
					long left = (expiry - System.currentTimeMillis()) / 1000L;
					serverPlayer.displayClientMessage(Component.literal("Mute expiry in: " + TimeUtils.prettyTimeString(left)).withStyle(ChatFormatting.RED), false);
				});
				return EventResult.interruptFalse();
			}
			return EventResult.pass();
		}).orElse(EventResult.pass());
	}

	private static void onPlayerDeath(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean wonGame) {
		// this is better than checking for living death event, because player cloning isn't cancellable
		// the player death event is cancellable, and we can't detect cancelled events with Architectury
		if (!wonGame) {
			// note: architectury changed the parameter order for the player clone event
			// we can work with old and new versions of arch if we check which player is dead, and use that location
			if (newPlayer.isAlive()) {
				oldPlayer.getLastDeathLocation().ifPresent(loc -> FTBEPlayerData.addTeleportHistory(oldPlayer));
			} else if (oldPlayer.isAlive()) {
				newPlayer.getLastDeathLocation().ifPresent(loc -> FTBEPlayerData.addTeleportHistory(newPlayer));
			}
		}
	}

	private static EventResult playerHurt(LivingEntity livingEntity, DamageSource damageSource, float amount) {
		if (livingEntity instanceof ServerPlayer sp && amount > 0f) {
			WarmupCooldownTeleporter.cancelWarmup(sp);
		}
		return EventResult.pass();
	}

	private static void playerChangedDimension(ServerPlayer serverPlayer, ResourceKey<Level> oldDimension, ResourceKey<Level> newDimension) {
		WarmupCooldownTeleporter.cancelWarmup(serverPlayer);
	}
}
