package com.feed_the_beast.mods.ftbessentials.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;

/**
 * @author LatvianModder
 */
public class WarpCommands
{
	public static void register(CommandDispatcher<CommandSource> dispatcher)
	{
		dispatcher.register(Commands.literal("warp")
				.then(Commands.argument("name", StringArgumentType.greedyString())
						.executes(context -> warp(context.getSource().asPlayer(), StringArgumentType.getString(context, "name")))
				)
		);

		dispatcher.register(Commands.literal("setwarp")
				.then(Commands.argument("name", StringArgumentType.greedyString())
						.executes(context -> setwarp(context.getSource().asPlayer(), StringArgumentType.getString(context, "name")))
				)
		);

		dispatcher.register(Commands.literal("delwarp")
				.then(Commands.argument("name", StringArgumentType.greedyString())
						.executes(context -> delwarp(context.getSource().asPlayer(), StringArgumentType.getString(context, "name")))
				)
		);

		dispatcher.register(Commands.literal("listwarps")
				.executes(context -> listwarps(context.getSource().asPlayer(), context.getSource().asPlayer().getGameProfile()))
		);
	}

	public static int warp(ServerPlayerEntity player, String name)
	{
		player.sendStatusMessage(new StringTextComponent("WIP!"), false);
		return 1;
	}

	public static int setwarp(ServerPlayerEntity player, String name)
	{
		player.sendStatusMessage(new StringTextComponent("WIP!"), false);
		return 1;
	}

	public static int delwarp(ServerPlayerEntity player, String name)
	{
		player.sendStatusMessage(new StringTextComponent("WIP!"), false);
		return 1;
	}

	public static int listwarps(ServerPlayerEntity player, GameProfile of)
	{
		player.sendStatusMessage(new StringTextComponent("WIP!"), false);
		return 1;
	}
}
