package dev.ftb.mods.ftbessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.HashMap;
import java.util.Random;

/**
 * @author LatvianModder
 */
public class TPACommands {
	public static class TPARequest {
		public final String id;
		public MinecraftServer server;
		public FTBEPlayerData source;
		public FTBEPlayerData target;
		public boolean here;
		public long created;

		public TPARequest(String s) {
			id = s;
		}
	}

	public static final HashMap<String, TPARequest> REQUESTS = new HashMap<>();

	public static TPARequest create(MinecraftServer server, FTBEPlayerData source, FTBEPlayerData target, boolean here) {
		String key;

		do {
			key = String.format("%08X", new Random().nextInt());
		}
		while (REQUESTS.containsKey(key));

		TPARequest r = new TPARequest(key);
		r.server = server;
		r.source = source;
		r.target = target;
		r.here = here;
		r.created = System.currentTimeMillis();
		REQUESTS.put(key, r);
		return r;
	}

	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(Commands.literal("tpa")
				.then(Commands.argument("target", EntityArgument.player())
						.executes(context -> tpa(context.getSource().asPlayer(), EntityArgument.getPlayer(context, "target"), false))
				)
		);

		dispatcher.register(Commands.literal("tpahere")
				.then(Commands.argument("target", EntityArgument.player())
						.executes(context -> tpa(context.getSource().asPlayer(), EntityArgument.getPlayer(context, "target"), true))
				)
		);

		dispatcher.register(Commands.literal("tpaccept")
				.then(Commands.argument("id", StringArgumentType.string())
						.executes(context -> tpaccept(context.getSource().asPlayer(), StringArgumentType.getString(context, "id")))
				)
		);

		dispatcher.register(Commands.literal("tpdeny")
				.then(Commands.argument("id", StringArgumentType.string())
						.executes(context -> tpdeny(context.getSource().asPlayer(), StringArgumentType.getString(context, "id")))
				)
		);
	}

	public static int tpa(ServerPlayerEntity player, ServerPlayerEntity target, boolean here) {
		FTBEPlayerData dataSource = FTBEPlayerData.get(player);
		FTBEPlayerData dataTarget = FTBEPlayerData.get(target);

		if (REQUESTS.values().stream().anyMatch(r -> r.source == dataSource && r.target == dataTarget)) {
			player.sendStatusMessage(new StringTextComponent("Request already sent!"), false);
			return 0;
		}

		TPARequest request = create(player.server, dataSource, dataTarget, here);

		TeleportPos.TeleportResult result = (here ? dataTarget : dataSource).tpaTeleporter.checkCooldown();

		if (!result.isSuccess()) {
			return result.runCommand(player);
		}

		StringTextComponent component = new StringTextComponent("TPA request! [ ");
		component.append((here ? target : player).getDisplayName().deepCopy().mergeStyle(TextFormatting.YELLOW));
		component.appendString(" \u27A1 ");
		component.append((here ? player : target).getDisplayName().deepCopy().mergeStyle(TextFormatting.YELLOW));
		component.appendString(" ]");

		StringTextComponent component2 = new StringTextComponent("Click one of these: ");
		component2.append(new StringTextComponent("Accept \u2714").setStyle(Style.EMPTY
				.setColor(Color.fromTextFormatting(TextFormatting.GREEN))
				.setBold(true)
				.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + request.id))
				.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Click to Accept")))
		));

		component2.appendString(" | ");

		component2.append(new StringTextComponent("Deny \u274C").setStyle(Style.EMPTY
				.setColor(Color.fromTextFormatting(TextFormatting.RED))
				.setBold(true)
				.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny " + request.id))
				.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Click to Deny")))
		));

		component2.appendString(" |");

		target.sendStatusMessage(component, false);
		target.sendStatusMessage(component2, false);

		player.sendStatusMessage(new StringTextComponent("Request sent!"), false);
		return 1;
	}

	public static int tpaccept(ServerPlayerEntity player, String id) {
		TPARequest request = REQUESTS.get(id);

		if (request == null) {
			player.sendStatusMessage(new StringTextComponent("Invalid request!"), false);
			return 0;
		}

		ServerPlayerEntity sourcePlayer = player.server.getPlayerList().getPlayerByUUID(request.source.uuid);

		if (sourcePlayer == null) {
			player.sendStatusMessage(new StringTextComponent("Player has gone offline!"), false);
			return 0;
		}

		TeleportPos.TeleportResult result = (request.here ? request.target : request.source).tpaTeleporter.teleport(request.here ? player : sourcePlayer, p -> new TeleportPos(request.here ? sourcePlayer : player));

		if (result.isSuccess()) {
			REQUESTS.remove(request.id);
		}

		return result.runCommand(player);
	}

	public static int tpdeny(ServerPlayerEntity player, String id) {
		TPARequest request = REQUESTS.get(id);

		if (request == null) {
			player.sendStatusMessage(new StringTextComponent("Invalid request!"), false);
			return 0;
		}

		REQUESTS.remove(request.id);

		player.sendStatusMessage(new StringTextComponent("Request denied!"), false);

		ServerPlayerEntity player2 = player.server.getPlayerList().getPlayerByUUID(request.target.uuid);

		if (player2 != null) {
			player2.sendStatusMessage(new StringTextComponent("Request denied!"), false);
		}

		return 1;
	}
}
