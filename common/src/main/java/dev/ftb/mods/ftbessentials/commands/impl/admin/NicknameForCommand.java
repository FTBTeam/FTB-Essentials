package dev.ftb.mods.ftbessentials.commands.impl.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftblibrary.util.PlayerDisplayNameUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class NicknameForCommand implements FTBCommand {
    @Override
    public boolean enabled() {
        return FTBEConfig.NICK.isEnabled();
    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> register() {
        return Collections.singletonList(literal("nicknamefor")
                .requires(FTBEConfig.NICK.enabledAndOp())
                .then(argument("player", EntityArgument.player())
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> nicknameFor(context.getSource(), EntityArgument.getPlayer(context, "player"), ""))
                        .then(argument("nickname", StringArgumentType.greedyString())
                                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(context -> nicknameFor(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "nickname")))
                        )
                ));
    }

    public int nicknameFor(CommandSourceStack source, ServerPlayer player, String nick) {
        if (nick.length() > 30) {
            player.displayClientMessage(Component.literal("Nickname too long!"), false);
            return 0;
        }

        return FTBEPlayerData.getOrCreate(player).map(data -> {
            data.setNick(nick.trim());
            data.markDirty();
            PlayerDisplayNameUtil.refreshDisplayName(player);

            if (data.getNick().isEmpty()) {
                source.sendSuccess(() -> Component.literal("Nickname reset!"), true);
            } else {
                source.sendSuccess(() -> Component.literal("Nickname changed to '" + data.getNick() + "'"), true);
            }

            data.sendTabName(source.getServer());
            return 1;
        }).orElse(0);
    }
}
