package dev.ftb.mods.ftbessentials.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.FTBEssentialsEvents;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.BlockUtil;
import dev.ftb.mods.ftbessentials.util.DimensionFilter;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * @author LatvianModder
 */
public class TeleportCommands {
	public static final TagKey<Block> IGNORE_RTP_BLOCKS = TagKey.create(Registries.BLOCK, new ResourceLocation(FTBEssentials.MOD_ID, "ignore_rtp"));
	public static final TagKey<Biome> IGNORE_RTP_BIOMES = TagKey.create(Registries.BIOME, new ResourceLocation(FTBEssentials.MOD_ID, "ignore_rtp"));

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

		if (FTBEConfig.JUMP.isEnabled()) {
			dispatcher.register(Commands.literal("jump")
					.requires(FTBEConfig.JUMP.enabledAndOp())
					.executes(ctx -> jump(ctx.getSource()))
			);
		}
	}

	private static int jump(CommandSourceStack source) {
		try {
			ServerPlayer player = source.getPlayerOrException();

			BlockHitResult res = BlockUtil.getFocusedBlock(player, player.getServer().getPlayerList().getViewDistance() * 16)
					.orElseThrow(() -> new IllegalArgumentException("Not looking at a block"));
			// want to land the player on top of the focused block, so scan up as far as needed
			BlockPos.MutableBlockPos mPos = res.getBlockPos().above().mutable();
			while (true) {
				Level level = player.level();
				if (isEmptyShape(level, mPos.above()) && isEmptyShape(level, mPos.above(2)) || mPos.getY() >= level.getMaxBuildHeight())
					break;
				mPos.move(Direction.UP, 2);
			}
			Vec3 vec = Vec3.atBottomCenterOf(mPos);
			player.teleportTo(vec.x(), vec.y(), vec.z());
		} catch (Exception e) {
			source.sendFailure(Component.literal("Can't jump: " + e.getMessage()));
		}
		return 0;
	}

	private static boolean isEmptyShape(Level level, BlockPos pos) {
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	public static int back(ServerPlayer player) {
		return FTBEPlayerData.getOrCreate(player).map(data -> {
			if (data.teleportHistory.isEmpty()) {
				player.displayClientMessage(Component.literal("Teleportation history is empty!").withStyle(ChatFormatting.RED), false);
				return 0;
			}

			if (data.backTeleporter.teleport(player, serverPlayerEntity -> data.teleportHistory.getLast()).runCommand(player) != 0) {
				data.markDirty();
				return 1;
			}

			return 0;
		}).orElse(0);
	}

	public static int spawn(ServerPlayer player) {
		return FTBEPlayerData.getOrCreate(player).map(data -> {
			ServerLevel level = player.server.getLevel(Level.OVERWORLD);
			return level == null ? 0 : data.spawnTeleporter.teleport(player, p -> new TeleportPos(level, level.getSharedSpawnPos(), level.getSharedSpawnAngle(), 0F)).runCommand(player);
		}).orElse(0);
	}

	public static int rtp(ServerPlayer player) {
		if (!player.hasPermissions(2) && !DimensionFilter.isDimensionOK(player.level().dimension())) {
			player.displayClientMessage(Component.literal("You may not use /rtp in this dimension!").withStyle(ChatFormatting.RED), false);
			return 0;
		}
		return FTBEPlayerData.getOrCreate(player).map(data -> data.rtpTeleporter.teleport(player, p -> {
					p.displayClientMessage(Component.literal("Looking for random location..."), false);
					return findBlockPos((ServerLevel) player.level(), p);
				}).runCommand(player))
				.orElse(0);
	}

	private static TeleportPos findBlockPos(ServerLevel world, ServerPlayer player) {
		for (int attempt = 0; attempt < FTBEConfig.RTP_MAX_TRIES.get(); attempt++) {
			double dist = FTBEConfig.RTP_MIN_DISTANCE.get() + world.random.nextDouble() * (FTBEConfig.RTP_MAX_DISTANCE.get() - FTBEConfig.RTP_MIN_DISTANCE.get());
			double angle = world.random.nextDouble() * Math.PI * 2D;

			int x = Mth.floor(Math.cos(angle) * dist);
			int y = 256;
			int z = Mth.floor(Math.sin(angle) * dist);
			BlockPos currentPos = new BlockPos(x, y, z);

			if (!world.getWorldBorder().isWithinBounds(currentPos)) {
				continue;
			}
			if (world.getBiome(currentPos).is(IGNORE_RTP_BIOMES)) {
				continue;
			}
			// TODO: FTB Chunks will listen to RTPEvent and cancel it if position is inside a claimed chunk
			EventResult res = FTBEssentialsEvents.RTP_EVENT.invoker().teleport(world, player, currentPos, attempt);
			if (res.isFalse()) {
				continue;
			}

			world.getChunkAt(currentPos);
			BlockPos hmPos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, currentPos);

			if (hmPos.getY() > 0) {
				BlockPos goodPos = null;
				if (hmPos.getY() < world.getLogicalHeight()) {
					goodPos = hmPos;
				} else {
					// broken heightmap (nether, other mod dimensions)
					for (BlockPos newPos : BlockPos.spiralAround(new BlockPos(hmPos.getX(), world.getSeaLevel(), hmPos.getY()), 16, Direction.EAST, Direction.SOUTH)) {
						BlockState bs = world.getBlockState(newPos);
						if (bs.blocksMotion() && !bs.is(IGNORE_RTP_BLOCKS) && world.isEmptyBlock(newPos.above(1))
								&& world.isEmptyBlock(newPos.above(2)) && world.isEmptyBlock(newPos.above(3))) {
							goodPos = newPos;
						}
					}
				}
				if (goodPos != null) {
					player.displayClientMessage(Component.literal(String.format("Found good location after %d " + (attempt == 1 ? "attempt" : "attempts") + " @ [x %d, z %d]", attempt, goodPos.getX(), goodPos.getZ())), false);
					return new TeleportPos(world.dimension(), goodPos.above());
				}
			}
		}
		player.displayClientMessage(Component.literal("Could not find a valid location to teleport to!").withStyle(ChatFormatting.RED), false);
		return new TeleportPos(player);
	}

	public static int tpLast(ServerPlayer player, GameProfile to) {
		ServerPlayer toPlayer = player.server.getPlayerList().getPlayer(to.getId());
		if (toPlayer != null) {
			FTBEPlayerData.addTeleportHistory(player);
			new TeleportPos(toPlayer).teleport(player);
			return 1;
		}
		// dest player not online; teleport to where they were last seen
		return FTBEPlayerData.getOrCreate(to).map(dataTo -> {
			FTBEPlayerData.addTeleportHistory(player);
			dataTo.getLastSeenPos().teleport(player);
			return 1;
		}).orElse(0);
	}

	public static int tpx(ServerPlayer player, ServerLevel to) {
		player.teleportTo(to, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
		return 1;
	}
}
