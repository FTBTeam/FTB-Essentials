package dev.ftb.mods.ftbessentials.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.RTPEvent;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;

/**
 * @author LatvianModder
 */
public class TeleportCommands {
	public static final TagKey<Block> IGNORE_RTP = TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation(FTBEssentials.MOD_ID, "ignore_rtp"));

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		if (FTBEConfig.BACK.isEnabled()) {
			dispatcher.register(Commands.literal("back")
					.requires(FTBEConfig.BACK)
					.executes(context -> back(context.getSource().getPlayerOrException()))
			);
		}

		if (FTBEConfig.SPAWN.isEnabled()) {
			dispatcher.register(Commands.literal("spawn")
					.requires(FTBEConfig.SPAWN)
					.executes(context -> spawn(context.getSource().getPlayerOrException()))
			);
		}

		if (FTBEConfig.RTP.isEnabled()) {
			dispatcher.register(Commands.literal("rtp")
					.requires(FTBEConfig.RTP)
					.executes(context -> rtp(context.getSource().getPlayerOrException()))
			);
		}

		if (FTBEConfig.TPL.isEnabled()) {
			dispatcher.register(Commands.literal("teleport_last")
					.requires(FTBEConfig.TPL.enabledAndOp())
					.then(Commands.argument("player", GameProfileArgument.gameProfile())
							.executes(context -> tpLast(context.getSource().getPlayerOrException(), GameProfileArgument.getGameProfiles(context, "player").iterator().next()))
					)
			);
		}

		if (FTBEConfig.TPX.isEnabled()) {
			dispatcher.register(Commands.literal("tpx")
					.requires(FTBEConfig.TPX.enabledAndOp())
					.then(Commands.argument("dimension", DimensionArgument.dimension())
							.executes(context -> tpx(context.getSource().getPlayerOrException(), DimensionArgument.getDimension(context, "dimension")))
					)
			);
		}
	}

	public static int back(ServerPlayer player) {
		FTBEPlayerData data = FTBEPlayerData.get(player);

		if (data.teleportHistory.isEmpty()) {
			player.displayClientMessage(new TranslatableComponent("tp_command_message.ftbessentials.tp_history_empty").withStyle(ChatFormatting.RED), false);
			return 0;
		}

		if (data.backTeleporter.teleport(player, serverPlayerEntity -> data.teleportHistory.getLast()).runCommand(player) != 0) {
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

		return data.spawnTeleporter.teleport(player, p -> new TeleportPos(w, w.getSharedSpawnPos(), w.getSharedSpawnAngle(), 0F)).runCommand(player);
	}

	public static int rtp(ServerPlayer player) {
		FTBEPlayerData data = FTBEPlayerData.get(player);
		return data.rtpTeleporter.teleport(player, p -> {
			p.displayClientMessage(new TranslatableComponent("tp_command_message.ftbessentials.rtp_process"), false);
			return findBlockPos(player.getLevel(), p, 1);
		}).runCommand(player);
	}

	private static TeleportPos findBlockPos(ServerLevel world, ServerPlayer player, int attempt) {
		if (attempt > FTBEConfig.RTP_MAX_TRIES.get()) {
			player.displayClientMessage(new TranslatableComponent("tp_command_message.ftbessentials.rtp_fail").withStyle(ChatFormatting.RED), false);
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

		Holder<Biome> biomeKey = world.getBiome(currentPos);

		if (biomeKey.unwrapKey().isPresent() && biomeKey.unwrapKey().get().location().getPath().contains("ocean")) {
			return findBlockPos(world, player, attempt + 1);
		}

		// TODO: FTB Chunks will listen to RTPEvent and cancel it if position is inside a claimed chunk
		if (MinecraftForge.EVENT_BUS.post(new RTPEvent(world, player, currentPos, attempt))) {
			return findBlockPos(world, player, attempt + 1);
		}

		world.getChunk(currentPos.getX() >> 4, currentPos.getZ() >> 4, ChunkStatus.HEIGHTMAPS);
		BlockPos hmPos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, currentPos);

		if (hmPos.getY() > 0) {
			if (hmPos.getY() >= world.getLogicalHeight()) { // broken heightmap (nether, other mod dimensions)
				for (BlockPos newPos : BlockPos.spiralAround(new BlockPos(hmPos.getX(), world.getSeaLevel(), hmPos.getY()), 16, Direction.EAST, Direction.SOUTH)) {
					BlockState bs = world.getBlockState(newPos);

					if (bs.getMaterial().isSolidBlocking() && !bs.is(IGNORE_RTP) && world.isEmptyBlock(newPos.above(1)) && world.isEmptyBlock(newPos.above(2)) && world.isEmptyBlock(newPos.above(3))) {
						player.displayClientMessage(new TextComponent(String.format(new TranslatableComponent("tp_command_message.ftbessentials.rtp_result").getString()+" %d " + (attempt == 1 ? new TranslatableComponent("tp_command_message.ftbessentials.rtp_result_attempt").getString() : new TranslatableComponent("tp_command_message.ftbessentials.rtp_result_attempts").getString()) + " @ [x %d, z %d]", attempt, newPos.getX(), newPos.getZ())), false);
						return new TeleportPos(world.dimension(), newPos.above());
					}
				}
			} else {
				player.displayClientMessage(new TextComponent(String.format(new TranslatableComponent("tp_command_message.ftbessentials.rtp_result").getString()+" %d " + (attempt == 1 ? new TranslatableComponent("tp_command_message.ftbessentials.rtp_result_attempt").getString() : new TranslatableComponent("tp_command_message.ftbessentials.rtp_result_attempts").getString()) + " @ [x %d, z %d]", attempt, hmPos.getX(), hmPos.getZ())), false);
				return new TeleportPos(world.dimension(), hmPos.above());
			}
		}

		return findBlockPos(world, player, attempt + 1);
	}

	public static int tpLast(ServerPlayer player, GameProfile to) {
		ServerPlayer p = player.server.getPlayerList().getPlayer(to.getId());

		if (p != null) {
			FTBEPlayerData.addTeleportHistory(player);
			new TeleportPos(p).teleport(player);
			return 1;
		}

		FTBEPlayerData dataTo = FTBEPlayerData.get(to);

		if (dataTo == null) {
			return 0;
		}

		FTBEPlayerData.addTeleportHistory(player);
		dataTo.lastSeen.teleport(player);

		return 1;
	}

	public static int tpx(ServerPlayer player, ServerLevel to) {
		player.teleportTo(to, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
		return 1;
	}
}
