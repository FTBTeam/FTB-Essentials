package dev.ftb.mods.ftbessentials.commands.impl.teleporting;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbessentials.api.records.TPARequest;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class TPACommand implements FTBCommand {
    private static final HashMap<UUID, TPARequest> REQUESTS = new HashMap<>();

    @Override
    public boolean enabled() {
        return FTBEConfig.TPA.isEnabled();
    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> register() {
        return List.of(
                Commands.literal("tpa")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> tpa(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "target"), false))
                        ),
                Commands.literal("tpahere")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> tpa(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "target"), true))
                        ),
                Commands.literal("tpaccept")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(context -> tpaccept(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "id")))
                        ),
                Commands.literal("tpdeny")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(context -> tpdeny(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "id")))
                        )
        );
    }

    public int tpa(ServerPlayer player, ServerPlayer target, boolean here) {
        FTBEPlayerData dataSource = FTBEPlayerData.getOrCreate(player).orElse(null);
        FTBEPlayerData dataTarget = FTBEPlayerData.getOrCreate(target).orElse(null);

        if (dataSource == null || dataTarget == null) {
            return 0;
        }

        if (REQUESTS.values().stream().anyMatch(r -> r.source() == dataSource && r.target() == dataTarget)) {
            player.displayClientMessage(Component.translatable("ftbessentials.tpa.already_sent"), false);
            return 0;
        }

        TeleportPos.TeleportResult result = here ?
                dataTarget.tpaTeleporter.checkCooldown(target) :
                dataSource.tpaTeleporter.checkCooldown(player);

        if (!result.isSuccess()) {
            return result.runCommand(player);
        }

        TPARequest request = create(dataSource, dataTarget, here);

        Component line1 = Component.translatable("ftbessentials.tpa.notify",
                (here ? target : player).getDisplayName().copy().withStyle(ChatFormatting.YELLOW),
                (here ? player : target).getDisplayName().copy().withStyle(ChatFormatting.YELLOW)
        );

        MutableComponent line2 = Component.translatable("ftbessentials.tpa.click_one");
        line2.append(Component.translatable("ftbessentials.tpa.accept").setStyle(Style.EMPTY
                .applyFormat(ChatFormatting.GREEN)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + request.id()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("ftbessentials.tpa.accept.tooltip")))
        ));

        line2.append(" | ");

        line2.append(Component.translatable("ftbessentials.tpa.deny").setStyle(Style.EMPTY
                .applyFormat(ChatFormatting.RED)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny " + request.id()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("ftbessentials.tpa.deny.tooltip")))
        ));

        line2.append(" |");

        target.displayClientMessage(line1, false);
        target.displayClientMessage(line2, false);

        player.displayClientMessage(Component.translatable("ftbessentials.tpa.request_sent"), false);
        return 1;
    }

    private static final Component INVALID_REQUEST = Component.translatable("ftbessentials.tpa.invalid_request").withStyle(ChatFormatting.RED);

    public int tpaccept(ServerPlayer player, String id) {

        var uuid = attemptUuid(id);
        if (uuid == null) {
            player.displayClientMessage(INVALID_REQUEST, false);
            return 0;
        }

        TPARequest request = REQUESTS.get(uuid);

        if (request == null) {
            player.displayClientMessage(INVALID_REQUEST, false);
            return 0;
        }

        ServerPlayer sourcePlayer = player.server.getPlayerList().getPlayer(request.source().getUuid());

        if (sourcePlayer == null) {
            player.displayClientMessage(Component.translatable("ftbessentials.tpa.gone_offline").withStyle(ChatFormatting.GOLD), false);
            return 0;
        }

        TeleportPos.TeleportResult result = request.here() ?
                request.target().tpaTeleporter.teleport(player, p -> new TeleportPos(sourcePlayer)) :
                request.source().tpaTeleporter.teleport(sourcePlayer, p -> new TeleportPos(player));

        if (result.isSuccess()) {
            REQUESTS.remove(request.id());
        }

        return result.runCommand(player);
    }

    public int tpdeny(ServerPlayer player, String id) {
        var uuid = attemptUuid(id);
        if (uuid == null) {
            player.displayClientMessage(INVALID_REQUEST, false);
            return 0;
        }

        TPARequest request = REQUESTS.get(uuid);
        if (request == null) {
            player.displayClientMessage(INVALID_REQUEST, false);
            return 0;
        }

        REQUESTS.remove(request.id());

        player.displayClientMessage(Component.translatable("ftbessentials.tpa.denied"), false);

        ServerPlayer player2 = player.server.getPlayerList().getPlayer(request.target().getUuid());

        if (player2 != null) {
            player2.displayClientMessage(Component.translatable("ftbessentials.tpa.denied"), false);
        }

        return 1;
    }

    public static TPARequest create(FTBEPlayerData source, FTBEPlayerData target, boolean here) {
        var uuid = UUID.randomUUID();
        TPARequest r = new TPARequest(uuid, source, target, here, System.currentTimeMillis());
        REQUESTS.put(uuid, r);
        return r;
    }

    public static void clearRequests() {
        REQUESTS.clear();
    }

    @Nullable
    private UUID attemptUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static HashMap<UUID, TPARequest> requests() {
        return REQUESTS;
    }
}
