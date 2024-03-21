package dev.ftb.mods.ftbessentials.commands.groups;

import dev.ftb.mods.ftbessentials.commands.CommandUtils;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.commands.SimpleConfigurableCommand;
import dev.ftb.mods.ftbessentials.commands.impl.misc.LeaderboardCommand;
import dev.ftb.mods.ftbessentials.commands.impl.misc.NearCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
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
            new NearCommand()
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
}
