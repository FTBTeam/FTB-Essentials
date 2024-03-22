package dev.ftb.mods.ftbessentials.commands.impl.teleporting;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.ftb.mods.ftbessentials.commands.CommandUtils;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.FTBEWorldData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

public class WarpCommand implements FTBCommand {
    @Override
    public boolean enabled() {
        return FTBEConfig.WARP.isEnabled();
    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> register() {
        return List.of(
                Commands.literal("warp")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(getWarpSuggestions(context), builder))
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
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(getWarpSuggestions(context), builder))
                                .executes(context -> deleteWarp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
                        ),
                Commands.literal("listwarps")
                        .requires(FTBEConfig.WARP)
                        .executes(context -> listWarps(context.getSource()))
        );
    }

    private Set<String> getWarpSuggestions(CommandContext<CommandSourceStack> context) {
        return FTBEWorldData.instance.warpManager().getNames();
    }

    private int warp(ServerPlayer player, String name) {
        return FTBEPlayerData.getOrCreate(player)
                .map(data -> FTBEWorldData.instance.warpManager().teleportTo(name, player, data.warpTeleporter).runCommand(player))
                .orElse(0);
    }

    private int setWarp(ServerPlayer player, String name) {
        FTBEWorldData.instance.warpManager().addDestination(name, new TeleportPos(player), player);
        player.displayClientMessage(Component.literal("Warp set!"), false);
        return 1;
    }

    private int deleteWarp(ServerPlayer player, String name) {
        if (FTBEWorldData.instance.warpManager().deleteDestination(name.toLowerCase())) {
            player.displayClientMessage(Component.literal("Warp deleted!"), false);
            return 1;
        } else {
            player.displayClientMessage(Component.literal("Warp not found!"), false);
            return 0;
        }
    }

    private int listWarps(CommandSourceStack source) {
        if (FTBEWorldData.instance.warpManager().getNames().isEmpty()) {
            source.sendSuccess(() -> Component.literal("None"), false);
        } else {
            TeleportPos origin = new TeleportPos(source.getLevel().dimension(), BlockPos.containing(source.getPosition()));
            FTBEWorldData.instance.warpManager().destinations().forEach(entry ->
                    source.sendSuccess(() -> Component.literal(entry.name() + ": " + entry.destination().distanceString(origin)), false));
        }
        return 1;
    }
}
