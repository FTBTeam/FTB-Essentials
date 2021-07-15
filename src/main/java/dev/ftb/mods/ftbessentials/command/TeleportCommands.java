package dev.ftb.mods.ftbessentials.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.RTPEvent;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;

import java.util.Optional;

/**
 * @author LatvianModder
 */
public class TeleportCommands {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		if (FTBEConfig.BACK.isEnabled()) {
			dispatcher.register(Commands.literal("back")
					.executes(context -> back(context.getSource().getPlayerOrException()))
			);
		}

		if (FTBEConfig.SPAWN.isEnabled()) {
			dispatcher.register(Commands.literal("spawn")
					.executes(context -> spawn(context.getSource().getPlayerOrException()))
			);
		}

		if (FTBEConfig.RTP.isEnabled()) {
			dispatcher.register(Commands.literal("rtp")
					.executes(context -> rtp(context.getSource().getPlayerOrException()))
			);
		}

		if (FTBEConfig.TPL.isEnabled()) {
			dispatcher.register(Commands.literal("teleport_last")
					.requires(source -> source.hasPermission(2))
					.then(Commands.argument("player", GameProfileArgument.gameProfile())
							.executes(context -> tpLast(context.getSource().getPlayerOrException(), GameProfileArgument.getGameProfiles(context, "player").iterator().next()))
					)
			);
		}
	}

	public static int back(ServerPlayer player) {
		FTBEPlayerData data = FTBEPlayerData.get(player);

		if (data.teleportHistory.isEmpty()) {
			player.displayClientMessage(new TextComponent("Teleportation history is empty!"), false);
			return 0;
		}

		if (data.backTeleporter.teleport(player, serverPlayerEntity -> data.teleportHistory.getLast()).runCommand(player) != 0) {
			data.teleportHistory.removeLast();
			data.save();
			return 1;
		}

		return 0;
	}

	public static int spawn(ServerPlayer player) {
		FTBEPlayerData data = FTBEPlayerData.get(player);
		ServerLevel w = player.server.getLevel(Level.OVERWORLD);

		if (w == null) {
			return 0;
		}

		return data.spawnTeleporter.teleport(player, p -> new TeleportPos(w, w.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, w.getSharedSpawnPos()))).runCommand(player);
	}

	public static int rtp(ServerPlayer player) {
		FTBEPlayerData data = FTBEPlayerData.get(player);
		return data.rtpTeleporter.teleport(player, p -> {
			p.displayClientMessage(new TextComponent("Looking for random location..."), false);
			return findBlockPos(p.server.getLevel(Level.OVERWORLD), p, 1);
		}).runCommand(player);
	}

	private static TeleportPos findBlockPos(ServerLevel world, ServerPlayer player, int attempt) {
		if (attempt > FTBEConfig.RTP_MAX_TRIES.get()) {
			player.displayClientMessage(new TextComponent("Could not find a valid location to teleport to!"), false);
			return new TeleportPos(player);
		}

		double dist = FTBEConfig.RTP_MIN_DISTANCE.get() + world.random.nextDouble() * (FTBEConfig.RTP_MAX_DISTANCE.get() - FTBEConfig.RTP_MIN_DISTANCE.get());
		double angle = world.random.nextDouble() * Math.PI * 2D;

		int x = Mth.floor(Math.cos(angle) * dist);
		int y = 256;
		int z = Mth.floor(Math.sin(angle) * dist);
		BlockPos currentPos = new BlockPos(x, y, z);

		WorldBorder border = world.getWorldBorder();

		if (!border.isWithinBounds(currentPos)) {
			return findBlockPos(world, player, attempt + 1);
		}

		Optional<ResourceKey<Biome>> biomeKey = world.getBiomeName(currentPos);

		if (biomeKey.isPresent() && biomeKey.get().location().getPath().contains("ocean")) {
			return findBlockPos(world, player, attempt + 1);
		}

		// TODO: FTB Chunks will listen to RTPEvent and cancel it if position is inside a claimed chunk
		if (MinecraftForge.EVENT_BUS.post(new RTPEvent(world, player, currentPos, attempt))) {
			return findBlockPos(world, player, attempt + 1);
		}

		world.getChunk(currentPos.getX() >> 4, currentPos.getZ() >> 4, ChunkStatus.HEIGHTMAPS);
		BlockPos newPos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, currentPos);

		if (newPos.getY() > 0) {
			player.displayClientMessage(new TextComponent(String.format("Found good location after %d " + (attempt == 1 ? "attempt" : "attempts") + " @ [x %d, z %d]", attempt, newPos.getX(), newPos.getZ())), false);
			return new TeleportPos(world.dimension(), newPos.above());
		}

		return findBlockPos(world, player, attempt + 1);
	}

	public static int tpLast(ServerPlayer player, GameProfile to) {
		ServerPlayer p = player.server.getPlayerList().getPlayer(to.getId());

		if (p != null) {
			new TeleportPos(p).teleport(player);
			return 1;
		}

		FTBEPlayerData dataTo = FTBEPlayerData.get(to);
		dataTo.lastSeen.teleport(player);
		return 1;
	}
}
