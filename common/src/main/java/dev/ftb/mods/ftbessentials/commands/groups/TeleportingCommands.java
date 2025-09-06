package dev.ftb.mods.ftbessentials.commands.groups;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.FTBEssentialsEvents;
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
import net.minecraft.tags.FluidTags;
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
                player.displayClientMessage(Component.translatable("ftbessentials.teleport.history_empty").withStyle(ChatFormatting.RED), false);
                return 0;
            }

            TeleportPos lastPos = data.teleportHistory.getLast();
            BlockPos targetPos = lastPos.getPos();
            ServerLevel targetLevel = player.server.getLevel(lastPos.getDimension());
            if (targetLevel == null) {
                if (data.backTeleporter.teleport(player, serverPlayerEntity -> lastPos).runCommand(player) != 0) {
                    data.markDirty();
                    return 1;
                }
                return 0;
            }
            BlockPos safePos = findSafeBackPos(targetLevel, targetPos);

            if (safePos == null) {
                player.displayClientMessage(Component.translatable("ftbessentials.teleport.no_safe_position").withStyle(ChatFormatting.RED), false);
                return 0;
            }

            if (data.backTeleporter.teleport(player, serverPlayerEntity -> new TeleportPos(targetLevel.dimension(), safePos)).runCommand(player) != 0) {
                data.markDirty();
                return 1;
            }
            return 0;
        }).orElse(0);
    }

    private static BlockPos findSafeBackPos(ServerLevel world, BlockPos originalPos) {
        if (isSafePosition(world, originalPos)) {
            return originalPos;
        }
        for (BlockPos pos : BlockPos.spiralAround(originalPos, 16, Direction.EAST, Direction.SOUTH)) {
            if (isSafePosition(world, pos)) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static boolean isSafePosition(ServerLevel world, BlockPos pos) {
        if (pos.getY() < world.getMinBuildHeight() || pos.getY() >= world.getMaxBuildHeight()) {
            return false;
        }
        BlockPos headPos = pos.above();
        BlockPos groundPos = pos.below();
        BlockState groundState = world.getBlockState(groundPos);
        if (!groundState.isCollisionShapeFullBlock(world, groundPos) && !groundState.isFaceSturdy(world, groundPos, Direction.UP)) {

            return false;
        }
        BlockState feetState = world.getBlockState(pos);
        if (!feetState.isAir() && !feetState.getCollisionShape(world, pos).isEmpty()) {
            return false;
        }
        BlockState headState = world.getBlockState(headPos);
        if (!headState.isAir() && !headState.getCollisionShape(world, headPos).isEmpty()) {
            return false;
        }

        // Check for dangerous fluids
        return !(world.getFluidState(pos).is(FluidTags.LAVA) || world.getFluidState(headPos).is(FluidTags.LAVA));
    }

    private static int spawn(ServerPlayer player) {
        return FTBEPlayerData.getOrCreate(player).map(data -> {
            ServerLevel level = player.server.getLevel(Level.OVERWORLD);
            return level == null ? 0 : data.spawnTeleporter.teleport(player, p -> new TeleportPos(level, level.getSharedSpawnPos(), level.getSharedSpawnAngle(), 0F)).runCommand(player);
        }).orElse(0);
    }

    //#region RTP
    private static int rtp(ServerPlayer player, int minDistance, int maxDistance) {
        if (maxDistance < minDistance) {
            player.displayClientMessage(Component.translatable("ftbessentials.teleport.max_less_than_min"), false);
            return 0;
        }
        if ((!player.hasPermissions(Commands.LEVEL_GAMEMASTERS) || !FTBEConfig.ADMINS_EXEMPT_DIMENSION_BLACKLISTS.get())
                && !DimensionFilter.isRtpDimensionOK(player.level().dimension())) {
            player.displayClientMessage(Component.translatable("ftbessentials.rtp.not_here").withStyle(ChatFormatting.RED), false);
            return 0;
        }
        return FTBEPlayerData.getOrCreate(player).map(data -> data.rtpTeleporter.teleport(player, p -> {
                    p.displayClientMessage(Component.translatable("ftbessentials.rtp.looking"), false);
                    return findBlockPos((ServerLevel) player.level(), p, minDistance, maxDistance);
                }).runCommand(player))
                .orElse(0);
    }

    private static TeleportPos findBlockPos(ServerLevel world, ServerPlayer player, int minDistance, int maxDistance) {
        for (int attempt = 0; attempt < FTBEConfig.RTP_MAX_TRIES.get(); attempt++) {
            double dist = minDistance + world.random.nextDouble() * (maxDistance - minDistance);
            double angle = world.random.nextDouble() * Math.PI * 2D;

            int x = Mth.floor(Math.cos(angle) * dist);
            int y = 256;
            int z = Mth.floor(Math.sin(angle) * dist);
            BlockPos currentPos = new BlockPos(x, y, z);
            // Check world border
            if (!world.getWorldBorder().isWithinBounds(currentPos)) {
                continue;
            }
            // Biome blacklist check
            if (world.getBiome(currentPos).is(IGNORE_RTP_BIOMES)) {
                continue;
            }
            // FTB Chunks (via FTB XMod Compat) listens to this.  Other mods can too.
            EventResult res = FTBEssentialsEvents.RTP_EVENT.invoker().teleport(world, player, currentPos, attempt);
            if (res.isFalse()) {
                continue;
            }

            world.getChunkAt(currentPos);
            // Heightmap check
            BlockPos hmPos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, currentPos);
            if (hmPos.getY() > 0) {
                BlockPos goodPos = null;
                // Check if the position is in liquid
                BlockPos belowPos = hmPos.below();
                if (hmPos.getY() < world.getLogicalHeight() &&
                        !(world.getFluidState(belowPos).is(FluidTags.WATER) || world.getFluidState(belowPos).is(FluidTags.LAVA))) {
                    goodPos = hmPos;
                } else {
                    // broken heightmap (nether, other mod dimensions)
                    for (BlockPos newPos : BlockPos.spiralAround(new BlockPos(hmPos.getX(), world.getSeaLevel(), hmPos.getZ()), 16, Direction.EAST, Direction.SOUTH)) {
                        BlockState bs = world.getBlockState(newPos);
                        if (bs.isCollisionShapeFullBlock(world, newPos) && !bs.is(IGNORE_RTP_BLOCKS) && world.isEmptyBlock(newPos.above(1))
                                && world.isEmptyBlock(newPos.above(2)) && world.isEmptyBlock(newPos.above(3))) {
                            goodPos = newPos.immutable();
                            break;
                        }
                    }
                }
                if (goodPos != null) {
                    String pos = String.format(" @ [x %d, y %d, z %d]", goodPos.getX(), goodPos.getY(), goodPos.getZ());
                    player.displayClientMessage(Component.translatable("ftbessentials.rtp.found", attempt + 1, pos), false);
                    return new TeleportPos(world.dimension(), goodPos.above());
                }
            }
        }
        player.displayClientMessage(Component.translatable("ftbessentials.rtp.failed").withStyle(ChatFormatting.RED), false);
        return new TeleportPos(player);
    }
    //#endregion

    private static int tpLast(ServerPlayer player, GameProfile to) {
        ServerPlayer toPlayer = player.server.getPlayerList().getPlayer(to.getId());
        if (toPlayer != null) {
            FTBEPlayerData.addTeleportHistory(player);
            new TeleportPos(toPlayer).teleport(player);
            return 1;
        }

        // dest player not online; teleport to where they were last seen
        return FTBEPlayerData.getOrCreate(player.getServer(), to.getId())
                .map(data -> {
                    FTBEPlayerData.addTeleportHistory(player);
                    data.getLastSeenPos().teleport(player);

                    return 1;
                }).orElse(0);
    }

    private static int tpx(ServerPlayer player, ServerLevel to) {
        player.teleportTo(to, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        return 1;
    }

    private static int jump(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        BlockHitResult res = BlockUtil.getFocusedBlock(player, Objects.requireNonNull(player.getServer()).getPlayerList().getViewDistance() * 16)
                .orElseThrow(KitCommand.NOT_LOOKING_AT_BLOCK::create);
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
        return 0;
    }

    private static boolean isEmptyShape(Level level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }
}
