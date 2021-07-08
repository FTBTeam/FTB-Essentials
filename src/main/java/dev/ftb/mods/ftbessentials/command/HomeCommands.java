package dev.ftb.mods.ftbessentials.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.ftb.mods.ftbessentials.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;

/**
 * @author LatvianModder
 */
public class HomeCommands {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("home")
				.executes(context -> home(context.getSource().getPlayerOrException(), "home"))
				.then(Commands.argument("name", StringArgumentType.greedyString())
						.suggests((context, builder) -> SharedSuggestionProvider.suggest(getHomeSuggestions(context.getSource().getPlayerOrException()), builder))
						.executes(context -> home(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
				)
		);

		dispatcher.register(Commands.literal("sethome")
				.executes(context -> sethome(context.getSource().getPlayerOrException(), "home"))
				.then(Commands.argument("name", StringArgumentType.greedyString())
						.executes(context -> sethome(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
				)
		);

		dispatcher.register(Commands.literal("delhome")
				.executes(context -> delhome(context.getSource().getPlayerOrException(), "home"))
				.then(Commands.argument("name", StringArgumentType.greedyString())
						.suggests((context, builder) -> SharedSuggestionProvider.suggest(getHomeSuggestions(context.getSource().getPlayerOrException()), builder))
						.executes(context -> delhome(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
				)
		);

		dispatcher.register(Commands.literal("listhomes")
				.executes(context -> listhomes(context.getSource(), context.getSource().getPlayerOrException().getGameProfile()))
				.then(Commands.argument("player", GameProfileArgument.gameProfile())
						.requires(source -> source.getServer().isSingleplayer() || source.hasPermission(2))
						.executes(context -> listhomes(context.getSource(), GameProfileArgument.getGameProfiles(context, "player").iterator().next()))
				)
		);
	}

	public static Set<String> getHomeSuggestions(ServerPlayer player) {
		FTBEPlayerData data = FTBEPlayerData.get(player);
		return data.homes.keySet();
	}

	public static int home(ServerPlayer player, String name) {
		FTBEPlayerData data = FTBEPlayerData.get(player);
		TeleportPos pos = data.homes.get(name.toLowerCase());

		if (pos == null) {
			player.displayClientMessage(new TextComponent("Home not found!"), false);
			return 0;
		}

		return data.homeTeleporter.teleport(player, p -> pos).runCommand(player);
	}

	public static int sethome(ServerPlayer player, String name) {
		FTBEPlayerData data = FTBEPlayerData.get(player);

		if (data.homes.size() >= FTBEConfig.getMaxHomes(player) && !data.homes.containsKey(name.toLowerCase())) {
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
		FTBEPlayerData data = FTBEPlayerData.get(of);

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
}
