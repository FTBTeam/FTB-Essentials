package dev.ftb.mods.ftbessentials.commands.impl.teleporting;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.SavedTeleportManager;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

public class HomeCommand implements FTBCommand {
    @Override
    public boolean enabled() {
        return FTBEConfig.HOME.isEnabled();
    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> register() {
        return List.of(
                Commands.literal("home")
                        .requires(FTBEConfig.HOME)
                        .executes(context -> home(context.getSource().getPlayerOrException(), "home"))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(getHomeSuggestions(context), builder))
                                .executes(context -> home(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
                        ),
                Commands.literal("sethome")
                        .requires(FTBEConfig.HOME)
                        .executes(context -> setHome(context.getSource().getPlayerOrException(), "home"))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> setHome(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
                        ),
                Commands.literal("delhome")
                        .requires(FTBEConfig.HOME)
                        .executes(context -> delHome(context.getSource().getPlayerOrException(), "home"))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(getHomeSuggestions(context), builder))
                                .executes(context -> delHome(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
                        ),
                Commands.literal("listhomes")
                        .requires(FTBEConfig.HOME)
                        .executes(context -> listHomes(context.getSource(), context.getSource().getPlayerOrException().getGameProfile()))
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .requires(source -> source.getServer().isSingleplayer() || source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(context -> listHomes(context.getSource(), GameProfileArgument.getGameProfiles(context, "player").iterator().next()))
                        )
        );
    }

    public Set<String> getHomeSuggestions(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return FTBEPlayerData.getOrCreate(context.getSource().getPlayerOrException())
                .map(data -> data.homeManager().getNames())
                .orElse(Set.of());
    }

    public int home(ServerPlayer player, String name) {
        return FTBEPlayerData.getOrCreate(player)
                .map(data -> data.homeManager().teleportTo(name, player, data.homeTeleporter).runCommand(player))
                .orElse(0);
    }

    public int setHome(ServerPlayer player, String name) {
        return FTBEPlayerData.getOrCreate(player).map(data -> {
            try {
                if (player.blockPosition().getY() < FTBEConfig.HOME_MIN_Y.get()) {
                    player.displayClientMessage(Component.translatable("ftbessentials.home.y_too_low", FTBEConfig.HOME_MIN_Y.get()), false);
                    return 0;
                }
                data.homeManager().addDestination(name, new TeleportPos(player), player);
                player.displayClientMessage(Component.translatable("ftbessentials.home.set"), false);
                return 1;
            } catch (SavedTeleportManager.TooManyDestinationsException e) {
                player.displayClientMessage(Component.translatable("ftbessentials.home.too_many"), false);
                return 0;
            }
        }).orElse(0);
    }

    public int delHome(ServerPlayer player, String name) {
        return FTBEPlayerData.getOrCreate(player).map(data -> {
            if (data.homeManager().deleteDestination(name.toLowerCase())) {
                player.displayClientMessage(Component.translatable("ftbessentials.home.deleted"), false);
                return 1;
            } else {
                player.displayClientMessage(Component.translatable("ftbessentials.home.not_found"), false);
                return 0;
            }
        }).orElse(0);
    }

    public static int listHomes(CommandSourceStack source, GameProfile of) {
        return FTBEPlayerData.getOrCreate(source.getServer(), of.getId())
                .map(data -> {
                    if (data.homeManager().getNames().isEmpty()) {
                        source.sendSuccess(() -> Component.translatable("ftbessentials.none"), false);
                    } else {
                        source.sendSuccess(() -> Component.translatable("ftbessentials.home.for_player", of.getName()).withStyle(ChatFormatting.GOLD), false);
                        source.sendSuccess(() -> Component.literal("---").withStyle(ChatFormatting.GOLD), false);

                        TeleportPos origin = new TeleportPos(source.getLevel().dimension(), BlockPos.containing(source.getPosition()));
                        data.homeManager().destinations().forEach(entry ->
                                source.sendSuccess(() -> {
                                    MutableComponent line = Component.translatable("ftbessentials.home.show_home",
                                            Component.literal(entry.name()).withStyle(ChatFormatting.AQUA), entry.destination().distanceString(origin));

                                    if (source.hasPermission(Commands.LEVEL_GAMEMASTERS)) {
                                        line.withStyle(Style.EMPTY
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp @s " + entry.destination().posAsString()))
                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("ftbessentials.click_to_teleport")))
                                        );
                                    }
                                    return line;
                                }, false));
                    }

                    return 1;
                }).orElse(0);
    }
}
