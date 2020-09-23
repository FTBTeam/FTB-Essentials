package com.feed_the_beast.mods.ftbessentials.command;

import com.feed_the_beast.mods.ftbessentials.FTBEPlayerData;
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
				.executes(context -> heal(context.getSource(), context.getSource().asPlayer()))
				.then(Commands.argument("player", EntityArgument.player())
						.requires(source -> source.hasPermissionLevel(2))
						.executes(context -> heal(context.getSource(), EntityArgument.getPlayer(context, "player")))
				)
		);

		dispatcher.register(Commands.literal("fly")
				.requires(source -> source.hasPermissionLevel(2))
				.executes(context -> fly(context.getSource(), context.getSource().asPlayer()))
				.then(Commands.argument("player", EntityArgument.player())
						.requires(source -> source.hasPermissionLevel(2))
						.executes(context -> fly(context.getSource(), EntityArgument.getPlayer(context, "player")))
				)
		);

		dispatcher.register(Commands.literal("god")
				.requires(source -> source.hasPermissionLevel(2))
				.executes(context -> god(context.getSource(), context.getSource().asPlayer()))
				.then(Commands.argument("player", EntityArgument.player())
						.requires(source -> source.hasPermissionLevel(2))
						.executes(context -> god(context.getSource(), EntityArgument.getPlayer(context, "player")))
				)
		);

		dispatcher.register(Commands.literal("invsee")
				.requires(source -> source.hasPermissionLevel(2))
				.then(Commands.argument("player", EntityArgument.player())
						.requires(source -> source.hasPermissionLevel(2))
						.executes(context -> invsee(context.getSource().asPlayer(), EntityArgument.getPlayer(context, "player")))
				)
		);

		dispatcher.register(Commands.literal("nickfor")
				.requires(source -> source.hasPermissionLevel(2))
				.then(Commands.argument("player", EntityArgument.player())
						.requires(source -> source.hasPermissionLevel(2))
						.executes(context -> nickfor(context.getSource(), EntityArgument.getPlayer(context, "player"), ""))
						.then(Commands.argument("nick", StringArgumentType.greedyString())
								.requires(source -> source.hasPermissionLevel(2))
								.executes(context -> nickfor(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "nick")))
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

	public static int heal(CommandSource source, ServerPlayerEntity player)
	{
		player.setHealth(player.getMaxHealth());
		player.getFoodStats().addStats(40, 40F);
		player.extinguish();
		return 1;
	}

	public static int fly(CommandSource source, ServerPlayerEntity player)
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

	public static int god(CommandSource source, ServerPlayerEntity player)
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
			public Container createMenu(int id, PlayerInventory playerInventory, PlayerEntity player)
			{
				return new ChestContainer(ContainerType.GENERIC_9X5, id, playerInventory, new OtherPlayerInventory((ServerPlayerEntity) player), 5);
			}
		});

		return 1;
	}

	public static int nickfor(CommandSource source, ServerPlayerEntity player, String nick)
	{
		source.sendFeedback(new StringTextComponent("WIP!"), false);
		return 1;
	}

	public static int mute(CommandSource source, ServerPlayerEntity player)
	{
		source.sendFeedback(new StringTextComponent("WIP!"), false);
		return 1;
	}

	public static int unmute(CommandSource source, ServerPlayerEntity player)
	{
		source.sendFeedback(new StringTextComponent("WIP!"), false);
		return 1;
	}
}
