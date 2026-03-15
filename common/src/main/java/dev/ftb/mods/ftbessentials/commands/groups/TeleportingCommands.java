package dev.ftb.mods.ftbessentials.commands.groups;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.api.event.RTPEvent;
import dev.ftb.mods.ftbessentials.commands.CommandUtils;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.commands.SimpleConfigurableCommand;
import dev.ftb.mods.ftbessentials.commands.impl.kit.KitCommand;
import dev.ftb.mods.ftbessentials.commands.impl.teleporting.HomeCommand;
import dev.ftb.mods.ftbessentials.commands.impl.teleporting.OfflineTeleportCommand;
import dev.ftb.mods.ftbessentials.commands.impl.teleporting.TPACommand;
import dev.ftb.mods.ftbessentials.commands.impl.teleporting.WarpCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.BlockUtil;
import dev.ftb.mods.ftbessentials.util.DimensionFilter;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import dev.ftb.mods.ftblibrary.platform.event.EventPostingHandler;
import dev.ftb.mods.ftblibrary.util.result.Outcome;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TeleportingCommands {
    public static final TagKey<Block> IGNORE_RTP_BLOCKS = TagKey.create(Registries.BLOCK, FTBEssentials.essentialsId("ignore_rtp"));
    public static final TagKey<Biome> IGNORE_RTP_BIOMES = TagKey.create(Registries.BIOME, FTBEssentials.essentialsId("ignore_rtp"));

    public static final List<FTBCommand> COMMANDS = List.of(
            new OfflineTeleportCommand(),
            new HomeCommand(),
            new WarpCommand(),
            new TPACommand(),

            // General teleport commands
            // Back command
            new SimpleConfigurableCommand(FTBEConfig.BACK, Commands.literal("back")
                    .executes(context -> back(context.getSource().getPlayerOrException()))),

            // Playerspawn command
            new SimpleConfigurableCommand(FTBEConfig.PLAYER_SPAWN, Commands.literal("playerspawn")
                    .executes(context -> playerSpawn(context.getSource().getPlayerOrException()))),

            // Spawn command
            new SimpleConfigurableCommand(FTBEConfig.SPAWN, Commands.literal("spawn")
                    .executes(context -> spawn(context.getSource().getPlayerOrException()))),

            // Random teleport command
            new SimpleConfigurableCommand(FTBEConfig.RTP, Commands.literal("rtp")
                    .then(Commands.argument("maxDistance", IntegerArgumentType.integer(FTBEConfig.RTP_MIN_DISTANCE.get(), FTBEConfig.RTP_MAX_DISTANCE.get()))
                            .requires(context -> FTBEConfig.RTP_MAX_DISTANCE_CUSTOM.get(context.getPlayer()))
                            .executes(context -> rtp(context.getSource().getPlayerOrException(), FTBEConfig.RTP_MIN_DISTANCE.get(), IntegerArgumentType.getInteger(context, "maxDistance")))
                    )
                    .then(Commands.argument("minDistance", IntegerArgumentType.integer(0, FTBEConfig.RTP_MAX_DISTANCE.get()))
                            .requires(context -> FTBEConfig.RTP_MIN_DISTANCE_CUSTOM.get(context.getPlayer()))
                            .then(Commands.argument("maxDistance", IntegerArgumentType.integer(0, FTBEConfig.RTP_MAX_DISTANCE.get()))
                                    .executes(context -> rtp(context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "minDistance"), IntegerArgumentType.getInteger(context, "maxDistance")))
                            )
                    )
                    .executes(context -> rtp(context.getSource().getPlayerOrException(), FTBEConfig.RTP_MIN_DISTANCE.get(), FTBEConfig.RTP_MAX_DISTANCE.get()))),

            // Teleport to the last location of a player
            new SimpleConfigurableCommand(FTBEConfig.TPL, Commands.literal("teleport_last")
                    .requires(CommandUtils.isGamemaster())
                    .then(Commands.argument("player", GameProfileArgument.gameProfile())
                            .executes(context -> tpLast(context.getSource().getPlayerOrException(), GameProfileArgument.getGameProfiles(context, "player").iterator().next()))
                    )),

            // Teleport to a specific dimension
            new SimpleConfigurableCommand(FTBEConfig.TPX, Commands.literal("tpx")
                    .requires(CommandUtils.isGamemaster())
                    .then(Commands.argument("dimension", DimensionArgument.dimension())
                            .executes(context -> tpx(context.getSource().getPlayerOrException(), DimensionArgument.getDimension(context, "dimension")))
                    )),

            // Jump to command, allows you to jump to the top of the block you're looking at
            new SimpleConfigurableCommand(FTBEConfig.JUMP, Commands.literal("jump")
                    .requires(CommandUtils.isGamemaster())
                    .executes(ctx -> jump(ctx.getSource())))
    );

    public static void register() {
    }

    private static int back(ServerPlayer player) {
        return FTBEPlayerData.getOrCreate(player).map(data -> {
            if (data.teleportHistory.isEmpty()) {
                player.sendSystemMessage(Component.translatable("ftbessentials.teleport.history_empty").withStyle(ChatFormatting.RED));
                return 0;
            }

            if (data.backTeleporter.teleport(player, serverPlayerEntity -> data.teleportHistory.getLast().safeForPlayer(player)).runCommand(player) != 0) {
                data.markDirty();
                return 1;
            }

            return 0;
        }).orElse(0);
    }

    public static int playerSpawn(ServerPlayer player) {
        return FTBEPlayerData.getOrCreate(player).map(data -> {
            var respawnConfig = player.getRespawnConfig();
            if (respawnConfig != null) {
                ServerLevel level = player.level().getServer().getLevel(respawnConfig.respawnData().dimension());
                if (level == null) {
                    return 0;
                }
                BlockPos pos = Objects.requireNonNullElse(respawnConfig.respawnData().pos(), level.getRespawnData().pos());
                return data.spawnTeleporter.teleport(player, p -> new TeleportPos(level, pos, respawnConfig.respawnData().yaw(), 0F)).runCommand(player);
            }
            return 0;
        }).orElse(0);
    }

    private static int spawn(ServerPlayer player) {
        return FTBEPlayerData.getOrCreate(player).map(data -> {
            ServerLevel level = player.level().getServer().getLevel(Level.OVERWORLD);
            return level == null ? 0 : data.spawnTeleporter.teleport(player, p -> new TeleportPos(level, level.getRespawnData().pos(), level.getRespawnData().yaw(), 0F)).runCommand(player);
        }).orElse(0);
    }

    //#region RTP
    private static int rtp(ServerPlayer player, int minDistance, int maxDistance) {
        if (maxDistance < minDistance) {
            player.sendSystemMessage(Component.translatable("ftbessentials.teleport.max_less_than_min"));
            return 0;
        }
        if ((!player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) || !FTBEConfig.ADMINS_EXEMPT_DIMENSION_BLACKLISTS.get())
                && !DimensionFilter.isRtpDimensionOK(player.level().dimension())) {
            player.sendSystemMessage(Component.translatable("ftbessentials.rtp.not_here").withStyle(ChatFormatting.RED));
            return 0;
        }
        return FTBEPlayerData.getOrCreate(player).map(data -> data.rtpTeleporter.teleport(player, p -> {
                    p.sendSystemMessage(Component.translatable("ftbessentials.rtp.looking"));
                    return findBlockPos(player.level(), p, minDistance, maxDistance);
                }).runCommand(player))
                .orElse(0);
    }

    private static TeleportPos findBlockPos(ServerLevel level, ServerPlayer player, int minDistance, int maxDistance) {
        for (int attempt = 0; attempt < FTBEConfig.RTP_MAX_TRIES.get(); attempt++) {
            double dist = minDistance + level.getRandom().nextDouble() * (maxDistance - minDistance);
            double angle = level.getRandom().nextDouble() * Math.PI * 2D;

            int x = Mth.floor(Math.cos(angle) * dist);
            int y = 256;
            int z = Mth.floor(Math.sin(angle) * dist);
            BlockPos currentPos = new BlockPos(x, y, z);

            if (!level.getWorldBorder().isWithinBounds(currentPos)) {
                continue;
            }
            if (level.getBiome(currentPos).is(IGNORE_RTP_BIOMES)) {
                continue;
            }

            // FTB Chunks (via FTB XMod Compat) listens to this.  Other mods can too.
            Outcome outcome = EventPostingHandler.INSTANCE.postEventWithResult(new RTPEvent.Data(
                    level, player, currentPos, attempt
            ));

            if (outcome.isFail()) {
                continue;
            }

            level.getChunkAt(currentPos);
            BlockPos hmPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, currentPos);

            if (hmPos.getY() > 0) {
                BlockPos goodPos = null;
                if (hmPos.getY() < level.getLogicalHeight()) {
                    goodPos = hmPos;
                } else {
                    // broken heightmap (nether, other mod dimensions)
                    for (BlockPos newPos : BlockPos.spiralAround(new BlockPos(hmPos.getX(), level.getSeaLevel(), hmPos.getZ()), 16, Direction.EAST, Direction.SOUTH)) {
                        BlockState bs = level.getBlockState(newPos);
                        if (bs.blocksMotion() && !bs.is(IGNORE_RTP_BLOCKS) && level.isEmptyBlock(newPos.above(1))
                                && level.isEmptyBlock(newPos.above(2)) && level.isEmptyBlock(newPos.above(3))) {
                            goodPos = newPos.immutable();
                            break;
                        }
                    }
                }
                if (goodPos != null) {
                    String pos = String.format(" @ [x %d, y %d, z %d]", goodPos.getX(), goodPos.getY(), goodPos.getZ());
                    player.sendSystemMessage(Component.translatable("ftbessentials.rtp.found", attempt + 1, pos));
                    return new TeleportPos(level.dimension(), goodPos.above());
                }
            }
        }
        player.sendSystemMessage(Component.translatable("ftbessentials.rtp.failed").withStyle(ChatFormatting.RED));
        return new TeleportPos(player);
    }
    //#endregion

    private static int tpLast(ServerPlayer player, NameAndId to) {
        ServerPlayer toPlayer = player.level().getServer().getPlayerList().getPlayer(to.id());
        if (toPlayer != null) {
            FTBEPlayerData.addTeleportHistory(player);
            new TeleportPos(toPlayer).teleport(player);
            return 1;
        }

        // dest player not online; teleport to where they were last seen
        return FTBEPlayerData.getOrCreate(player.level().getServer(), to.id())
                .map(data -> data.getLastSeenPos().map(pos -> {
                    FTBEPlayerData.addTeleportHistory(player);
                    pos.teleport(player);
                    return 1;
                }).orElse(0))
                .orElse(0);
    }

    private static int tpx(ServerPlayer player, ServerLevel to) {
        player.teleportTo(to, player.getX(), player.getY(), player.getZ(), Set.of(), player.getYRot(), player.getXRot(), false);
        return 1;
    }

    private static int jump(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        BlockHitResult res = BlockUtil.getFocusedBlock(player, player.level().getServer().getPlayerList().getViewDistance() * 16)
                .orElseThrow(KitCommand.NOT_LOOKING_AT_BLOCK::create);
        // want to land the player on top of the focused block, so scan up as far as needed
        BlockPos.MutableBlockPos mPos = res.getBlockPos().above().mutable();
        while (true) {
            Level level = player.level();
            if (isEmptyShape(level, mPos.above()) && isEmptyShape(level, mPos.above(2)) || mPos.getY() >= level.getMaxY())
                break;
            mPos.move(Direction.UP, 2);
        }
        Vec3 vec = Vec3.atBottomCenterOf(mPos);
        player.teleportTo(vec.x(), vec.y(), vec.z());
        return 0;
    }

    private static boolean isEmptyShape(Level level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }
}
