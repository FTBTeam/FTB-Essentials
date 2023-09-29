package dev.ftb.mods.ftbessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * @author LatvianModder
 */
public class FTBEssentialsCommands {
	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		HomeCommands.register(dispatcher);
		WarpCommands.register(dispatcher);
		TeleportCommands.register(dispatcher);
		TPACommands.register(dispatcher);
		CheatCommands.register(dispatcher);
		MiscCommands.register(dispatcher);
		KitCommands.register(dispatcher);
	}

	static CompletableFuture<Suggestions> suggestDurations(SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(Stream.of("5m", "10m", "1h", "1d", "1w", "* (indefinite)", "<number>[smhdw]"), builder);
	}

	static CompletableFuture<Suggestions> suggestCooldowns(SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(Stream.of("5m", "10m", "1h", "1d", "1w", "* (once only)", "<number>[smhdw]"), builder);
	}
}
