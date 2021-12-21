package dev.ftb.mods.ftbessentials.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.UsernameCache;

import java.util.*;

/**
 * @author LatvianModder
 */
public class HomeCommands {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("home")
				.requires(FTBEConfig.HOME)
				.executes(context -> home(context.getSource().getPlayerOrException(), "home"))
				.then(Commands.argument("name", StringArgumentType.greedyString())
						.suggests((context, builder) -> SharedSuggestionProvider.suggest(getHomeSuggestions(context), builder))
						.executes(context -> home(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
				)
		);

		dispatcher.register(Commands.literal("sethome")
				.requires(FTBEConfig.HOME)
				.executes(context -> sethome(context.getSource().getPlayerOrException(), "home"))
				.then(Commands.argument("name", StringArgumentType.greedyString())
						.executes(context -> sethome(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
				)
		);

		dispatcher.register(Commands.literal("delhome")
				.requires(FTBEConfig.HOME)
				.executes(context -> delhome(context.getSource().getPlayerOrException(), "home"))
				.then(Commands.argument("name", StringArgumentType.greedyString())
						.suggests((context, builder) -> SharedSuggestionProvider.suggest(getHomeSuggestions(context), builder))
						.executes(context -> delhome(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
				)
		);

		dispatcher.register(Commands.literal("listhomes")
				.requires(FTBEConfig.HOME)
				.executes(context -> listhomes(context.getSource(), context.getSource().getPlayerOrException().getGameProfile()))
				.then(Commands.argument("player", GameProfileArgument.gameProfile())
						.requires(source -> source.getServer().isSingleplayer() || source.hasPermission(2))
						.executes(context -> listhomes(context.getSource(), GameProfileArgument.getGameProfiles(context, "player").iterator().next()))
				)
		);

		dispatcher.register(Commands.literal("homefor")
				.requires(FTBEConfig.HOME_FOR.enabledAndOp())
				.then(Commands.argument("player", StringArgumentType.string())
						.requires(source -> source.hasPermission(2))
						.suggests((context, builder) -> SharedSuggestionProvider.suggest(getAllPlayerNameSuggestion(context.getSource().getPlayerOrException()), builder))
						.executes(context -> listHomesFor(context.getSource(), StringArgumentType.getString(context, "player")))
						.then(Commands.argument("name", StringArgumentType.greedyString())
								.requires(source -> source.hasPermission(2))
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(getOfflineHomeSuggestions(StringArgumentType.getString(context, "player")), builder))
								.executes(context -> homeFor(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "player"), StringArgumentType.getString(context, "name")))
						)
				)
		);

		dispatcher.register(Commands.literal("listhomesfor")
				.requires(FTBEConfig.HOME_FOR.enabledAndOp())
				.then(Commands.argument("player", StringArgumentType.string())
						.requires(source -> source.hasPermission(2))
						.suggests((context, builder) -> SharedSuggestionProvider.suggest(getAllPlayerNameSuggestion(context.getSource().getPlayerOrException()), builder))
						.executes(context -> listHomesFor(context.getSource(), StringArgumentType.getString(context, "player")))
				)
		);
	}

	public static Set<String> getHomeSuggestions(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		FTBEPlayerData data = FTBEPlayerData.get(context.getSource().getPlayerOrException());

		if (data == null) {
			return Collections.emptySet();
		}

		return data.homes.keySet();
	}

	public static int home(ServerPlayer player, String name) {
		FTBEPlayerData data = FTBEPlayerData.get(player);

		if (data == null) {
			return 0;
		}

		TeleportPos pos = data.homes.get(name.toLowerCase());

		if (pos == null) {
			player.displayClientMessage(new TextComponent("Home not found!"), false);
			return 0;
		}

		return data.homeTeleporter.teleport(player, p -> pos).runCommand(player);
	}

	public static int sethome(ServerPlayer player, String name) {
		FTBEPlayerData data = FTBEPlayerData.get(player);

		if (data == null) {
			return 0;
		}

		if (data.homes.size() >= FTBEConfig.MAX_HOMES.get(player) && !data.homes.containsKey(name.toLowerCase())) {
			player.displayClientMessage(new TextComponent("Can't add any more homes!"), false);
			return 0;
		}

		data.homes.put(name.toLowerCase(), new TeleportPos(player));
		data.save();
		player.displayClientMessage(new TextComponent("Home set!"), false);
		return 1;
	}

	public static int delhome(ServerPlayer player, String name) {
		FTBEPlayerData data = FTBEPlayerData.get(player);

		if (data == null) {
			return 0;
		}

		if (data.homes.remove(name.toLowerCase()) != null) {
			data.save();
			player.displayClientMessage(new TextComponent("Home deleted!"), false);
			return 1;
		} else {
			player.displayClientMessage(new TextComponent("Home not found!"), false);
			return 0;
		}
	}

	public static int listhomes(CommandSourceStack source, GameProfile of) {
		return listhomes(source, FTBEPlayerData.get(of));
	}

	public static int listhomes(CommandSourceStack source, FTBEPlayerData data) {
		if (data == null) {
			return 0;
		}

		if (data.homes.isEmpty()) {
			source.sendSuccess(new TextComponent("None"), false);
			return 1;
		}

		TeleportPos origin = new TeleportPos(source.getLevel().dimension(), new BlockPos(source.getPosition()));

		for (Map.Entry<String, TeleportPos> entry : data.homes.entrySet()) {
			source.sendSuccess(new TextComponent(entry.getKey() + ": " + entry.getValue().distanceString(origin)), false);
		}

		return 1;
	}

	private static Iterable<String> getAllPlayerNameSuggestion(ServerPlayer player) {
		List<String> names = new ArrayList<>(UsernameCache.getMap().values());
		names.remove(player.getGameProfile().getName());
		return names;
	}

	private static UUID getOfflineUUIDByName(String name) {
		return UsernameCache.getMap().entrySet().stream()
				.filter(entry -> entry.getValue().equalsIgnoreCase(name))
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(null);
	}

	private static FTBEPlayerData getOfflinePlayerData(UUID uuid) {
		if (uuid == null) return null;
		FTBEPlayerData data = FTBEPlayerData.MAP.get(uuid);
		if (data == null) {
			data = new FTBEPlayerData(uuid);
			data.load();
		}
		return data;
	}

	private static int listHomesFor(CommandSourceStack source, String name) throws CommandSyntaxException {
		UUID uuid = getOfflineUUIDByName(name);
		if (uuid == null) {
			source.getPlayerOrException().displayClientMessage(new TextComponent("No player found with name " + name + " !"), false);
			return 0;
		}
		FTBEPlayerData data = getOfflinePlayerData(uuid);
		return listhomes(source, data);
	}

	private static int homeFor(ServerPlayer player, String offlineName, String homeName) {
		UUID uuid = getOfflineUUIDByName(offlineName);
		if (uuid == null) {
			player.displayClientMessage(new TextComponent("No player with name " + offlineName + " found!"), false);
			return 0;
		}
		FTBEPlayerData data = getOfflinePlayerData(uuid);
		TeleportPos pos = data.homes.get(homeName.toLowerCase());

		if (pos == null) {
			player.displayClientMessage(new TextComponent("Home not found!"), false);
			return 0;
		}

		return data.homeTeleporter.teleport(player, p -> pos).runCommand(player);
	}

    private static Set<String> getOfflineHomeSuggestions(String offlineName) {
        UUID uuid = getOfflineUUIDByName(offlineName);
        if (uuid == null) {
            return Collections.emptySet();
        }
        FTBEPlayerData data = getOfflinePlayerData(uuid);
        return data.homes.keySet();
    }
}
