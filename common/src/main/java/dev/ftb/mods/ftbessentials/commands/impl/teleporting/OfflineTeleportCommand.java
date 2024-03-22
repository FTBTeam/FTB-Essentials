package dev.ftb.mods.ftbessentials.commands.impl.teleporting;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class OfflineTeleportCommand implements FTBCommand {
    @Override
    public boolean enabled() {
        return FTBEConfig.TP_OFFLINE.isEnabled();
    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> register() {
        LiteralArgumentBuilder<CommandSourceStack> tpOffline = literal("tp_offline");
        tpOffline.requires(FTBEConfig.TP_OFFLINE.enabledAndOp())
                .then(literal("name")
                        .then(argument("player", StringArgumentType.word())
                                .then(argument("pos", Vec3Argument.vec3())
                                        .executes(ctx -> tpOffline(ctx.getSource(), StringArgumentType.getString(ctx,"player"), ctx.getSource().getLevel(), Vec3Argument.getCoordinates(ctx, "pos")))
                                )
                        )
                )
                .then(literal("id")
                        .then(argument("player_id", UuidArgument.uuid())
                                .then(argument("pos", Vec3Argument.vec3())
                                        .executes(ctx -> tpOffline(ctx.getSource(), UuidArgument.getUuid(ctx,"player_id"), ctx.getSource().getLevel(), Vec3Argument.getCoordinates(ctx, "pos")))
                                )
                        )
                );

        var alias = literal("tpo").redirect(tpOffline.build());

        return List.of(tpOffline, alias);
    }

    private int tpOffline(CommandSourceStack source, String playerName, ServerLevel level, Coordinates dest) {
        source.getServer().getProfileCache().getAsync(playerName).whenComplete((profileOpt, throwable) -> {
            source.getServer().executeIfPossible(() ->
                    profileOpt.ifPresentOrElse(profile -> tpOffline(source, profile.getId(), level, dest),
                            () -> source.sendFailure(Component.literal("Unknown player: " + playerName))
                    )
            );
        });

        return 1;
    }

    private int tpOffline(CommandSourceStack source, UUID playerId, ServerLevel level, Coordinates dest) {
        MinecraftServer server = source.getServer();

        Path playerDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        Path datFile = playerDir.resolve(playerId + ".dat");

        if (server.getPlayerList().getPlayer(playerId) != null) {
            source.sendFailure(Component.literal("Player is online! Use regular /tp command instead"));
            return 0;
        }

        try {
            CompoundTag tag = NbtIo.readCompressed(datFile, NbtAccounter.unlimitedHeap());

            Vec3 vec = dest.getPosition(source);
            ListTag newPos = new ListTag();
            newPos.add(DoubleTag.valueOf(vec.x));
            newPos.add(DoubleTag.valueOf(vec.y));
            newPos.add(DoubleTag.valueOf(vec.z));
            tag.put("Pos", newPos);

            tag.putString("Dimension", level.dimension().location().toString());

            Path tempFile = File.createTempFile(playerId + "-", ".dat", playerDir.toFile()).toPath();
            NbtIo.writeCompressed(tag, tempFile);
            Path backupFile = playerDir.resolve(playerId + ".dat_old");
            Util.safeReplaceFile(datFile, tempFile, backupFile);

            source.sendSuccess(() -> Component.literal(String.format("Offline player %s moved to [%.2f,%.2f,%.2f] in %s",
                    playerId, vec.x, vec.y, vec.z, source.getLevel().dimension().location())), false);
            return 1;
        } catch (IOException e) {
            source.sendFailure(Component.literal("Can't update dat file: " + e.getMessage()));
            return 0;
        }
    }
}
