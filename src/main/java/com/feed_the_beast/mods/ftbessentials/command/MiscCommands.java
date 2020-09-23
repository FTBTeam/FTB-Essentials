package com.feed_the_beast.mods.ftbessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

/**
 * @author LatvianModder
 */
public class MiscCommands
{
	public static void register(CommandDispatcher<CommandSource> dispatcher)
	{
		dispatcher.register(Commands.literal("kickme")
				.executes(context -> kickme(context.getSource().asPlayer()))
		);

		dispatcher.register(Commands.literal("trashcan")
				.executes(context -> trashcan(context.getSource().asPlayer()))
		);

		dispatcher.register(Commands.literal("leaderboard")
				.executes(context -> leaderboard(context.getSource().asPlayer()))
		);

		dispatcher.register(Commands.literal("rec")
				.executes(context -> rec(context.getSource().asPlayer()))
		);

		dispatcher.register(Commands.literal("nick")
				.executes(context -> nick(context.getSource().asPlayer(), ""))
				.then(Commands.argument("nick", StringArgumentType.greedyString())
						.executes(context -> nick(context.getSource().asPlayer(), StringArgumentType.getString(context, "nick")))
				)
		);
	}

	public static int kickme(ServerPlayerEntity player)
	{
		player.sendMessage(new StringTextComponent("WIP!"), Util.DUMMY_UUID);
		return 1;
	}

	public static int trashcan(ServerPlayerEntity player)
	{
		player.openContainer(new INamedContainerProvider()
		{
			@Override
			public ITextComponent getDisplayName()
			{
				return new StringTextComponent("Trash Can");
			}

			@Override
			public Container createMenu(int id, PlayerInventory playerInventory, PlayerEntity player)
			{
				return ChestContainer.createGeneric9X4(id, playerInventory);
			}
		});

		return 1;
	}

	public static int leaderboard(ServerPlayerEntity player)
	{
		player.sendMessage(new StringTextComponent("WIP!"), Util.DUMMY_UUID);
		return 1;
	}

	public static int rec(ServerPlayerEntity player)
	{
		player.sendMessage(new StringTextComponent("WIP!"), Util.DUMMY_UUID);
		return 1;
	}

	public static int nick(ServerPlayerEntity player, String nick)
	{
		player.sendMessage(new StringTextComponent("WIP!"), Util.DUMMY_UUID);
		return 1;
	}
}
