package com.feed_the_beast.mods.ftbessentials.command;

import com.feed_the_beast.mods.ftbessentials.util.FTBEPlayerData;
import com.feed_the_beast.mods.ftbessentials.util.OtherPlayerInventory;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

/**
 * @author LatvianModder
 */
public class CheatCommands
{
	public static void register(CommandDispatcher<CommandSource> dispatcher)
	{
		/*
		killall
		dumpchunkloaders
		 */

		dispatcher.register(Commands.literal("heal")
				.requires(source -> source.hasPermissionLevel(2))
				.executes(context -> heal(context.getSource().asPlayer()))
				.then(Commands.argument("player", EntityArgument.player())
						.requires(source -> source.hasPermissionLevel(2))
						.executes(context -> heal(EntityArgument.getPlayer(context, "player")))
				)
		);

		dispatcher.register(Commands.literal("fly")
				.requires(source -> source.hasPermissionLevel(2))
				.executes(context -> fly(context.getSource().asPlayer()))
				.then(Commands.argument("player", EntityArgument.player())
						.requires(source -> source.hasPermissionLevel(2))
						.executes(context -> fly(EntityArgument.getPlayer(context, "player")))
				)
		);

		dispatcher.register(Commands.literal("god")
				.requires(source -> source.hasPermissionLevel(2))
				.executes(context -> god(context.getSource().asPlayer()))
				.then(Commands.argument("player", EntityArgument.player())
						.requires(source -> source.hasPermissionLevel(2))
						.executes(context -> god(EntityArgument.getPlayer(context, "player")))
				)
		);

		dispatcher.register(Commands.literal("invsee")
				.requires(source -> source.hasPermissionLevel(2))
				.then(Commands.argument("player", EntityArgument.player())
						.requires(source -> source.hasPermissionLevel(2))
						.executes(context -> invsee(context.getSource().asPlayer(), EntityArgument.getPlayer(context, "player")))
				)
		);

		dispatcher.register(Commands.literal("nicknamefor")
				.requires(source -> source.hasPermissionLevel(2))
				.then(Commands.argument("player", EntityArgument.player())
						.requires(source -> source.hasPermissionLevel(2))
						.executes(context -> nicknamefor(context.getSource(), EntityArgument.getPlayer(context, "player"), ""))
						.then(Commands.argument("nickname", StringArgumentType.greedyString())
								.requires(source -> source.hasPermissionLevel(2))
								.executes(context -> nicknamefor(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "nickname")))
						)
				)
		);

		dispatcher.register(Commands.literal("mute")
				.requires(source -> source.hasPermissionLevel(2))
				.then(Commands.argument("player", EntityArgument.player())
						.requires(source -> source.hasPermissionLevel(2))
						.executes(context -> mute(context.getSource(), EntityArgument.getPlayer(context, "player")))
				)
		);

		dispatcher.register(Commands.literal("unmute")
				.requires(source -> source.hasPermissionLevel(2))
				.then(Commands.argument("player", EntityArgument.player())
						.requires(source -> source.hasPermissionLevel(2))
						.executes(context -> unmute(context.getSource(), EntityArgument.getPlayer(context, "player")))
				)
		);
	}

	public static int heal(ServerPlayerEntity player)
	{
		player.setHealth(player.getMaxHealth());
		player.getFoodStats().addStats(40, 40F);
		player.extinguish();
		player.curePotionEffects(new ItemStack(Items.MILK_BUCKET));
		return 1;
	}

	public static int fly(ServerPlayerEntity player)
	{
		FTBEPlayerData data = FTBEPlayerData.get(player);

		if (data.fly)
		{
			data.fly = false;
			data.save();
			player.abilities.allowFlying = false;
			player.abilities.isFlying = false;
			player.sendStatusMessage(new StringTextComponent("Flight disabled"), true);
		}
		else
		{
			data.fly = true;
			data.save();
			player.abilities.allowFlying = true;
			player.sendStatusMessage(new StringTextComponent("Flight enabled"), true);
		}

		player.sendPlayerAbilities();
		return 1;
	}

	public static int god(ServerPlayerEntity player)
	{
		FTBEPlayerData data = FTBEPlayerData.get(player);

		if (data.god)
		{
			data.god = false;
			data.save();
			player.abilities.allowFlying = false;
			player.abilities.isFlying = false;
			player.sendStatusMessage(new StringTextComponent("God mode disabled"), true);
		}
		else
		{
			data.god = true;
			data.save();
			player.abilities.allowFlying = true;
			player.sendStatusMessage(new StringTextComponent("God mode enabled"), true);
		}

		player.sendPlayerAbilities();
		return 1;
	}

	public static int invsee(ServerPlayerEntity source, ServerPlayerEntity player)
	{
		source.openContainer(new INamedContainerProvider()
		{
			@Override
			public ITextComponent getDisplayName()
			{
				return player.getDisplayName();
			}

			@Override
			public Container createMenu(int id, PlayerInventory playerInventory, PlayerEntity p)
			{
				return new ChestContainer(ContainerType.GENERIC_9X5, id, playerInventory, new OtherPlayerInventory(player), 5);
			}
		});

		return 1;
	}

	public static int nicknamefor(CommandSource source, ServerPlayerEntity player, String nick)
	{
		if (nick.length() > 30)
		{
			player.sendStatusMessage(new StringTextComponent("Nickname too long!"), false);
			return 0;
		}

		FTBEPlayerData data = FTBEPlayerData.get(player);
		data.nick = nick.trim();
		data.save();
		player.refreshDisplayName();

		if (data.nick.isEmpty())
		{
			source.sendFeedback(new StringTextComponent("Nickname reset!"), true);
		}
		else
		{
			source.sendFeedback(new StringTextComponent("Nickname changed to '" + data.nick + "'"), true);
		}

		data.sendTabName();
		return 1;
	}

	public static int mute(CommandSource source, ServerPlayerEntity player)
	{
		FTBEPlayerData data = FTBEPlayerData.get(player);
		data.muted = true;
		data.save();
		source.sendFeedback(new StringTextComponent("").append(player.getDisplayName()).appendString(" has been muted by ").append(source.getDisplayName()), true);
		return 1;
	}

	public static int unmute(CommandSource source, ServerPlayerEntity player)
	{
		FTBEPlayerData data = FTBEPlayerData.get(player);
		data.muted = false;
		data.save();
		source.sendFeedback(new StringTextComponent("").append(player.getDisplayName()).appendString(" has been unmuted by ").append(source.getDisplayName()), true);
		return 1;
	}
}
