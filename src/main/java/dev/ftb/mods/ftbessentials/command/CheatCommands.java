package dev.ftb.mods.ftbessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.OtherPlayerInventory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * @author LatvianModder
 */
public class CheatCommands {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		/*
		killall
		dumpchunkloaders
		 */

		dispatcher.register(Commands.literal("heal")
				.requires(FTBEConfig.HEAL.enabledAndOp())
				.executes(context -> heal(context.getSource().getPlayerOrException()))
				.then(Commands.argument("player", EntityArgument.player())
						.executes(context -> heal(EntityArgument.getPlayer(context, "player")))
				)
		);

		dispatcher.register(Commands.literal("fly")
				.requires(FTBEConfig.FLY.enabledAndOp())
				.executes(context -> fly(context.getSource().getPlayerOrException()))
				.then(Commands.argument("player", EntityArgument.player())
						.executes(context -> fly(EntityArgument.getPlayer(context, "player")))
				)
		);

		dispatcher.register(Commands.literal("god")
				.requires(FTBEConfig.GOD.enabledAndOp())
				.executes(context -> god(context.getSource().getPlayerOrException()))
				.then(Commands.argument("player", EntityArgument.player())
						.executes(context -> god(EntityArgument.getPlayer(context, "player")))
				)
		);

		dispatcher.register(Commands.literal("invsee")
				.requires(FTBEConfig.INVSEE.enabledAndOp())
				.then(Commands.argument("player", EntityArgument.player())
						.executes(context -> invsee(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))
				)
		);

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

		dispatcher.register(Commands.literal("mute")
				.requires(FTBEConfig.MUTE.enabledAndOp())
				.requires(source -> source.hasPermission(2))
				.then(Commands.argument("player", EntityArgument.player())
						.requires(source -> source.hasPermission(2))
						.executes(context -> mute(context.getSource(), EntityArgument.getPlayer(context, "player")))
				)
		);

		dispatcher.register(Commands.literal("unmute")
				.requires(FTBEConfig.MUTE.enabledAndOp())
				.then(Commands.argument("player", EntityArgument.player())
						.requires(source -> source.hasPermission(2))
						.executes(context -> unmute(context.getSource(), EntityArgument.getPlayer(context, "player")))
				)
		);
	}

	public static int heal(ServerPlayer player) {
		player.setHealth(player.getMaxHealth());
		player.getFoodData().eat(40, 40F);
		player.clearFire();
		player.curePotionEffects(new ItemStack(Items.MILK_BUCKET));
		return 1;
	}

	public static int fly(ServerPlayer player) {
		var data = FTBEPlayerData.get(player);
		var abilities = player.getAbilities();

		if (data.fly) {
			data.fly = false;
			data.save();
			abilities.mayfly = false;
			abilities.flying = false;
			player.displayClientMessage(new TextComponent("Flight disabled"), true);
		} else {
			data.fly = true;
			data.save();
			abilities.mayfly = true;
			player.displayClientMessage(new TextComponent("Flight enabled"), true);
		}

		player.onUpdateAbilities();
		return 1;
	}

	public static int god(ServerPlayer player) {
		var data = FTBEPlayerData.get(player);
		var abilities = player.getAbilities();

		if (data.god) {
			data.god = false;
			data.save();
			abilities.invulnerable = false;
			player.displayClientMessage(new TextComponent("God mode disabled"), true);
		} else {
			data.god = true;
			data.save();
			abilities.invulnerable = true;
			player.displayClientMessage(new TextComponent("God mode enabled"), true);
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
			player.displayClientMessage(new TextComponent("Nickname too long!"), false);
			return 0;
		}

		FTBEPlayerData data = FTBEPlayerData.get(player);
		data.nick = nick.trim();
		data.save();
		player.refreshDisplayName();

		if (data.nick.isEmpty()) {
			source.sendSuccess(new TextComponent("Nickname reset!"), true);
		} else {
			source.sendSuccess(new TextComponent("Nickname changed to '" + data.nick + "'"), true);
		}

		data.sendTabName(source.getServer());
		return 1;
	}

	public static int mute(CommandSourceStack source, ServerPlayer player) {
		FTBEPlayerData data = FTBEPlayerData.get(player);
		data.muted = true;
		data.save();
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" has been muted by ").append(source.getDisplayName()), true);
		return 1;
	}

	public static int unmute(CommandSourceStack source, ServerPlayer player) {
		FTBEPlayerData data = FTBEPlayerData.get(player);
		data.muted = false;
		data.save();
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" has been unmuted by ").append(source.getDisplayName()), true);
		return 1;
	}
}
