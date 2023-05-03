package dev.ftb.mods.ftbessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.ftb.mods.ftbessentials.FTBEssentialsPlatform;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.DurationInfo;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.FTBEWorldData;
import dev.ftb.mods.ftbessentials.util.OtherPlayerInventory;
import dev.ftb.mods.ftblibrary.util.PlayerDisplayNameUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * @author LatvianModder
 */
public class CheatCommands {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		/*
		killall
		dumpchunkloaders
		 */

		if (FTBEConfig.HEAL.isEnabled()) {
			dispatcher.register(Commands.literal("heal")
					.requires(FTBEConfig.HEAL.enabledAndOp())
					.executes(context -> heal(context.getSource().getPlayerOrException()))
					.then(Commands.argument("player", EntityArgument.player())
							.executes(context -> heal(EntityArgument.getPlayer(context, "player")))
					)
			);
		}

		if (FTBEConfig.FLY.isEnabled()) {
			dispatcher.register(Commands.literal("fly")
					.requires(FTBEConfig.FLY.enabledAndOp())
					.executes(context -> fly(context.getSource().getPlayerOrException()))
					.then(Commands.argument("player", EntityArgument.player())
							.executes(context -> fly(EntityArgument.getPlayer(context, "player")))
					)
			);
		}

		if (FTBEConfig.GOD.isEnabled()) {
			dispatcher.register(Commands.literal("god")
					.requires(FTBEConfig.GOD.enabledAndOp())
					.executes(context -> god(context.getSource().getPlayerOrException()))
					.then(Commands.argument("player", EntityArgument.player())
							.executes(context -> god(EntityArgument.getPlayer(context, "player")))
					)
			);
		}

		if (FTBEConfig.INVSEE.isEnabled()) {
			dispatcher.register(Commands.literal("invsee")
					.requires(FTBEConfig.INVSEE.enabledAndOp())
					.then(Commands.argument("player", EntityArgument.player())
							.executes(context -> invsee(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))
					)
			);
		}

		if (FTBEConfig.NICK.isEnabled()) {
			dispatcher.register(Commands.literal("nicknamefor")
					.requires(FTBEConfig.NICK.enabledAndOp())
					.then(Commands.argument("player", EntityArgument.player())
							.requires(source -> source.hasPermission(2))
							.executes(context -> nicknamefor(context.getSource(), EntityArgument.getPlayer(context, "player"), ""))
							.then(Commands.argument("nickname", StringArgumentType.greedyString())
									.requires(source -> source.hasPermission(2))
									.executes(context -> nicknamefor(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "nickname")))
							)
					)
			);
		}

		if (FTBEConfig.MUTE.isEnabled()) {
			dispatcher.register(Commands.literal("mute")
					.requires(FTBEConfig.MUTE.enabledAndOp())
					.then(Commands.argument("player", EntityArgument.player())
							.executes(context -> mute(context.getSource(), EntityArgument.getPlayer(context, "player"), ""))
							.then(Commands.argument("until", StringArgumentType.greedyString())
									.suggests((context, builder) -> suggestTimeouts(builder))
									.executes(context -> mute(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "until")))
							)
					)
			);
			dispatcher.register(Commands.literal("unmute")
					.requires(FTBEConfig.MUTE.enabledAndOp())
					.then(Commands.argument("player", EntityArgument.player())
							.executes(context -> unmute(context.getSource(), EntityArgument.getPlayer(context, "player")))
					)
			);
		}
	}

	public static int heal(ServerPlayer player) {
		player.setHealth(player.getMaxHealth());
		player.getFoodData().eat(40, 40F);
		player.clearFire();
		FTBEssentialsPlatform.curePotionEffects(player);
		return 1;
	}

	public static int fly(ServerPlayer player) {
		var data = FTBEPlayerData.get(player);
		if (data == null) return 0;
		var abilities = player.getAbilities();

		if (data.fly) {
			data.fly = false;
			data.markDirty();
			abilities.mayfly = false;
			abilities.flying = false;
			player.displayClientMessage(Component.literal("Flight disabled"), true);
		} else {
			data.fly = true;
			data.markDirty();
			abilities.mayfly = true;
			player.displayClientMessage(Component.literal("Flight enabled"), true);
		}

		player.onUpdateAbilities();
		return 1;
	}

	public static int god(ServerPlayer player) {
		var data = FTBEPlayerData.get(player);
		if (data == null) return 0;
		var abilities = player.getAbilities();

		if (data.god) {
			data.god = false;
			data.markDirty();
			abilities.invulnerable = false;
			player.displayClientMessage(Component.literal("God mode disabled"), true);
		} else {
			data.god = true;
			data.markDirty();
			abilities.invulnerable = true;
			player.displayClientMessage(Component.literal("God mode enabled"), true);
		}

		player.onUpdateAbilities();
		return 1;
	}

	public static int invsee(ServerPlayer source, ServerPlayer player) {
		source.openMenu(new MenuProvider() {
			@Override
			public Component getDisplayName() {
				return player.getDisplayName();
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player p) {
				return new ChestMenu(MenuType.GENERIC_9x5, id, playerInventory, new OtherPlayerInventory(player), 5);
			}
		});

		return 1;
	}

	public static int nicknamefor(CommandSourceStack source, ServerPlayer player, String nick) {
		if (nick.length() > 30) {
			player.displayClientMessage(Component.literal("Nickname too long!"), false);
			return 0;
		}

		FTBEPlayerData data = FTBEPlayerData.get(player);
		if (data == null) return 0;
		data.nick = nick.trim();
		data.markDirty();
		PlayerDisplayNameUtil.refreshDisplayName(player);

		if (data.nick.isEmpty()) {
			source.sendSuccess(Component.literal("Nickname reset!"), true);
		} else {
			source.sendSuccess(Component.literal("Nickname changed to '" + data.nick + "'"), true);
		}

		data.sendTabName(source.getServer());
		return 1;
	}

	public static int mute(CommandSourceStack source, ServerPlayer player, String duration) {
		FTBEPlayerData data = FTBEPlayerData.get(player);
		if (data == null) return 0;

		try {
			DurationInfo info = DurationInfo.fromString(duration);
			data.muted = true;
			FTBEWorldData.instance.setMuteTimeout(player, info.until());

			data.markDirty();

			MutableComponent msg = player.getDisplayName().copy()
					.append(" has been muted by ")
					.append(source.getDisplayName())
					.append(", ")
					.append(info.desc());
			notifyMuting(source, player, msg);

			return 1;
		} catch (IllegalArgumentException e) {
			source.sendFailure(Component.literal("Invalid duration syntax: '" + duration + "': " + e.getMessage()));
			return 0;
		}
	}

	private static CompletableFuture<Suggestions> suggestTimeouts(SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(Stream.of("5m", "10m", "1h", "1d", "1w", "<number>[smhdw]"), builder);
	}

	public static int unmute(CommandSourceStack source, ServerPlayer player) {
		FTBEPlayerData data = FTBEPlayerData.get(player);
		if (data == null) return 0;

		data.muted = false;
		FTBEWorldData.instance.setMuteTimeout(player, -1);
		data.markDirty();

		MutableComponent msg = player.getDisplayName().copy()
				.append(" has been unmuted by ")
				.append(source.getDisplayName());
		notifyMuting(source, player, msg);

		return 1;
	}

	private static void notifyMuting(CommandSourceStack source, Player target, Component msg) {
		// notify any online ops, plus the player being (un)muted
		source.getServer().getPlayerList().getPlayers().forEach(p -> {
			if (p.hasPermissions(2) || p == target) {
				p.displayClientMessage(msg, false);
			}
		});
		// notify command sender if not actually a player
		if (!source.isPlayer()) {
			source.sendSuccess(msg, true);
		}
	}
}
