package com.feed_the_beast.mods.ftbessentials.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.player.ServerPlayerEntity;

/**
 * @author LatvianModder
 */
public class HomeCommands
{
	public static void register(CommandDispatcher<CommandSource> dispatcher)
	{
		dispatcher.register(Commands.literal("home")
				.executes(context -> home(context.getSource().asPlayer(), "home"))
				.then(Commands.argument("name", StringArgumentType.greedyString())
						.executes(context -> home(context.getSource().asPlayer(), StringArgumentType.getString(context, "name")))
				)
		);

		dispatcher.register(Commands.literal("sethome")
				.executes(context -> sethome(context.getSource().asPlayer(), "home"))
				.then(Commands.argument("name", StringArgumentType.greedyString())
						.executes(context -> sethome(context.getSource().asPlayer(), StringArgumentType.getString(context, "name")))
				)
		);

		dispatcher.register(Commands.literal("delhome")
				.executes(context -> delhome(context.getSource().asPlayer(), "home"))
				.then(Commands.argument("name", StringArgumentType.greedyString())
						.executes(context -> delhome(context.getSource().asPlayer(), StringArgumentType.getString(context, "name")))
				)
		);

		dispatcher.register(Commands.literal("listhomes")
				.executes(context -> listhomes(context.getSource().asPlayer(), context.getSource().asPlayer().getGameProfile()))
				.then(Commands.argument("player", GameProfileArgument.gameProfile())
						.requires(source -> source.hasPermissionLevel(2))
						.executes(context -> listhomes(context.getSource().asPlayer(), GameProfileArgument.getGameProfiles(context, "player").iterator().next()))
				)
		);
	}

	public static int home(ServerPlayerEntity player, String name)
	{
		return 1;
	}

	public static int sethome(ServerPlayerEntity player, String name)
	{
		return 1;
	}

	public static int delhome(ServerPlayerEntity player, String name)
	{
		return 1;
	}

	public static int listhomes(ServerPlayerEntity player, GameProfile of)
	{
		return 1;
	}
}
