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
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author LatvianModder
 */
public class HomeCommands {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		if (FTBEConfig.HOME.isEnabled()) {
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
		}
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
			player.displayClientMessage(new TranslatableComponent("home_command_message.ftbessentials.home_notfound"), false);
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
			player.displayClientMessage(new TranslatableComponent("home_command_message.ftbessentials.home_toomuch"), false);
			return 0;
		}

		data.homes.put(name.toLowerCase(), new TeleportPos(player));
		data.save();
		player.displayClientMessage(new TranslatableComponent("home_command_message.ftbessentials.home_set"), false);
		return 1;
	}

	public static int delhome(ServerPlayer player, String name) {
		FTBEPlayerData data = FTBEPlayerData.get(player);

		if (data == null) {
			return 0;
		}

		if (data.homes.remove(name.toLowerCase()) != null) {
			data.save();
			player.displayClientMessage(new TranslatableComponent("home_command_message.ftbessentials.home_deleted"), false);
			return 1;
		} else {
			player.displayClientMessage(new TranslatableComponent("home_command_message.ftbessentials.home_notfound"), false);
			return 0;
		}
	}

	public static int listhomes(CommandSourceStack source, GameProfile of) {
		FTBEPlayerData data = FTBEPlayerData.get(of);

		if (data == null) {
			return 0;
		}

		if (data.homes.isEmpty()) {
			source.sendSuccess(new TranslatableComponent("home_command_message.ftbessentials.home_none"), false);
			return 1;
		}

		TeleportPos origin = new TeleportPos(source.getLevel().dimension(), new BlockPos(source.getPosition()));

		for (Map.Entry<String, TeleportPos> entry : data.homes.entrySet()) {
			source.sendSuccess(new TextComponent(entry.getKey() + ": " + entry.getValue().distanceString(origin)), false);
		}

		return 1;
	}
}
