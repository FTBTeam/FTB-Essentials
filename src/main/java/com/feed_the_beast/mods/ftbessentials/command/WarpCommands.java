package com.feed_the_beast.mods.ftbessentials.command;

import com.feed_the_beast.mods.ftbessentials.util.FTBEPlayerData;
import com.feed_the_beast.mods.ftbessentials.util.FTBEWorldData;
import com.feed_the_beast.mods.ftbessentials.util.TeleportPos;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;

import java.util.Map;

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
				.executes(context -> listwarps(context.getSource()))
		);
	}

	public static int warp(ServerPlayerEntity player, String name)
	{
		player.sendStatusMessage(new StringTextComponent("WIP!"), false);
		FTBEPlayerData data = FTBEPlayerData.get(player);
		TeleportPos pos = FTBEWorldData.instance.warps.get(name.toLowerCase());

		if (pos == null)
		{
			player.sendStatusMessage(new StringTextComponent("Warp not found!"), false);
			return 0;
		}

		return data.warpTeleporter.teleport(player, p -> pos).runCommand(player);
	}

	public static int setwarp(ServerPlayerEntity player, String name)
	{
		FTBEWorldData.instance.warps.put(name.toLowerCase(), new TeleportPos(player));
		FTBEWorldData.instance.save();
		player.sendStatusMessage(new StringTextComponent("Warp set!"), false);
		return 1;
	}

	public static int delwarp(ServerPlayerEntity player, String name)
	{
		if (FTBEWorldData.instance.warps.remove(name.toLowerCase()) != null)
		{
			FTBEWorldData.instance.save();
			player.sendStatusMessage(new StringTextComponent("Warp deleted!"), false);
			return 1;
		}
		else
		{
			player.sendStatusMessage(new StringTextComponent("Warp not found!"), false);
			return 0;
		}
	}

	public static int listwarps(CommandSource source)
	{
		if (FTBEWorldData.instance.warps.isEmpty())
		{
			source.sendFeedback(new StringTextComponent("None"), false);
			return 1;
		}

		TeleportPos origin = new TeleportPos(source.getWorld().getDimensionKey(), new BlockPos(source.getPos()));

		for (Map.Entry<String, TeleportPos> entry : FTBEWorldData.instance.warps.entrySet())
		{
			source.sendFeedback(new StringTextComponent(entry.getKey() + ": " + entry.getValue().distanceString(origin)), false);
		}

		return 1;
	}
}
