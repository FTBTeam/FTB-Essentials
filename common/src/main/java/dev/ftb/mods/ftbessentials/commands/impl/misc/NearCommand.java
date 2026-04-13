package dev.ftb.mods.ftbessentials.commands.impl.misc;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbessentials.commands.CommandUtils;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.config.FTBEStartupConfig;
import dev.ftb.mods.ftblibrary.integration.permissions.PermissionHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class NearCommand implements FTBCommand {
    protected static final int DEFAULT_RADIUS = 200;   // default radius for /near command
    protected static final int MAX_PLAYER_RADIUS = 16; // max radius for non-admin users (adjustable with FTB Ranks)

    @Override
    public boolean enabled() {
        return FTBEStartupConfig.NEAR.isEnabled();
    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> register() {
        return Collections.singletonList(literal("near")
                .requires(CommandUtils.isGamemaster())
                .executes(context -> showNear(context.getSource(), context.getSource().getPlayerOrException(), DEFAULT_RADIUS))
                .then(argument("radius", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                        .executes(context -> showNear(context.getSource(), context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "radius")))
                )
                .then(argument("player", EntityArgument.player())
                        .executes(context -> showNear(context.getSource(), EntityArgument.getPlayer(context, "player"), DEFAULT_RADIUS))
                        .then(argument("radius", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                                .executes(context -> showNear(context.getSource(), EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "radius")))
                        )
                ));
    }

    private static int showNear(CommandSourceStack source, ServerPlayer target, int radius) {
        if (!source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) && source.isPlayer()) {
            int max = PermissionHelper.getProvider().getIntegerPermission(source.getPlayer(),"ftbessentials.near.max_radius", MAX_PLAYER_RADIUS);
            if (radius > max) {
                source.sendSuccess(() -> Component.translatable("ftbessentials.feedback.limit_radius", max).withStyle(ChatFormatting.GOLD), false);
                radius = max;
            }
        }

        int radius2 = radius * radius;
        List<ServerPlayer> l = target.level().getServer().getPlayerList().getPlayers().stream()
                .filter(other -> other != target)
                .filter(other -> other.distanceToSqr(target) < radius2)
                .sorted(Comparator.comparingDouble(o -> o.distanceToSqr(target)))
                .toList();

        final int r = radius;
        source.sendSuccess(() -> Component.translatable("ftbessentials.near.players_within", l.size(), r).withStyle(ChatFormatting.YELLOW), false);
        l.forEach(player ->
                source.sendSuccess(() -> Component.literal("• ")
                                .append(player.getDisplayName()).withStyle(ChatFormatting.AQUA)
                                .append(String.format(" - %5.2fm", player.distanceTo(target))),
                        false
                )
        );

        return 1;
    }
}
