package dev.ftb.mods.ftbessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.FTBEWorldData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class WarpCommands {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("warp")
				.then(Commands.argument("name", StringArgumentType.greedyString())
						.suggests((context, builder) -> SharedSuggestionProvider.suggest(getWarpSuggestions(context), builder))
						.executes(context -> warp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
				)
		);

		dispatcher.register(Commands.literal("setwarp")
				.requires(source -> source.hasPermission(2))
				.then(Commands.argument("name", StringArgumentType.greedyString())
						.executes(context -> setwarp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
				)
		);

		dispatcher.register(Commands.literal("delwarp")
				.requires(source -> source.hasPermission(2))
				.then(Commands.argument("name", StringArgumentType.greedyString())
						.suggests((context, builder) -> SharedSuggestionProvider.suggest(getWarpSuggestions(context), builder))
						.executes(context -> delwarp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
				)
		);

		dispatcher.register(Commands.literal("listwarps")
				.executes(context -> listwarps(context.getSource()))
		);
	}

	public static Set<String> getWarpSuggestions(CommandContext<CommandSourceStack> context) {
		return FTBEWorldData.instance.warps.keySet();
	}

	public static int warp(ServerPlayer player, String name) {
		FTBEPlayerData data = FTBEPlayerData.get(player);
		TeleportPos pos = FTBEWorldData.instance.warps.get(name.toLowerCase());

		if (pos == null) {
			player.displayClientMessage(new TextComponent("Warp not found!"), false);
			return 0;
		}

		return data.warpTeleporter.teleport(player, p -> pos).runCommand(player);
	}

	public static int setwarp(ServerPlayer player, String name) {
		FTBEWorldData.instance.warps.put(name.toLowerCase(), new TeleportPos(player));
		FTBEWorldData.instance.save();
		player.displayClientMessage(new TextComponent("Warp set!"), false);
		return 1;
	}

	public static int delwarp(ServerPlayer player, String name) {
		if (FTBEWorldData.instance.warps.remove(name.toLowerCase()) != null) {
			FTBEWorldData.instance.save();
			player.displayClientMessage(new TextComponent("Warp deleted!"), false);
			return 1;
		} else {
			player.displayClientMessage(new TextComponent("Warp not found!"), false);
			return 0;
		}
	}

	public static int listwarps(CommandSourceStack source) {
		if (FTBEWorldData.instance.warps.isEmpty()) {
			source.sendSuccess(new TextComponent("None"), false);
			return 1;
		}

		TeleportPos origin = new TeleportPos(source.getLevel().dimension(), new BlockPos(source.getPosition()));

		for (Map.Entry<String, TeleportPos> entry : FTBEWorldData.instance.warps.entrySet()) {
			source.sendSuccess(new TextComponent(entry.getKey() + ": " + entry.getValue().distanceString(origin)), false);
		}

		return 1;
	}
}
