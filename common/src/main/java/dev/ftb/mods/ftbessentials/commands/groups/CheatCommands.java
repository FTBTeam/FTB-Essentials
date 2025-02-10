package dev.ftb.mods.ftbessentials.commands.groups;

import com.mojang.brigadier.context.CommandContext;
import dev.ftb.mods.ftbessentials.FTBEssentialsPlatform;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.commands.SimpleCommandPlayer;
import dev.ftb.mods.ftbessentials.commands.impl.cheat.SpeedCommand;
import dev.ftb.mods.ftbessentials.commands.impl.cheat.VirtualInventoryCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;

import java.util.List;

public class CheatCommands {
    public static final List<FTBCommand> COMMANDS = List.of(
            // Heal command
            SimpleCommandPlayer.create("heal", Commands.LEVEL_GAMEMASTERS, FTBEConfig.HEAL, (ctx, player) -> heal(player)),

            // Feed command
            SimpleCommandPlayer.create("feed", Commands.LEVEL_GAMEMASTERS, FTBEConfig.FEED, (ctx, player) -> player.getFoodData().eat(40, 40F)),

            // Extinguish command
            SimpleCommandPlayer.create("extinguish", Commands.LEVEL_GAMEMASTERS, FTBEConfig.EXTINGUISH, (ctx, player) -> player.clearFire()),

            // Fly command
            SimpleCommandPlayer.create("fly", Commands.LEVEL_GAMEMASTERS, FTBEConfig.FLY, (ctx, player) -> fly(player)),

            // God command
            SimpleCommandPlayer.create("god", Commands.LEVEL_GAMEMASTERS, FTBEConfig.GOD, (ctx, player) -> god(player)),

            // Speed command
            new SpeedCommand(),

            // Virtual inventory's
            new VirtualInventoryCommand(),

            // Enderchest
            new SimpleCommandPlayer("enderchest", Commands.LEVEL_GAMEMASTERS, FTBEConfig.ENDER_CHEST, CheatCommands::enderChest)
    );

    private static void enderChest(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        MutableComponent title = Component.translatable("container.enderchest");
        ServerPlayer srcPlayer = ctx.getSource().getPlayer();
        if (player != null && srcPlayer != null) {
            title.append(" × ").append(player.getDisplayName());
            srcPlayer.openMenu(new SimpleMenuProvider((i, inv, p) -> ChestMenu.threeRows(i, inv, player.getEnderChestInventory()), title));
        } else {
            ctx.getSource().sendFailure(Component.translatable("ftbessentials.enderchest.unable"));
        }
    }

    public static void heal(ServerPlayer player) {
        player.setHealth(player.getMaxHealth());
        player.getFoodData().eat(40, 40F);
        player.clearFire();
        FTBEssentialsPlatform.curePotionEffects(player);
    }

    private static void fly(ServerPlayer player) {
        FTBEPlayerData.getOrCreate(player).ifPresent(data -> {
            var abilities = player.getAbilities();

            if (data.canFly()) {
                data.setCanFly(false);
                if (player.gameMode.isSurvival()) {
                    abilities.mayfly = false;
                    abilities.flying = false;
                }
                player.displayClientMessage(Component.translatable("ftbessentials.flight.disabled"), true);
            } else {
                data.setCanFly(true);
                abilities.mayfly = true;
                player.displayClientMessage(Component.translatable("ftbessentials.flight.enabled"), true);
            }

            player.onUpdateAbilities();
        });
    }

    private static void god(ServerPlayer player) {
        FTBEPlayerData.getOrCreate(player).ifPresent(data -> {
            var abilities = player.getAbilities();

            if (data.isGod()) {
                data.setGod(false);
                abilities.invulnerable = false;
                player.displayClientMessage(Component.translatable("ftbessentials.god_mode.disabled"), true);
            } else {
                data.setGod(true);
                abilities.invulnerable = true;
                player.displayClientMessage(Component.translatable("ftbessentials.god_mode.enabled"), true);
            }

            player.onUpdateAbilities();
        });
    }
}
