package dev.ftb.mods.ftbessentials.commands.groups;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.commands.SimpleCommandPlayer;
import dev.ftb.mods.ftbessentials.commands.impl.cheat.SpeedCommand;
import dev.ftb.mods.ftbessentials.commands.impl.cheat.VirtualInventoryCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;

import java.util.List;

public class CheatCommands {
    public static final List<FTBCommand> COMMANDS = List.of(
            SimpleCommandPlayer.create("heal", Permissions.COMMANDS_GAMEMASTER, FTBEConfig.HEAL, (ctx, player) -> heal(player)),
            SimpleCommandPlayer.create("feed", Permissions.COMMANDS_GAMEMASTER, FTBEConfig.FEED, (ctx, player) -> feed(player)),
            SimpleCommandPlayer.create("extinguish", Permissions.COMMANDS_GAMEMASTER, FTBEConfig.EXTINGUISH, (ctx, player) -> clearFire(player)),
            SimpleCommandPlayer.create("fly", Permissions.COMMANDS_GAMEMASTER, FTBEConfig.FLY, (ctx, player) -> fly(player)),
            SimpleCommandPlayer.create("god", Permissions.COMMANDS_GAMEMASTER, FTBEConfig.GOD, (ctx, player) -> god(player)),
            new SpeedCommand(),
            new VirtualInventoryCommand(),
            new SimpleCommandPlayer("enderchest", null, Permissions.COMMANDS_GAMEMASTER, FTBEConfig.ENDER_CHEST, CheatCommands::enderChest)
    );

    private static int enderChest(CommandContext<CommandSourceStack> ctx, ServerPlayer targetPlayer) {
        MutableComponent title = Component.translatable("container.enderchest");
        ServerPlayer srcPlayer = ctx.getSource().getPlayer();
        if (srcPlayer != null) {
            if (!targetPlayer.getUUID().equals(srcPlayer.getUUID())) {
                title.append(" × ").append(targetPlayer.getDisplayName());
            }
            srcPlayer.openMenu(new SimpleMenuProvider((i, inv, p) -> ChestMenu.threeRows(i, inv, targetPlayer.getEnderChestInventory()), title));
            return Command.SINGLE_SUCCESS;
        } else {
            ctx.getSource().sendFailure(Component.translatable("ftbessentials.enderchest.unable"));
            return 0;
        }
    }

    private static int clearFire(ServerPlayer player) {
        player.clearFire();
        return Command.SINGLE_SUCCESS;
    }

    private static int feed(ServerPlayer player) {
        player.getFoodData().eat(40, 40F);
        return Command.SINGLE_SUCCESS;
    }

    public static int heal(ServerPlayer targetPlayer) {
        targetPlayer.setHealth(targetPlayer.getMaxHealth());
        targetPlayer.getFoodData().eat(40, 40F);
        targetPlayer.clearFire();
        targetPlayer.removeAllEffects();
        return Command.SINGLE_SUCCESS;
    }

    private static int fly(ServerPlayer targetPlayer) {
        return FTBEPlayerData.getOrCreate(targetPlayer).map(data -> {
            var abilities = targetPlayer.getAbilities();

            if (data.canFly()) {
                data.setCanFly(false);
                if (targetPlayer.gameMode.isSurvival()) {
                    abilities.mayfly = false;
                    abilities.flying = false;
                }
                targetPlayer.sendOverlayMessage(Component.translatable("ftbessentials.flight.disabled"));
            } else {
                data.setCanFly(true);
                abilities.mayfly = true;
                targetPlayer.sendOverlayMessage(Component.translatable("ftbessentials.flight.enabled"));
            }

            targetPlayer.onUpdateAbilities();
            return Command.SINGLE_SUCCESS;
        }).orElse(0);
    }

    private static int god(ServerPlayer targetPlayer) {
        return FTBEPlayerData.getOrCreate(targetPlayer).map(data -> {
            var abilities = targetPlayer.getAbilities();

            if (data.isGod()) {
                data.setGod(false);
                abilities.invulnerable = false;
                targetPlayer.sendOverlayMessage(Component.translatable("ftbessentials.god_mode.disabled"));
            } else {
                data.setGod(true);
                abilities.invulnerable = true;
                targetPlayer.sendOverlayMessage(Component.translatable("ftbessentials.god_mode.enabled"));
            }

            targetPlayer.onUpdateAbilities();
            return Command.SINGLE_SUCCESS;
        }).orElse(0);
    }
}
