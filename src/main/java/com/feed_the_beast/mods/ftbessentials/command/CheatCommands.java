package com.feed_the_beast.mods.ftbessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Util;
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
		source.sendFeedback(new StringTextComponent("WIP!"), false);
		return 1;
	}

	public static int fly(CommandSource source, ServerPlayerEntity player)
	{
		source.sendFeedback(new StringTextComponent("WIP!"), false);
		return 1;
	}

	public static int god(CommandSource source, ServerPlayerEntity player)
	{
		source.sendFeedback(new StringTextComponent("WIP!"), false);
		return 1;
	}

	public static int invsee(ServerPlayerEntity source, ServerPlayerEntity player)
	{
		source.sendMessage(new StringTextComponent("WIP!"), Util.DUMMY_UUID);
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
