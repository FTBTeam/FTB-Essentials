package dev.ftb.mods.ftbessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Random;

/**
 * @author LatvianModder
 */
public class TPACommands {
	public record TPARequest(String id, FTBEPlayerData source, FTBEPlayerData target, boolean here, long created) {
	}

	public static final HashMap<String, TPARequest> REQUESTS = new HashMap<>();

	public static TPARequest create(FTBEPlayerData source, FTBEPlayerData target, boolean here) {
		String key;

		do {
			key = String.format("%08X", new Random().nextInt());
		}
		while (REQUESTS.containsKey(key));

		TPARequest r = new TPARequest(key, source, target, here, System.currentTimeMillis());
		REQUESTS.put(key, r);
		return r;
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		if (FTBEConfig.TPA.isEnabled()) {
			dispatcher.register(Commands.literal("tpa")
					.requires(FTBEConfig.TPA)
					.then(Commands.argument("target", EntityArgument.player())
							.executes(context -> tpa(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "target"), false))
					)
			);

			dispatcher.register(Commands.literal("tpahere")
					.requires(FTBEConfig.TPA)
					.then(Commands.argument("target", EntityArgument.player())
							.executes(context -> tpa(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "target"), true))
					)
			);

			dispatcher.register(Commands.literal("tpaccept")
					.requires(FTBEConfig.TPA)
					.then(Commands.argument("id", StringArgumentType.string())
							.executes(context -> tpaccept(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "id")))
					)
			);

			dispatcher.register(Commands.literal("tpdeny")
					.requires(FTBEConfig.TPA)
					.then(Commands.argument("id", StringArgumentType.string())
							.executes(context -> tpdeny(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "id")))
					)
			);
		}
	}

	public static int tpa(ServerPlayer player, ServerPlayer target, boolean here) {
		FTBEPlayerData dataSource = FTBEPlayerData.get(player);
		FTBEPlayerData dataTarget = FTBEPlayerData.get(target);

		if (dataSource == null || dataTarget == null) {
			return 0;
		}

		if (REQUESTS.values().stream().anyMatch(r -> r.source == dataSource && r.target == dataTarget)) {
			player.displayClientMessage(new TranslatableComponent("tpa_command_message.ftbessentials.tpa_2request"), false);
			return 0;
		}

		TeleportPos.TeleportResult result = here ?
				dataTarget.tpaTeleporter.checkCooldown() :
				dataSource.tpaTeleporter.checkCooldown();

		if (!result.isSuccess()) {
			return result.runCommand(player);
		}

		TPARequest request = create(dataSource, dataTarget, here);

		TranslatableComponent component = new TranslatableComponent("tpa_command_message.ftbessentials.tpa_request[");
		component.append((here ? target : player).getDisplayName().copy().withStyle(ChatFormatting.YELLOW));
		component.append(" \u27A1 ");
		component.append((here ? player : target).getDisplayName().copy().withStyle(ChatFormatting.YELLOW));
		component.append(" ]");

		TranslatableComponent component2 = new TranslatableComponent("tpa_command_message.ftbessentials.tpa_tip");
		component2
				.append(new TextComponent(new TranslatableComponent("tpa_command_message.ftbessentials.tpa_accept").getString()+ " \u2714").setStyle(Style.EMPTY
				.applyFormat(ChatFormatting.GREEN)
				.withBold(true)
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + request.id))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent("tpa_command_message.ftbessentials.tpa_accept_info")))
		));

		component2.append(" | ");

		component2
				.append(new TextComponent(new TranslatableComponent("tpa_command_message.ftbessentials.tpa_deny").getString() + " \u274C")
						.setStyle(Style.EMPTY
								.applyFormat(ChatFormatting.RED)
								.withBold(true)
								.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny " + request.id))
								.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent("tpa_command_message.ftbessentials.tpa_deny_info")))
		));

		component2.append(" |");

		target.displayClientMessage(component, false);
		target.displayClientMessage(component2, false);

		player.displayClientMessage(new TranslatableComponent("tpa_command_message.ftbessentials.tpa_request"), false);
		return 1;
	}

	public static int tpaccept(ServerPlayer player, String id) {
		TPARequest request = REQUESTS.get(id);

		if (request == null) {
			player.displayClientMessage(new TranslatableComponent("tpa_command_message.ftbessentials.tpa_request_invalid"), false);
			return 0;
		}

		ServerPlayer sourcePlayer = player.server.getPlayerList().getPlayer(request.source.uuid);

		if (sourcePlayer == null) {
			player.displayClientMessage(new TranslatableComponent("tpa_command_message.ftbessentials.tpa_request_offline"), false);
			return 0;
		}

		TeleportPos.TeleportResult result = request.here ?
				request.target.tpaTeleporter.teleport(player, p -> new TeleportPos(sourcePlayer)) :
				request.source.tpaTeleporter.teleport(sourcePlayer, p -> new TeleportPos(player));

		if (result.isSuccess()) {
			REQUESTS.remove(request.id);
		}

		return result.runCommand(player);
	}

	public static int tpdeny(ServerPlayer player, String id) {
		TPARequest request = REQUESTS.get(id);

		if (request == null) {
			player.displayClientMessage(new TranslatableComponent("tpa_command_message.ftbessentials.tpa_request_invalid"), false);
			return 0;
		}

		REQUESTS.remove(request.id);

		player.displayClientMessage(new TranslatableComponent("tpa_command_message.ftbessentials.tpa_request_deny"), false);

		ServerPlayer player2 = player.server.getPlayerList().getPlayer(request.target.uuid);

		if (player2 != null) {
			player2.displayClientMessage(new TranslatableComponent("tpa_command_message.ftbessentials.tpa_request_offline"), false);
		}

		return 1;
	}
}
