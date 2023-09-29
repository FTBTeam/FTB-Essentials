package dev.ftb.mods.ftbessentials.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
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

import java.util.Collection;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class KitCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (FTBEConfig.KIT.isEnabled()) {
            dispatcher.register(literal("kit")
                    .requires(FTBEConfig.KIT.enabledAndOp())
                    .then(literal("create_from_player_inv")
                            .then(argument("name", StringArgumentType.word())
                                    .executes(ctx -> createKitFromPlayer(ctx.getSource(), StringArgumentType.getString(ctx, "name"), "", false))
                                    .then(argument("cooldown", StringArgumentType.greedyString())
                                            .suggests((ctx, builder) -> FTBEssentialsCommands.suggestCooldowns(builder))
                                            .executes(ctx -> createKitFromPlayer(ctx.getSource(), StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "cooldown"), false))
                                    )
                            )
                    )
                    .then(literal("create_from_player_hotbar")
                            .then(argument("name", StringArgumentType.word())
                                    .executes(ctx -> createKitFromPlayer(ctx.getSource(), StringArgumentType.getString(ctx, "name"), "", true))
                                    .then(argument("cooldown", StringArgumentType.greedyString())
                                            .suggests((ctx, builder) -> FTBEssentialsCommands.suggestCooldowns(builder))
                                            .executes(ctx -> createKitFromPlayer(ctx.getSource(), StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "cooldown"), true))
                                    )
                            )
                    )
                    .then(literal("create_from_block_inv")
                            .then(argument("name", StringArgumentType.word())
                                    .executes(ctx -> createKitFromBlock(ctx.getSource(), StringArgumentType.getString(ctx, "name"), ""))
                                    .then(argument("cooldown", StringArgumentType.greedyString())
                                            .suggests((ctx, builder) -> FTBEssentialsCommands.suggestCooldowns(builder))
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
                    .then(literal("list")
                            .executes(ctx -> listKits(ctx.getSource()))
                    )
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
                                            .suggests((ctx, builder) -> FTBEssentialsCommands.suggestCooldowns(builder))
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
    }

    private static int putKitInBlockInv(CommandSourceStack source, String kitName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            BlockHitResult res = BlockUtil.getFocusedBlock(player, 5.5d).orElseThrow(() -> new IllegalArgumentException("Not looking at a block"));
            Kit kit = KitManager.getInstance().get(kitName).orElseThrow(() -> new IllegalArgumentException("No such kit: " + kitName));
            if (!InventoryUtil.putItemsInInventory(kit.getItems(), player.level(), res.getBlockPos(), res.getDirection())) {
                throw new RuntimeException("Not enough space");
            }
            source.sendSuccess(() -> Component.literal("Added item(s) from kit '" + kitName + "' to focused inventory").withStyle(ChatFormatting.YELLOW), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Can't store kit in inventory: " + e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static CompletableFuture<Suggestions> suggestKits(SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(KitManager.getInstance().allKits().stream().map(Kit::getKitName).toList(), builder);
    }

    private static int createKitFromPlayer(CommandSourceStack source, String name, String cooldown, boolean hotbarOnly) {
        try {
            long secs = DurationInfo.getSeconds(cooldown);
            KitManager.getInstance().createFromPlayerInv(name, source.getPlayerOrException(), secs, hotbarOnly);
            source.sendSuccess(() -> Component.literal("Kit '" + name + "' created").withStyle(ChatFormatting.YELLOW), false);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Can't create kit: " + e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
        return 1;
    }

    private static int createKitFromBlock(CommandSourceStack source, String name, String cooldown) {
        try {
            long secs = DurationInfo.getSeconds(cooldown);
            ServerPlayer player = source.getPlayerOrException();
            BlockHitResult res = BlockUtil.getFocusedBlock(player, 5.5d).orElseThrow(() -> new IllegalArgumentException("Not looking at a block"));

            KitManager.getInstance().createFromBlockInv(name, player.level(), res.getBlockPos(), res.getDirection(), secs);
            source.sendSuccess(() -> Component.literal("Kit '" + name + "' created").withStyle(ChatFormatting.YELLOW), false);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Can't create kit: " + e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
        return 1;
    }

    private static int giveKit(CommandSourceStack source, String name, Collection<ServerPlayer> players) {
        try {
            players.forEach(player -> KitManager.getInstance().giveKitToPlayer(name, player));
            source.sendSuccess(() -> Component.literal("Kit '" + name + "' given to " + players.size() + " player(s)").withStyle(ChatFormatting.YELLOW), false);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Can't give kit to player: " + e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
        return 1;
    }

    private static int listKits(CommandSourceStack source) {
        Collection<Kit> kits = KitManager.getInstance().allKits();

        source.sendSuccess(() -> Component.literal(kits.size() + " kit(s)").withStyle(ChatFormatting.AQUA), false);
        kits.stream().sorted(Comparator.comparing(Kit::getKitName))
                .forEach(kit -> source.sendSuccess(() -> Component.literal("• " + kit.getKitName()).withStyle(Style.EMPTY
                        .withColor(ChatFormatting.YELLOW)
                        .withClickEvent(new ClickEvent(Action.RUN_COMMAND, "/kit show " + kit.getKitName()))
                ), false));
        return 1;
    }

    private static int showKit(CommandSourceStack source, String kitName) {
        KitManager.getInstance().get(kitName).ifPresentOrElse(kit -> {
            source.sendSuccess(() -> Component.literal(Strings.repeat('-', 40)).withStyle(ChatFormatting.GREEN), false);
            source.sendSuccess(() -> Component.literal("Kit Name: ").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(kit.getKitName()).withStyle(ChatFormatting.YELLOW)), false);
            if (kit.getCooldown() > 0L) {
                source.sendSuccess(() -> Component.literal("  Cooldown: ").withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(TimeUtils.prettyTimeString(kit.getCooldown())).withStyle(ChatFormatting.YELLOW)), false);
            } else if (kit.getCooldown() == 0L) {
                source.sendSuccess(() -> Component.literal("  No Cooldown").withStyle(ChatFormatting.AQUA), false);
            } else {
                source.sendSuccess(() -> Component.literal("  One-Time Use").withStyle(ChatFormatting.AQUA), false);
            }
            if (kit.isAutoGrant()) {
                source.sendSuccess(() -> Component.literal("  Autogranted on player login").withStyle(ChatFormatting.AQUA), false);
            }
            source.sendSuccess(() -> Component.literal("  Items:").withStyle(ChatFormatting.AQUA), false);
            for (ItemStack stack : kit.getItems()) {
                source.sendSuccess(()-> Component.literal("  • ").withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(stack.getCount() + " x ").withStyle(ChatFormatting.WHITE))
                        .append(stack.getDisplayName()), false);
            }
        }, () -> source.sendFailure(Component.literal("No such kit: " + kitName).withStyle(ChatFormatting.RED)));
        return 1;
    }

    private static int deleteKit(CommandSourceStack source, String kitName) {
        try {
            KitManager.getInstance().deleteKit(kitName);
            source.sendSuccess(() -> Component.literal("Kit '" + kitName + "' deleted").withStyle(ChatFormatting.YELLOW), false);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Can't delete kit '" + kitName + "': " + e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
        return 1;
    }

    private static int modifyAutogrant(CommandSourceStack source, String kitName, boolean grant) {
        KitManager.getInstance().get(kitName).ifPresentOrElse(kit -> {
            KitManager.getInstance().addKit(kit.withAutoGrant(grant), true);
            source.sendSuccess(() -> Component.literal("Kit '" + kitName + "' autogrant modified: " + grant).withStyle(ChatFormatting.YELLOW), false);
        }, () -> source.sendFailure(Component.literal("No such kit: " + kitName).withStyle(ChatFormatting.RED)));

        return 1;
    }

    private static int modifyCooldown(CommandSourceStack source, String kitName, String cooldown) {
        KitManager.getInstance().get(kitName).ifPresentOrElse(kit -> {
            long secs = DurationInfo.getSeconds(cooldown);
            KitManager.getInstance().addKit(kit.withCooldown(secs), true);
            String newTime = secs < 0 ? "One-Time Use" : TimeUtils.prettyTimeString(secs);
            source.sendSuccess(() -> Component.literal("Kit '" + kitName + "' cooldown modified: " + newTime).withStyle(ChatFormatting.YELLOW), false);
        }, () -> source.sendFailure(Component.literal("No such kit: " + kitName).withStyle(ChatFormatting.RED)));

        return 1;
    }


    private static int resetCooldowns(CommandSourceStack source, String name, ServerPlayer player) {
        return resetCooldowns(source, name, player.getUUID());
    }

    private static int resetCooldowns(CommandSourceStack source, String name, UUID playerId) {
        if (KitManager.getInstance().get(name).isEmpty()) {
            source.sendFailure(Component.literal("Unknown kit: " + name).withStyle(ChatFormatting.RED));
            return 0;
        }
        if (!FTBEPlayerData.playerExists(playerId)) {
            source.sendFailure(Component.literal("Unknown player ID: " + playerId).withStyle(ChatFormatting.RED));
            return 0;
        }

        FTBEPlayerData.getOrCreate(new GameProfile(playerId, "")).ifPresent(data -> {
            data.setLastKitUseTime(name, 0L);
            source.sendSuccess(() -> Component.literal("Cooldown for '" + name + "' reset for UUID " + playerId).withStyle(ChatFormatting.YELLOW), false);
        });

        return 1;
    }

    private static int resetCooldowns(CommandSourceStack source, String name) {
        if (KitManager.getInstance().get(name).isPresent()) {
            FTBEPlayerData.cleanupKitCooldowns(name);
            source.sendSuccess(() -> Component.literal("Cooldown for '" + name + "' reset for all players").withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }

        source.sendFailure(Component.literal("Unknown kit: " + name).withStyle(ChatFormatting.RED));

        return 0;
    }

}
