package com.feed_the_beast.mods.ftbessentials.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;

/**
 * @author LatvianModder
 */
public class TPCommands
{
	public static void register(CommandDispatcher<CommandSource> dispatcher)
	{
		dispatcher.register(Commands.literal("back")
				.executes(context -> back(context.getSource().asPlayer()))
		);

		dispatcher.register(Commands.literal("spawn")
				.executes(context -> spawn(context.getSource().asPlayer()))
		);

		dispatcher.register(Commands.literal("rtp")
				.executes(context -> rtp(context.getSource().asPlayer()))
		);

		dispatcher.register(Commands.literal("teleport_last")
				.then(Commands.argument("player", GameProfileArgument.gameProfile())
						.executes(context -> tpLast(context.getSource().asPlayer(), GameProfileArgument.getGameProfiles(context, "name").iterator().next()))
				)
		);

		dispatcher.register(Commands.literal("tpa")
				.then(Commands.argument("to", EntityArgument.player())
						.executes(context -> tpa(context.getSource().asPlayer(), EntityArgument.getPlayer(context, "to")))
				)
		);

		dispatcher.register(Commands.literal("tpaccept")
				.then(Commands.argument("from", EntityArgument.player())
						.executes(context -> tpaccept(context.getSource().asPlayer(), EntityArgument.getPlayer(context, "from")))
				)
		);

		dispatcher.register(Commands.literal("tpdeny")
				.then(Commands.argument("from", EntityArgument.player())
						.executes(context -> tpdeny(context.getSource().asPlayer(), EntityArgument.getPlayer(context, "from")))
				)
		);
	}

	public static int back(ServerPlayerEntity player)
	{
		player.sendMessage(new StringTextComponent("WIP!"), Util.DUMMY_UUID);
		return 1;
	}

	public static int spawn(ServerPlayerEntity player)
	{
		player.sendMessage(new StringTextComponent("WIP!"), Util.DUMMY_UUID);
		return 1;
	}

	public static int rtp(ServerPlayerEntity player)
	{
		player.sendMessage(new StringTextComponent("WIP!"), Util.DUMMY_UUID);
		return 1;
	}

	public static int tpLast(ServerPlayerEntity player, GameProfile to)
	{
		player.sendMessage(new StringTextComponent("WIP!"), Util.DUMMY_UUID);
		return 1;
	}

	public static int tpa(ServerPlayerEntity player, ServerPlayerEntity to)
	{
		player.sendMessage(new StringTextComponent("WIP!"), Util.DUMMY_UUID);
		return 1;
	}

	public static int tpaccept(ServerPlayerEntity player, ServerPlayerEntity from)
	{
		player.sendMessage(new StringTextComponent("WIP!"), Util.DUMMY_UUID);
		return 1;
	}

	public static int tpdeny(ServerPlayerEntity player, ServerPlayerEntity from)
	{
		player.sendMessage(new StringTextComponent("WIP!"), Util.DUMMY_UUID);
		return 1;
	}
}
