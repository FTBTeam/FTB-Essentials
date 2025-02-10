package dev.ftb.mods.ftbessentials.commands.impl.kit;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.ftb.mods.ftbessentials.commands.CommandUtils;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.kit.Kit;
import dev.ftb.mods.ftbessentials.kit.KitManager;
import dev.ftb.mods.ftbessentials.util.BlockUtil;
import dev.ftb.mods.ftbessentials.util.DurationInfo;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.InventoryUtil;
import dev.ftb.mods.ftblibrary.util.TimeUtils;
import joptsimple.internal.Strings;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class KitCommand implements FTBCommand {
    public static final SimpleCommandExceptionType NOT_LOOKING_AT_BLOCK
            = new SimpleCommandExceptionType(Component.translatable("ftbessentials.kit.not_looking_at_block"));
    public static final SimpleCommandExceptionType NO_ITEMS_TO_ADD
            = new SimpleCommandExceptionType(Component.translatable("ftbessentials.kit.no_items"));
    public static final SimpleCommandExceptionType NOT_ENOUGH_SPACE
            = new SimpleCommandExceptionType(Component.translatable("ftbessentials.kit.not_enough_space"));
    public static final DynamicCommandExceptionType NO_SUCH_KIT
            = new DynamicCommandExceptionType(kitName -> Component.translatable("ftbessentials.kit.no_such_kit", kitName));
    public static final DynamicCommandExceptionType NO_KIT_PERMISSION
            = new DynamicCommandExceptionType(kitName -> Component.translatable("ftbessentials.kit.no_permission", kitName));
    public static final DynamicCommandExceptionType UNKNOWN_PLAYER_ID
            = new DynamicCommandExceptionType(playerId -> Component.translatable("ftbessentials.unknown_player_id", playerId));
    public static final Dynamic2CommandExceptionType ONE_TIME_ONLY
            = new Dynamic2CommandExceptionType((kitName, playerName) -> Component.translatable("ftbessentials.kit.one_time_only", kitName, playerName));
    public static final Dynamic2CommandExceptionType ON_COOLDOWN
            = new Dynamic2CommandExceptionType((kitName, remaining) -> Component.translatable("ftbessentials.kit.on_cooldown", kitName, remaining));
    public static final DynamicCommandExceptionType ALREADY_EXISTS
            = new DynamicCommandExceptionType(kitName -> Component.translatable("ftbessentials.kit.already_exists", kitName));

    @Override
    public boolean enabled() {
        return FTBEConfig.KIT.isEnabled();
    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> register() {
        return List.of(literal("kit")
                .requires(CommandUtils.isGamemaster())
                .then(literal("create_from_player_inv")
                        .then(argument("name", StringArgumentType.word())
                                .executes(ctx -> createKitFromPlayer(ctx.getSource(), StringArgumentType.getString(ctx, "name"), "", false))
                                .then(argument("cooldown", StringArgumentType.greedyString())
                                        .suggests((ctx, builder) -> CommandUtils.suggestCooldowns(builder))
                                        .executes(ctx -> createKitFromPlayer(ctx.getSource(), StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "cooldown"), false))
                                )
                        )
                )
                .then(literal("create_from_player_hotbar")
                        .then(argument("name", StringArgumentType.word())
                                .executes(ctx -> createKitFromPlayer(ctx.getSource(), StringArgumentType.getString(ctx, "name"), "", true))
                                .then(argument("cooldown", StringArgumentType.greedyString())
                                        .suggests((ctx, builder) -> CommandUtils.suggestCooldowns(builder))
                                        .executes(ctx -> createKitFromPlayer(ctx.getSource(), StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "cooldown"), true))
                                )
                        )
                )
                .then(literal("create_from_block_inv")
                        .then(argument("name", StringArgumentType.word())
                                .executes(ctx -> createKitFromBlock(ctx.getSource(), StringArgumentType.getString(ctx, "name"), ""))
                                .then(argument("cooldown", StringArgumentType.greedyString())
                                        .suggests((ctx, builder) -> CommandUtils.suggestCooldowns(builder))
                                        .executes(ctx -> createKitFromBlock(ctx.getSource(), StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "cooldown")))
                                )
                        )
                )
                .then(literal("delete")
                        .then(argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestKits(builder))
                                .executes(ctx -> deleteKit(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                )
                .then(literal("list").executes(ctx -> listKits(ctx.getSource())))
                .then(literal("show")
                        .then(argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestKits(builder))
                                .executes(ctx -> showKit(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                )
                .then(literal("give")
                        .then(argument("players", EntityArgument.players())
                                .then(argument("name", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestKits(builder))
                                        .executes(ctx -> giveKit(ctx.getSource(), StringArgumentType.getString(ctx, "name"), EntityArgument.getPlayers(ctx, "players")))
                                )
                        )
                )
                .then(literal("put_in_block_inv")
                        .then(argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestKits(builder))
                                .executes(ctx -> putKitInBlockInv(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                )
                .then(literal("cooldown")
                        .then(argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestKits(builder))
                                .then(argument("cooldown", StringArgumentType.greedyString())
                                        .suggests((ctx, builder) -> CommandUtils.suggestCooldowns(builder))
                                        .executes(ctx -> modifyCooldown(ctx.getSource(), StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "cooldown")))
                                )
                        )
                )
                .then(literal("reset_cooldown")
                        .then(argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestKits(builder))
                                .executes(ctx -> resetCooldowns(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                                .then(argument("player", EntityArgument.player())
                                        .executes(ctx -> resetCooldowns(ctx.getSource(), StringArgumentType.getString(ctx, "name"), EntityArgument.getPlayer(ctx, "player")))
                                )
                                .then(argument("id", UuidArgument.uuid())
                                        .executes(ctx -> resetCooldowns(ctx.getSource(), StringArgumentType.getString(ctx, "name"), UuidArgument.getUuid(ctx, "id")))
                                )
                        )
                )
                .then(literal("set_autogrant")
                        .then(argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestKits(builder))
                                .then(argument("grant", BoolArgumentType.bool())
                                        .executes(ctx -> modifyAutogrant(ctx.getSource(), StringArgumentType.getString(ctx, "name"), BoolArgumentType.getBool(ctx, "grant")))
                                )
                        )
                )
        );
    }

    private static int putKitInBlockInv(CommandSourceStack source, String kitName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockHitResult res = BlockUtil.getFocusedBlock(player, 5.5d)
                .orElseThrow(NOT_LOOKING_AT_BLOCK::create);
        Kit kit = KitManager.getInstance().get(kitName).orElseThrow(() -> NO_SUCH_KIT.create(kitName));

        if (!InventoryUtil.putItemsInInventory(kit.getItems(), player.level(), res.getBlockPos(), res.getDirection())) {
            throw NOT_ENOUGH_SPACE.create();
        }

        source.sendSuccess(() -> Component.translatable("ftbessentials.kit.added_items", kitName).withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestKits(SuggestionsBuilder builder) {
        return suggestKits(null, builder);
    }

    static CompletableFuture<Suggestions> suggestKits(@Nullable ServerPlayer player, SuggestionsBuilder builder) {
        List<String> list = KitManager.getInstance().allKits().stream()
                .filter(kit -> kit.playerCanGetKit(player))
                .map(Kit::getKitName)
                .toList();
        return SharedSuggestionProvider.suggest(list, builder);
    }

    private static int createKitFromPlayer(CommandSourceStack source, String name, String cooldown, boolean hotbarOnly) throws CommandSyntaxException {
        long secs = DurationInfo.getSeconds(cooldown);
        KitManager.getInstance().createFromPlayerInv(name, source.getPlayerOrException(), secs, hotbarOnly);
        source.sendSuccess(() -> Component.translatable("ftbessentials.kit.created", name).withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int createKitFromBlock(CommandSourceStack source, String name, String cooldown) throws CommandSyntaxException {
        long secs = DurationInfo.getSeconds(cooldown);
        ServerPlayer player = source.getPlayerOrException();
        BlockHitResult res = BlockUtil.getFocusedBlock(player, 5.5d).orElseThrow(NOT_LOOKING_AT_BLOCK::create);

        KitManager.getInstance().createFromBlockInv(name, player.level(), res.getBlockPos(), res.getDirection(), secs);
        source.sendSuccess(() -> Component.translatable("ftbessentials.kit.created", name).withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    static int giveKit(CommandSourceStack source, String name, Collection<ServerPlayer> players) throws CommandSyntaxException {
        if (!source.hasPermission(Commands.LEVEL_GAMEMASTERS) && players.size() == 1 && source.getPlayer() != null && players.contains(source.getPlayer()) ) {
            // giving to self and not an admin; also check the `ftbessentials.give_me_kit.<kitname>` node
            if (!Kit.checkPermissionNode(source.getPlayer(), name)) {
                throw NO_KIT_PERMISSION.create(name);
            }
        }
        for (ServerPlayer player : players) {
            KitManager.getInstance().giveKitToPlayer(name, player);
        }
        source.sendSuccess(() -> Component.translatable("ftbessentials.kit.gave_to_players", name, players.size()).withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int listKits(CommandSourceStack source) {
        Collection<Kit> kits = KitManager.getInstance().allKits();

        source.sendSuccess(() -> Component.translatable("ftbessentials.kit.count", kits.size()).withStyle(ChatFormatting.AQUA), false);
        kits.stream().sorted(Comparator.comparing(Kit::getKitName))
                .forEach(kit -> source.sendSuccess(() -> Component.literal("• " + kit.getKitName()).withStyle(Style.EMPTY
                        .withColor(ChatFormatting.YELLOW)
                        .withClickEvent(new ClickEvent(Action.RUN_COMMAND, "/kit show " + kit.getKitName()))
                ), false));
        return 1;
    }

    private static int showKit(CommandSourceStack source, String kitName) throws CommandSyntaxException {
        Kit kit = KitManager.getInstance().get(kitName).orElseThrow(() -> NO_SUCH_KIT.create(kitName));

        source.sendSuccess(() -> Component.literal(Strings.repeat('-', 40)).withStyle(ChatFormatting.GREEN), false);
        source.sendSuccess(() -> Component.translatable("ftbessentials.kit_name",
                Component.literal(kit.getKitName()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.AQUA), false);
        if (kit.getCooldown() > 0L) {
            source.sendSuccess(() -> Component.literal("  ")
                    .append(Component.translatable("ftbessentials.kit.cooldown",
                            Component.literal(TimeUtils.prettyTimeString(kit.getCooldown())).withStyle(ChatFormatting.YELLOW))
                    ).withStyle(ChatFormatting.AQUA), false);
        } else if (kit.getCooldown() == 0L) {
            source.sendSuccess(() -> Component.literal("  ")
                    .append(Component.translatable("ftbessentials.kit.cooldown.none")).withStyle(ChatFormatting.AQUA), false);
        } else {
            source.sendSuccess(() -> Component.literal("  ")
                    .append(Component.translatable("ftbessentials.kit.one_time")).withStyle(ChatFormatting.AQUA), false);
        }
        if (kit.isAutoGrant()) {
            source.sendSuccess(() -> Component.literal("  ")
                    .append(Component.translatable("ftbessentials.kit.autogranted")).withStyle(ChatFormatting.AQUA), false);
        }
        source.sendSuccess(() -> Component.literal("  ")
                .append(Component.translatable("ftbessentials.kit.items")).withStyle(ChatFormatting.AQUA), false);
        for (ItemStack stack : kit.getItems()) {
            source.sendSuccess(()-> Component.literal("  • ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(stack.getCount() + " x ").withStyle(ChatFormatting.WHITE))
                    .append(stack.getDisplayName()), false);
        }

        return 1;
    }

    private static int deleteKit(CommandSourceStack source, String kitName) throws CommandSyntaxException {
        KitManager.getInstance().get(kitName).orElseThrow(() -> NO_SUCH_KIT.create(kitName));
        KitManager.getInstance().deleteKit(kitName);
        source.sendSuccess(() -> Component.translatable("ftbessentials.kit.deleted", kitName).withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int modifyAutogrant(CommandSourceStack source, String kitName, boolean grant) throws CommandSyntaxException {
        Kit kit = KitManager.getInstance().get(kitName).orElseThrow(() -> NO_SUCH_KIT.create(kitName));

        KitManager.getInstance().addKit(kit.withAutoGrant(grant), true);
        source.sendSuccess(() -> Component.translatable("ftbessentials.kit.autogrant_modified", kitName, grant).withStyle(ChatFormatting.YELLOW), false);

        return 1;
    }

    private static int modifyCooldown(CommandSourceStack source, String kitName, String cooldown) throws CommandSyntaxException {
        Kit kit = KitManager.getInstance().get(kitName).orElseThrow(() -> NO_SUCH_KIT.create(kitName));

        long secs = DurationInfo.getSeconds(cooldown);
        KitManager.getInstance().addKit(kit.withCooldown(secs), true);
        Component newTime = secs < 0 ? Component.translatable("ftbessentials.kit.one_time") : Component.literal(TimeUtils.prettyTimeString(secs));
        source.sendSuccess(() -> Component.translatable("ftbessentials.kit.cooldown_modified", kitName, newTime).withStyle(ChatFormatting.YELLOW), false);

        return 1;
    }


    private static int resetCooldowns(CommandSourceStack source, String name, ServerPlayer player) throws CommandSyntaxException {
        return resetCooldowns(source, name, player.getUUID());
    }

    private static int resetCooldowns(CommandSourceStack source, String kitName, UUID playerId) throws CommandSyntaxException {
        KitManager.getInstance().get(kitName).orElseThrow(() -> NO_SUCH_KIT.create(kitName));

        if (!FTBEPlayerData.playerExists(playerId)) {
            throw UNKNOWN_PLAYER_ID.create(playerId);
        }

        return FTBEPlayerData.getOrCreate(source.getServer(), playerId)
                .map(data -> {
                    data.setLastKitUseTime(kitName, 0L);
                    source.sendSuccess(() -> Component.translatable("ftbessentials.kit.cooldown_reset", kitName, playerId)
                            .withStyle(ChatFormatting.YELLOW), false);
                    return 1;
                }).orElse(0);
    }

    private static int resetCooldowns(CommandSourceStack source, String kitName) throws CommandSyntaxException {
        KitManager.getInstance().get(kitName).orElseThrow(() -> NO_SUCH_KIT.create(kitName));

        if (KitManager.getInstance().get(kitName).isPresent()) {
            FTBEPlayerData.cleanupKitCooldowns(kitName);
            source.sendSuccess(() -> Component.translatable("ftbessentials.kit.cooldown_reset_all", kitName).withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }

        return 0;
    }
}
