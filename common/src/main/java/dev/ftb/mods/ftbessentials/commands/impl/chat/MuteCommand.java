package dev.ftb.mods.ftbessentials.commands.impl.chat;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbessentials.commands.CommandUtils;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.DurationInfo;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.FTBEWorldData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class MuteCommand implements FTBCommand {
    @Override
    public boolean enabled() {
        return FTBEConfig.MUTE.isEnabled();
    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> register() {
        return List.of(
                literal("mute")
                        .requires(FTBEConfig.MUTE.enabledAndOp())
                        .then(argument("player", EntityArgument.player())
                                .executes(context -> mute(context.getSource(), EntityArgument.getPlayer(context, "player"), ""))
                                .then(argument("until", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> CommandUtils.suggestDurations(builder))
                                        .executes(context -> mute(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "until")))
                                )
                        ),
                literal("unmute")
                        .requires(FTBEConfig.MUTE.enabledAndOp())
                        .then(argument("player", EntityArgument.player())
                                .executes(context -> unmute(context.getSource(), EntityArgument.getPlayer(context, "player")))
                        )
        );
    }

    private int mute(CommandSourceStack source, ServerPlayer player, String duration) {
        return FTBEPlayerData.getOrCreate(player).map(data -> {
            try {
                DurationInfo info = DurationInfo.fromString(duration);
                data.setMuted(true);
                FTBEWorldData.instance.setMuteTimeout(player, info.until());

                MutableComponent msg = player.getDisplayName().copy()
                        .append(" has been muted by ")
                        .append(source.getDisplayName())
                        .append(", ")
                        .append(info.desc());
                notifyMuting(source, player, msg);

                return 1;
            } catch (IllegalArgumentException e) {
                source.sendFailure(Component.literal("Invalid duration syntax: '" + duration + "': " + e.getMessage()));
                return 0;
            }
        }).orElse(0);
    }

    private int unmute(CommandSourceStack source, ServerPlayer player) {
        return FTBEPlayerData.getOrCreate(player).map(data -> {
            data.setMuted(false);
            FTBEWorldData.instance.setMuteTimeout(player, -1);

            MutableComponent msg = player.getDisplayName().copy()
                    .append(" has been unmuted by ")
                    .append(source.getDisplayName());
            notifyMuting(source, player, msg);

            return 1;
        }).orElse(0);
    }

    private void notifyMuting(CommandSourceStack source, Player target, Component msg) {
        // notify any online ops, plus the player being (un)muted
        source.getServer().getPlayerList().getPlayers().forEach(p -> {
            if (p.hasPermissions(2) || p == target) {
                p.displayClientMessage(msg, false);
            }
        });
        // notify command sender if not actually a player
        if (!source.isPlayer()) {
            source.sendSuccess(() -> msg, true);
        }
    }
}
