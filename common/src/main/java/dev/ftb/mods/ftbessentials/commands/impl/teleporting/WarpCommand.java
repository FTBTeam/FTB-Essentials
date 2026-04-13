package dev.ftb.mods.ftbessentials.commands.impl.teleporting;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbessentials.commands.CommandUtils;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.config.FTBEStartupConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.FTBEWorldData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.List;
import java.util.Set;

public class WarpCommand implements FTBCommand {
    @Override
    public boolean enabled() {
        return FTBEStartupConfig.WARP.isEnabled();
    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> register() {
        return List.of(
                Commands.literal("warp")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(getWarpSuggestions(), builder))
                                .executes(context -> warp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
                        ),
                Commands.literal("setwarp")
                        .requires(CommandUtils.isGamemaster())
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> setWarp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
                        ),
                Commands.literal("delwarp")
                        .requires(CommandUtils.isGamemaster())
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(getWarpSuggestions(), builder))
                                .executes(context -> deleteWarp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
                        ),
                Commands.literal("listwarps")
                        .requires(FTBEStartupConfig.WARP)
                        .executes(context -> listWarps(context.getSource()))
        );
    }

    private Set<String> getWarpSuggestions() {
        return FTBEWorldData.getInstance().warpManager().getNames();
    }

    private int warp(ServerPlayer player, String name) {
        return FTBEPlayerData.getOrCreate(player)
                .map(data -> FTBEWorldData.getInstance().warpManager().teleportTo(name, player, data.warpTeleporter).runCommand(player))
                .orElse(0);
    }

    private int setWarp(ServerPlayer player, String name) {
        FTBEWorldData.getInstance().warpManager().addDestination(name, new TeleportPos(player), player);
        player.sendSystemMessage(Component.translatable("ftbessentials.warp.set"));
        return 1;
    }

    private int deleteWarp(ServerPlayer player, String name) {
        if (FTBEWorldData.getInstance().warpManager().deleteDestination(name.toLowerCase())) {
            player.sendSystemMessage(Component.translatable("ftbessentials.warp.deleted"));
            return 1;
        } else {
            player.sendSystemMessage(Component.translatable("ftbessentials.warp.not_found"));
            return 0;
        }
    }

    private int listWarps(CommandSourceStack source) {
        if (FTBEWorldData.getInstance().warpManager().getNames().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("ftbessentials.none"), false);
        } else {
            TeleportPos origin = new TeleportPos(source.getLevel().dimension(), BlockPos.containing(source.getPosition()));
            FTBEWorldData.getInstance().warpManager().destinations().forEach(entry -> {
                MutableComponent line = Component.translatable("ftbessentials.home.show_home",
                        Component.literal(entry.name()).withStyle(ChatFormatting.AQUA), entry.destination().distanceString(origin));
                if (source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                    line.withStyle(Style.EMPTY
                            .withClickEvent(new ClickEvent.RunCommand("/tp @s " + entry.destination().posAsString()))
                            .withHoverEvent(new HoverEvent.ShowText(Component.translatable("ftbessentials.click_to_teleport")))
                    );
                }
                source.sendSuccess(() -> line, false);
            });
        }
        return 1;
    }
}
