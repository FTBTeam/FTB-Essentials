package dev.ftb.mods.ftbessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
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

import java.util.Set;

/**
 * @author LatvianModder
 */
public class WarpCommands {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		if (FTBEConfig.WARP.isEnabled()) {
			dispatcher.register(Commands.literal("warp")
					.requires(FTBEConfig.WARP)
					.then(Commands.argument("name", StringArgumentType.greedyString())
							.suggests((context, builder) -> SharedSuggestionProvider.suggest(getWarpSuggestions(context), builder))
							.executes(context -> warp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
					)
			);

			dispatcher.register(Commands.literal("setwarp")
					.requires(FTBEConfig.WARP.enabledAndOp())
					.then(Commands.argument("name", StringArgumentType.greedyString())
							.executes(context -> setWarp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
					)
			);

			dispatcher.register(Commands.literal("delwarp")
					.requires(FTBEConfig.WARP.enabledAndOp())
					.then(Commands.argument("name", StringArgumentType.greedyString())
							.suggests((context, builder) -> SharedSuggestionProvider.suggest(getWarpSuggestions(context), builder))
							.executes(context -> deleteWarp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
					)
			);

			dispatcher.register(Commands.literal("listwarps")
					.requires(FTBEConfig.WARP)
					.executes(context -> listWarps(context.getSource()))
			);
		}
	}

	public static Set<String> getWarpSuggestions(CommandContext<CommandSourceStack> context) {
		return FTBEWorldData.instance.warpManager().getNames();
	}

	public static int warp(ServerPlayer player, String name) {
		return FTBEPlayerData.getOrCreate(player)
				.map(data -> FTBEWorldData.instance.warpManager().teleportTo(name, player, data.warpTeleporter).runCommand(player))
				.orElse(0);
	}

	public static int setWarp(ServerPlayer player, String name) {
		FTBEWorldData.instance.warpManager().addDestination(name, new TeleportPos(player), player);
		player.displayClientMessage(Component.literal("Warp set!"), false);
		return 1;
	}

	public static int deleteWarp(ServerPlayer player, String name) {
		if (FTBEWorldData.instance.warpManager().deleteDestination(name.toLowerCase())) {
			player.displayClientMessage(Component.literal("Warp deleted!"), false);
			return 1;
		} else {
			player.displayClientMessage(Component.literal("Warp not found!"), false);
			return 0;
		}
	}

	public static int listWarps(CommandSourceStack source) {
		if (FTBEWorldData.instance.warpManager().getNames().isEmpty()) {
			source.sendSuccess(Component.literal("None"), false);
		} else {
			TeleportPos origin = new TeleportPos(source.getLevel().dimension(), BlockPos.containing(source.getPosition()));
			FTBEWorldData.instance.warpManager().destinations().forEach(entry ->
					source.sendSuccess(Component.literal(entry.name() + ": " + entry.destination().distanceString(origin)), false));
		}
		return 1;
	}
}
