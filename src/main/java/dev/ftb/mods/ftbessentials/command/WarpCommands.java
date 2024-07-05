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
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
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
							.executes(context -> setwarp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
					)
			);

			dispatcher.register(Commands.literal("delwarp")
					.requires(FTBEConfig.WARP.enabledAndOp())
					.then(Commands.argument("name", StringArgumentType.greedyString())
							.suggests((context, builder) -> SharedSuggestionProvider.suggest(getWarpSuggestions(context), builder))
							.executes(context -> delwarp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
					)
			);

			dispatcher.register(Commands.literal("listwarps")
					.requires(FTBEConfig.WARP)
					.executes(context -> listwarps(context.getSource()))
			);
		}
	}

	public static Set<String> getWarpSuggestions(CommandContext<CommandSourceStack> context) {
		return FTBEWorldData.instance.warps.keySet();
	}

	public static int warp(ServerPlayer player, String name) {
		FTBEPlayerData data = FTBEPlayerData.get(player);
		TeleportPos pos = FTBEWorldData.instance.warps.get(name.toLowerCase());

		if (pos == null) {
			player.displayClientMessage(new TranslatableComponent("warp_command_message.ftbessentials.warp_notfound"), false);
			return 0;
		}

		return data.warpTeleporter.teleport(player, p -> pos).runCommand(player);
	}

	public static int setwarp(ServerPlayer player, String name) {
		FTBEWorldData.instance.warps.put(name.toLowerCase(), new TeleportPos(player));
		FTBEWorldData.instance.save();
		player.displayClientMessage(new TranslatableComponent("warp_command_message.ftbessentials.warp_set"), false);
		return 1;
	}

	public static int delwarp(ServerPlayer player, String name) {
		if (FTBEWorldData.instance.warps.remove(name.toLowerCase()) != null) {
			FTBEWorldData.instance.save();
			player.displayClientMessage(new TranslatableComponent("warp_command_message.ftbessentials.warp_deleted"), false);
			return 1;
		} else {
			player.displayClientMessage(new TranslatableComponent("warp_command_message.ftbessentials.warp_notfound"), false);
			return 0;
		}
	}

	public static int listwarps(CommandSourceStack source) {
		if (FTBEWorldData.instance.warps.isEmpty()) {
			source.sendSuccess(new TranslatableComponent("warp_command_message.ftbessentials.warp_none"), false);
			return 1;
		}

		TeleportPos origin = new TeleportPos(source.getLevel().dimension(), new BlockPos(source.getPosition()));

		for (Map.Entry<String, TeleportPos> entry : FTBEWorldData.instance.warps.entrySet()) {
			source.sendSuccess(new TextComponent(entry.getKey() + ": " + entry.getValue().distanceString(origin)), false);
		}

		return 1;
	}
}
