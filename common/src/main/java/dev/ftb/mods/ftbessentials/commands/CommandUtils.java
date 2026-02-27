package dev.ftb.mods.ftbessentials.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.permissions.Permissions;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CommandUtils {
    private static final Predicate<CommandSourceStack> IS_OP = ctx -> ctx.permissions().hasPermission(Permissions.COMMANDS_OWNER);
    private static final Predicate<CommandSourceStack> IS_ADMIN = ctx -> ctx.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
    private static final Predicate<CommandSourceStack> IS_GAMEMASTER = ctx -> ctx.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    private static final Predicate<CommandSourceStack> IS_MOD = ctx -> ctx.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);

    //#region Permission predicates shortcuts
    public static Predicate<CommandSourceStack> isOp() {
        return IS_OP;
    }

    public static Predicate<CommandSourceStack> isAdmin() {
        return IS_ADMIN;
    }

    public static Predicate<CommandSourceStack> isGamemaster() {
        return IS_GAMEMASTER;
    }

    public static Predicate<CommandSourceStack> isMod() {
        return IS_MOD;
    }

    public static CompletableFuture<Suggestions> suggestDurations(SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Stream.of("5m", "10m", "1h", "1d", "1w", "* (indefinite)", "<number>[smhdw]"), builder);
    }

    public static CompletableFuture<Suggestions> suggestCooldowns(SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Stream.of("5m", "10m", "1h", "1d", "1w", "* (once only)", "<number>[smhdw]"), builder);
    }
    //#endregion
}
