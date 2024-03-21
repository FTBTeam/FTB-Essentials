package dev.ftb.mods.ftbessentials.commands.groups;

import dev.ftb.mods.ftbessentials.FTBEssentialsPlatform;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.commands.SimpleCommandPlayer;
import dev.ftb.mods.ftbessentials.commands.SimpleConfigurableCommand;
import dev.ftb.mods.ftbessentials.commands.impl.cheat.SpeedCommand;
import dev.ftb.mods.ftbessentials.commands.impl.cheat.VirtualInventoryCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
            new SimpleCommandPlayer("enderchest", Commands.LEVEL_GAMEMASTERS, FTBEConfig.ENDER_CHEST, (ctx, player) -> enderChest(player)),

            // TODO: Is this really a cheat or is more a utility command?
            // Trash command
            new SimpleConfigurableCommand(FTBEConfig.TRASHCAN, Commands.literal("trash")
                    .executes(context -> trashcan(context.getSource().getPlayerOrException())))
    );

    private static void enderChest(ServerPlayer player) {
        MutableComponent title = Component.translatable("container.enderchest");
        if (player != null) {
            title.append(" × ").append(player.getDisplayName());
        }

        player.openMenu(new SimpleMenuProvider((i, inv, p) -> ChestMenu.threeRows(i, inv, player.getEnderChestInventory()), title));
    }

    public static int trashcan(ServerPlayer player) {
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("sidebar_button.ftbessentials.trash_can");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
                return ChestMenu.fourRows(id, playerInventory);
            }
        });

        return 1;
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
                abilities.mayfly = false;
                abilities.flying = false;
                player.displayClientMessage(Component.literal("Flight disabled"), true);
            } else {
                data.setCanFly(true);
                abilities.mayfly = true;
                player.displayClientMessage(Component.literal("Flight enabled"), true);
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
                player.displayClientMessage(Component.literal("God mode disabled"), true);
            } else {
                data.setGod(true);
                abilities.invulnerable = true;
                player.displayClientMessage(Component.literal("God mode enabled"), true);
            }

            player.onUpdateAbilities();
        });
    }
}
