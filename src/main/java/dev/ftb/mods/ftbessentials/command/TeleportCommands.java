package dev.ftb.mods.ftbessentials.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import dev.ftb.mods.ftbessentials.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.RTPEvent;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;

import java.util.Optional;

/**
 * @author LatvianModder
 */
public class TeleportCommands {
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
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
				.requires(source -> source.hasPermissionLevel(2))
				.then(Commands.argument("player", GameProfileArgument.gameProfile())
						.executes(context -> tpLast(context.getSource().asPlayer(), GameProfileArgument.getGameProfiles(context, "player").iterator().next()))
				)
		);
	}

	public static int back(ServerPlayerEntity player) {
		FTBEPlayerData data = FTBEPlayerData.get(player);

		if (data.teleportHistory.isEmpty()) {
			player.sendStatusMessage(new StringTextComponent("Teleportation history is empty!"), false);
			return 0;
		}

		if (data.backTeleporter.teleport(player, serverPlayerEntity -> data.teleportHistory.getLast()).runCommand(player) != 0) {
			data.teleportHistory.removeLast();
			data.save();
			return 1;
		}

		return 0;
	}

	public static int spawn(ServerPlayerEntity player) {
		FTBEPlayerData data = FTBEPlayerData.get(player);
		ServerWorld w = player.server.getWorld(World.OVERWORLD);

		if (w == null) {
			return 0;
		}

		return data.spawnTeleporter.teleport(player, p -> new TeleportPos(w, w.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, w.getSpawnPoint()))).runCommand(player);
	}

	public static int rtp(ServerPlayerEntity player) {
		FTBEPlayerData data = FTBEPlayerData.get(player);
		return data.rtpTeleporter.teleport(player, p -> {
			p.sendStatusMessage(new StringTextComponent("Looking for random location..."), false);
			return findBlockPos(p.server.getWorld(World.OVERWORLD), p, 1);
		}).runCommand(player);
	}

	private static TeleportPos findBlockPos(ServerWorld world, ServerPlayerEntity player, int attempt) {
		if (attempt > FTBEConfig.rtpMaxTries) {
			player.sendStatusMessage(new StringTextComponent("Could not find a valid location to teleport to!"), false);
			return new TeleportPos(player);
		}

		double dist = FTBEConfig.rtpMinDistance + world.rand.nextDouble() * (FTBEConfig.rtpMaxDistance - FTBEConfig.rtpMinDistance);
		double angle = world.rand.nextDouble() * Math.PI * 2D;

		int x = MathHelper.floor(Math.cos(angle) * dist);
		int y = 256;
		int z = MathHelper.floor(Math.sin(angle) * dist);
		BlockPos currentPos = new BlockPos(x, y, z);

		WorldBorder border = world.getWorldBorder();

		if (!border.contains(currentPos)) {
			return findBlockPos(world, player, attempt + 1);
		}

		Optional<RegistryKey<Biome>> biomeKey = world.func_242406_i(currentPos);

		if (biomeKey.isPresent() && biomeKey.get().getLocation().getPath().contains("ocean")) {
			return findBlockPos(world, player, attempt + 1);
		}

		// TODO: FTB Chunks will listen to RTPEvent and cancel it if position is inside a claimed chunk
		if (MinecraftForge.EVENT_BUS.post(new RTPEvent(world, player, currentPos, attempt))) {
			return findBlockPos(world, player, attempt + 1);
		}

		world.getChunk(currentPos.getX() >> 4, currentPos.getZ() >> 4, ChunkStatus.HEIGHTMAPS);
		BlockPos newPos = world.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, currentPos);

		if (newPos.getY() > 0) {
			player.sendStatusMessage(new StringTextComponent(String.format("Found good location after %d " + (attempt == 1 ? "attempt" : "attempts") + " @ [x %d, z %d]", attempt, newPos.getX(), newPos.getZ())), false);
			return new TeleportPos(world.getDimensionKey(), newPos.up());
		}

		return findBlockPos(world, player, attempt + 1);
	}

	public static int tpLast(ServerPlayerEntity player, GameProfile to) {
		ServerPlayerEntity p = player.server.getPlayerList().getPlayerByUUID(to.getId());

		if (p != null) {
			new TeleportPos(p).teleport(player);
			return 1;
		}

		FTBEPlayerData dataTo = FTBEPlayerData.get(to);
		dataTo.lastSeen.teleport(player);
		return 1;
	}
}
