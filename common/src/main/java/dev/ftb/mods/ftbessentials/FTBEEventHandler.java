package dev.ftb.mods.ftbessentials;

import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.*;
import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbessentials.command.FTBEssentialsCommands;
import dev.ftb.mods.ftbessentials.command.TPACommands;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.FTBEWorldData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import dev.ftb.mods.ftbessentials.util.WarmupCooldownTeleporter;
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

import java.nio.file.Path;
import java.util.Iterator;

/**
 * @author LatvianModder
 */
public class FTBEEventHandler {
	public static final LevelResource CONFIG_FILE = new LevelResource("serverconfig/ftbessentials.snbt");
	private static final String[] DEFAULT_CONFIG = {
			"Default config file that will be copied to world's serverconfig/ftbessentials.snbt location",
			"Copy values you wish to override in here",
			"Example:",
			"",
			"{",
			"	misc: {",
			"		enderchest: {",
			"			enabled: false",
			"		}",
			"	}",
			"}",
	};

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
		Path configFilePath = minecraftServer.getWorldPath(CONFIG_FILE);
		Path defaultConfigFilePath = Platform.getConfigFolder().resolve("../defaultconfigs/ftbessentials-server.snbt");

		FTBEConfig.CONFIG.load(configFilePath, defaultConfigFilePath, () -> DEFAULT_CONFIG);

		FTBEPlayerData.MAP.clear();
		FTBEWorldData.instance = new FTBEWorldData(minecraftServer);
		FTBEWorldData.instance.load();
	}

	private static void serverStopped(MinecraftServer minecraftServer) {
		FTBEWorldData.instance = null;
		TPACommands.REQUESTS.clear();
	}

	private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection commandSelection) {
		FTBEssentialsCommands.registerCommands(dispatcher);
	}

	private static void levelSave(ServerLevel serverLevel) {
		if (FTBEWorldData.instance != null) {
			FTBEWorldData.instance.saveNow();

			if (Platform.isFabric()) {
				// on Forge, data is saved by the PlayerEvent.SaveToFile event handler
				FTBEPlayerData.MAP.values().forEach(FTBEPlayerData::saveNow);
			}
		}
	}

	private static void playerLoggedIn(ServerPlayer serverPlayer) {
		FTBEPlayerData data = FTBEPlayerData.get(serverPlayer);
		if (data != null) {
			if (Platform.isFabric()) {
				// on Forge, data is loaded by the PlayerEvent.LoadFromFile event handler
				data.load();
			}
			data.lastSeen = new TeleportPos(serverPlayer);
			data.markDirty();

			for (FTBEPlayerData d : FTBEPlayerData.MAP.values()) {
				d.sendTabName(serverPlayer);
			}
		}
	}

	private static void playerLoggedOut(ServerPlayer serverPlayer) {
		FTBEPlayerData data = FTBEPlayerData.get(serverPlayer);
		if (data != null) {
			data.lastSeen = new TeleportPos(serverPlayer);
			data.markDirty();
		}
	}

	private static void playerTickPost(Player player) {
		var data = FTBEPlayerData.get(player);
		var abilities = player.getAbilities();

		if (data == null) {
			return;
		}

		if (data.god && !abilities.invulnerable) {
			abilities.invulnerable = true;
			player.onUpdateAbilities();
		}

		if (data.fly && !abilities.mayfly) {
			abilities.mayfly = true;
			player.onUpdateAbilities();
		}
	}

	private static void serverTickPost(MinecraftServer server) {
		long now = System.currentTimeMillis();

		Iterator<TPACommands.TPARequest> iterator = TPACommands.REQUESTS.values().iterator();

		while (iterator.hasNext()) {
			TPACommands.TPARequest r = iterator.next();

			if (now > r.created() + 60000L) {
				ServerPlayer source = server.getPlayerList().getPlayer(r.source().uuid);
				ServerPlayer target = server.getPlayerList().getPlayer(r.target().uuid);

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
		}
	}

	// FIXME this should run with HIGHEST priority but we can't do that with Arch
	private static EventResult playerChat(@Nullable ServerPlayer serverPlayer, Component component) {
		if (serverPlayer != null) {
			FTBEPlayerData data = FTBEPlayerData.get(serverPlayer);
			if (data != null && data.muted) {
				serverPlayer.displayClientMessage(Component.literal("You can't use chat, you've been muted by an admin!")
						.withStyle(ChatFormatting.RED), false);
				return EventResult.interruptFalse();
			}
		}

		return EventResult.pass();
	}

	private static void onPlayerDeath(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean wonGame) {
		// this is better than checking for living death event, because player cloning isn't cancellable
		// the player death event is cancellable, and we can't detect cancelled events with Architectury
		if (!wonGame) {
			FTBEPlayerData.addTeleportHistory(oldPlayer);
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
