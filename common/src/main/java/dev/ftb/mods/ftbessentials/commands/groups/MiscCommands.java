package dev.ftb.mods.ftbessentials.commands.groups;

import dev.ftb.mods.ftbessentials.commands.CommandUtils;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.commands.SimpleConfigurableCommand;
import dev.ftb.mods.ftbessentials.commands.impl.misc.LeaderboardCommand;
import dev.ftb.mods.ftbessentials.commands.impl.misc.NearCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static net.minecraft.commands.Commands.literal;

public class MiscCommands {
    public static final List<FTBCommand> COMMANDS = List.of(
            new SimpleConfigurableCommand(FTBEConfig.KICKME, literal("kickme")
                    .executes(context -> kickme(context.getSource().getPlayerOrException()))),
            new SimpleConfigurableCommand(FTBEConfig.HAT, literal("hat")
                    .requires(CommandUtils.isGamemaster())
                    .executes(context -> hat(context.getSource().getPlayerOrException()))),

            new LeaderboardCommand(),
            new NearCommand(),

            // Trash command
            new SimpleConfigurableCommand(FTBEConfig.TRASHCAN, Commands.literal("trashcan")
                    .executes(context -> trashcan(context.getSource().getPlayerOrException())))
    );

    public static int kickme(ServerPlayer player) {
        player.connection.disconnect(Component.translatable("ftbessentials.messages.kick_self"));
        return 1;
    }

    /**
     * Allows any item in the mainhand to be used as a head item
     *
     * @param player The player to swap the items for
     */
    public static int hat(ServerPlayer player) {
        ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack targetStack = player.getItemBySlot(EquipmentSlot.MAINHAND);
        player.setItemSlot(EquipmentSlot.HEAD, targetStack);
        player.setItemSlot(EquipmentSlot.MAINHAND, headStack);
        player.inventoryMenu.broadcastChanges();
        return 1;
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
}
