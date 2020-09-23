package com.feed_the_beast.mods.ftbessentials.command;

import com.feed_the_beast.mods.ftbessentials.util.FTBEPlayerData;
import com.feed_the_beast.mods.ftbessentials.util.TeleportPos;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;

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
						.executes(context -> tpLast(context.getSource().asPlayer(), GameProfileArgument.getGameProfiles(context, "player").iterator().next()))
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
		FTBEPlayerData data = FTBEPlayerData.get(player);

		if (data.teleportHistory.isEmpty())
		{
			player.sendStatusMessage(new StringTextComponent("Teleportation history is empty!"), false);
			return 0;
		}

		if (data.backTeleporter.teleport(player, serverPlayerEntity -> data.teleportHistory.getLast()))
		{
			data.teleportHistory.removeLast();
			data.save();
		}

		return 1;
	}

	public static int spawn(ServerPlayerEntity player)
	{
		FTBEPlayerData data = FTBEPlayerData.get(player);
		ServerWorld w = player.server.getWorld(World.OVERWORLD);

		if (w == null)
		{
			return 0;
		}

		return data.spawnTeleporter.teleport(player, p -> new TeleportPos(w, w.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, w.getSpawnPoint()))) ? 1 : 0;
	}

	public static int rtp(ServerPlayerEntity player)
	{
		FTBEPlayerData data = FTBEPlayerData.get(player);
		player.sendStatusMessage(new StringTextComponent("WIP!"), false);
		return 1;
	}

	public static int tpLast(ServerPlayerEntity player, GameProfile to)
	{
		ServerPlayerEntity p = player.server.getPlayerList().getPlayerByUUID(to.getId());

		if (p != null)
		{
			new TeleportPos(p).teleport(player);
			return 1;
		}

		FTBEPlayerData dataTo = FTBEPlayerData.get(to);
		dataTo.lastSeen.teleport(player);
		return 1;
	}

	public static int tpa(ServerPlayerEntity player, ServerPlayerEntity to)
	{
		FTBEPlayerData data = FTBEPlayerData.get(player);
		FTBEPlayerData dataTo = FTBEPlayerData.get(to);
		player.sendStatusMessage(new StringTextComponent("WIP!"), false);
		return 1;
	}

	public static int tpaccept(ServerPlayerEntity player, ServerPlayerEntity from)
	{
		FTBEPlayerData data = FTBEPlayerData.get(player);
		FTBEPlayerData dataFrom = FTBEPlayerData.get(from);
		player.sendStatusMessage(new StringTextComponent("WIP!"), false);
		return 1;
	}

	public static int tpdeny(ServerPlayerEntity player, ServerPlayerEntity from)
	{
		FTBEPlayerData data = FTBEPlayerData.get(player);
		FTBEPlayerData dataFrom = FTBEPlayerData.get(from);
		player.sendStatusMessage(new StringTextComponent("WIP!"), false);
		return 1;
	}
}
