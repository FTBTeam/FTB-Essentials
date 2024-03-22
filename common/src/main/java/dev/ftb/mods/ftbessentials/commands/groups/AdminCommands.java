package dev.ftb.mods.ftbessentials.commands.groups;

import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.commands.SimpleConfigurableCommand;
import dev.ftb.mods.ftbessentials.commands.impl.admin.NicknameForCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.OtherPlayerInventory;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class AdminCommands {
    public static final List<FTBCommand> COMMANDS = List.of(
        // Manage nicknames
        new NicknameForCommand(),

        // Invsee command
        new SimpleConfigurableCommand(
                FTBEConfig.INVSEE,
                literal("invsee")
                        .requires(FTBEConfig.INVSEE.enabledAndOp())
                        .then(argument("player", EntityArgument.player())
                                .executes(context -> viewInventory(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))
                        )
        )
    );

    public static int viewInventory(ServerPlayer source, ServerPlayer player) {
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

        return 1;
    }
}
