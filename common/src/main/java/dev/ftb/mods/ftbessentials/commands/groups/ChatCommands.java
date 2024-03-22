package dev.ftb.mods.ftbessentials.commands.groups;

import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.commands.SimpleConfigurableCommand;
import dev.ftb.mods.ftbessentials.commands.impl.chat.MuteCommand;
import dev.ftb.mods.ftbessentials.commands.impl.chat.NicknameCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftblibrary.util.PlayerDisplayNameUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

import static net.minecraft.commands.Commands.literal;

public class ChatCommands {
    public static final List<FTBCommand> COMMANDS = List.of(
            new MuteCommand(),
            new NicknameCommand(),

            // Recording
            new SimpleConfigurableCommand(FTBEConfig.REC, literal("recording").executes(context -> recording(context.getSource().getPlayerOrException()))),
            new SimpleConfigurableCommand(FTBEConfig.REC, literal("streaming").executes(context -> streaming(context.getSource().getPlayerOrException())))
    );

    // TODO: These commands basically do the same thing, should be refactored into a single command
    public static int recording(ServerPlayer player) {
        return FTBEPlayerData.getOrCreate(player).map(data -> {
            data.setRecording(data.getRecording() == FTBEPlayerData.RecordingStatus.RECORDING ? FTBEPlayerData.RecordingStatus.NONE : FTBEPlayerData.RecordingStatus.RECORDING);
            PlayerDisplayNameUtil.refreshDisplayName(player);

            if (data.getRecording() == FTBEPlayerData.RecordingStatus.RECORDING) {
                player.server.getPlayerList().broadcastSystemMessage(player.getDisplayName().copy().withStyle(ChatFormatting.YELLOW).append(" ").append(Component.translatable("ftbessentials.chat.status.start_record")), false);
            } else {
                player.server.getPlayerList().broadcastSystemMessage(player.getDisplayName().copy().withStyle(ChatFormatting.YELLOW).append(" ").append(Component.translatable("ftbessentials.chat.status.stop_record")), false);
            }

            data.sendTabName(player.server);
            return 1;
        }).orElse(0);
    }

    public static int streaming(ServerPlayer player) {
        return FTBEPlayerData.getOrCreate(player).map(data -> {
            data.setRecording(data.getRecording() == FTBEPlayerData.RecordingStatus.STREAMING ? FTBEPlayerData.RecordingStatus.NONE : FTBEPlayerData.RecordingStatus.STREAMING);
            PlayerDisplayNameUtil.refreshDisplayName(player);

            if (data.getRecording() == FTBEPlayerData.RecordingStatus.STREAMING) {
                player.server.getPlayerList().broadcastSystemMessage(player.getDisplayName().copy().withStyle(ChatFormatting.YELLOW).append(" ").append(Component.translatable("ftbessentials.chat.status.start_stream")), false);
            } else {
                player.server.getPlayerList().broadcastSystemMessage(player.getDisplayName().copy().withStyle(ChatFormatting.YELLOW).append(" ").append(Component.translatable("ftbessentials.chat.status.stop_stream")), false);
            }

            data.sendTabName(player.server);
            return 1;
        }).orElse(0);
    }
}
