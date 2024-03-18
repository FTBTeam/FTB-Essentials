package dev.ftb.mods.ftbessentials.commands.groups;

import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.util.OtherPlayerInventory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.List;

public class AdminCommands {
    public static final List<FTBCommand> COMMANDS = List.of(

    );

    public static void viewInventory(ServerPlayer source, ServerPlayer player) {
        source.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return player.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player p) {
                return new ChestMenu(MenuType.GENERIC_9x5, id, playerInventory, new OtherPlayerInventory(player), 5);
            }
        });
    }
}
