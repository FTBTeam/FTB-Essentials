package dev.ftb.mods.ftbessentials.commands.impl.cheat;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.List;


public class VirtualInventoryCommand implements FTBCommand {
    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> register() {
        var openCommand = Commands.literal("open");

        if (FTBEConfig.ANVIL.isEnabled()) {
            openCommand.then(createMenu("anvil", "block.minecraft.anvil", VirtualAnvilMenu::new));
        }

        if (FTBEConfig.CRAFTING_TABLE.isEnabled()) {
            openCommand.then(createMenu("crafting", "block.minecraft.crafting_table", VirtualCraftingMenu::new));
        }

        if (FTBEConfig.SMITHING_TABLE.isEnabled()) {
            openCommand.then(createMenu("smithing", "block.minecraft.smithing_table", VirtualSmithingMenu::new));
        }

        if (FTBEConfig.STONECUTTER.isEnabled()) {
            openCommand.then(createMenu("stonecutter", "block.minecraft.stonecutter", VirtualStoneCutterMenu::new));
        }

        return Collections.singletonList(openCommand);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createMenu(String name, String translate, VirtualMenuFactory factory) {
        return Commands.literal(name)
                .requires(ctx -> ctx.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    player.openMenu(new SimpleMenuProvider((id, inv, p) -> factory.create(id, inv, (ServerPlayer) p), Component.translatable(translate)));
                    return 1;
                });
    }

    //#region Virtual Inventory
    @FunctionalInterface
    public interface VirtualMenuFactory {
        AbstractContainerMenu create(int id, Inventory inv, ServerPlayer player);
    }

    private static class VirtualAnvilMenu extends AnvilMenu {
        public VirtualAnvilMenu(int id, Inventory inv, ServerPlayer player) {
            super(id, inv, ContainerLevelAccess.create(player.level(), player.blockPosition()));
        }

        @Override
        protected boolean isValidBlock(BlockState blockState) {
            return true;
        }
    }

    private static class VirtualSmithingMenu extends SmithingMenu {
        public VirtualSmithingMenu(int id, Inventory inv, ServerPlayer player) {
            super(id, inv, ContainerLevelAccess.create(player.level(), player.blockPosition()));
        }

        @Override
        protected boolean isValidBlock(BlockState blockState) {
            return true;
        }
    }

    private static class VirtualStoneCutterMenu extends StonecutterMenu {
        public VirtualStoneCutterMenu(int id, Inventory inv, ServerPlayer player) {
            super(id, inv, ContainerLevelAccess.create(player.level(), player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static class VirtualCraftingMenu extends CraftingMenu {
        public VirtualCraftingMenu(int id, Inventory inv, ServerPlayer player) {
            super(id, inv, ContainerLevelAccess.create(player.level(), player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }
    //#endregion
}
