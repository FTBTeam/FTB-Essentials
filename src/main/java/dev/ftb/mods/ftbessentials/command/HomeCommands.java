package dev.ftb.mods.ftbessentials.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.ftb.mods.ftbessentials.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;

import java.util.Map;

/**
 * @author LatvianModder
 */
public class HomeCommands {
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(Commands.literal("home")
				.executes(context -> home(context.getSource().asPlayer(), "home"))
				.then(Commands.argument("name", StringArgumentType.greedyString())
						.executes(context -> home(context.getSource().asPlayer(), StringArgumentType.getString(context, "name")))
				)
		);

		dispatcher.register(Commands.literal("sethome")
				.executes(context -> sethome(context.getSource().asPlayer(), "home"))
				.then(Commands.argument("name", StringArgumentType.greedyString())
						.executes(context -> sethome(context.getSource().asPlayer(), StringArgumentType.getString(context, "name")))
				)
		);

		dispatcher.register(Commands.literal("delhome")
				.executes(context -> delhome(context.getSource().asPlayer(), "home"))
				.then(Commands.argument("name", StringArgumentType.greedyString())
						.executes(context -> delhome(context.getSource().asPlayer(), StringArgumentType.getString(context, "name")))
				)
		);

		dispatcher.register(Commands.literal("listhomes")
				.executes(context -> listhomes(context.getSource(), context.getSource().asPlayer().getGameProfile()))
				.then(Commands.argument("player", GameProfileArgument.gameProfile())
						.requires(source -> source.getServer().isSinglePlayer() || source.hasPermissionLevel(2))
						.executes(context -> listhomes(context.getSource(), GameProfileArgument.getGameProfiles(context, "player").iterator().next()))
				)
		);
	}

	public static int home(ServerPlayerEntity player, String name) {
		FTBEPlayerData data = FTBEPlayerData.get(player);
		TeleportPos pos = data.homes.get(name.toLowerCase());

		if (pos == null) {
			player.sendStatusMessage(new StringTextComponent("Home not found!"), false);
			return 0;
		}

		return data.homeTeleporter.teleport(player, p -> pos).runCommand(player);
	}

	public static int sethome(ServerPlayerEntity player, String name) {
		FTBEPlayerData data = FTBEPlayerData.get(player);

		if (data.homes.size() >= FTBEConfig.getMaxHomes(player) && !data.homes.containsKey(name.toLowerCase())) {
			player.sendStatusMessage(new StringTextComponent("Can't add any more homes!"), false);
			return 0;
		}

		data.homes.put(name.toLowerCase(), new TeleportPos(player));
		data.save();
		player.sendStatusMessage(new StringTextComponent("Home set!"), false);
		return 1;
	}

	public static int delhome(ServerPlayerEntity player, String name) {
		FTBEPlayerData data = FTBEPlayerData.get(player);

		if (data.homes.remove(name.toLowerCase()) != null) {
			data.save();
			player.sendStatusMessage(new StringTextComponent("Home deleted!"), false);
			return 1;
		} else {
			player.sendStatusMessage(new StringTextComponent("Home not found!"), false);
			return 0;
		}
	}

	public static int listhomes(CommandSource source, GameProfile of) {
		FTBEPlayerData data = FTBEPlayerData.get(of);

		if (data.homes.isEmpty()) {
			source.sendFeedback(new StringTextComponent("None"), false);
			return 1;
		}

		TeleportPos origin = new TeleportPos(source.getWorld().getDimensionKey(), new BlockPos(source.getPos()));

		for (Map.Entry<String, TeleportPos> entry : data.homes.entrySet()) {
			source.sendFeedback(new StringTextComponent(entry.getKey() + ": " + entry.getValue().distanceString(origin)), false);
		}

		return 1;
	}
}
