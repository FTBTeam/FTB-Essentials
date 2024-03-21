package dev.ftb.mods.ftbessentials.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.ftb.mods.ftbessentials.config.ToggleableConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Abstraction around a command that takes a single entity selector argument and performs an action on it.
 */
public record SimpleCommandPlayer(
        String name,
        int permissionLevel,
        ToggleableConfig config,
        EntitySelectorAction action
) implements FTBCommand {
    public static SimpleCommandPlayer create(String name, int permissionLevel, ToggleableConfig config, EntitySelectorAction action) {
        return new SimpleCommandPlayer(name, permissionLevel, config, action);
    }

    public static SimpleCommandPlayer create(String name, ToggleableConfig config, EntitySelectorAction action) {
        return new SimpleCommandPlayer(name,0, config, action);
    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> register() {
        var command = Commands.literal(name);

        if (this.permissionLevel > 0) {
            command.requires(cs -> cs.hasPermission(this.permissionLevel));
        }

        command.executes(context -> {
                var player = context.getSource().getPlayerOrException();
                action.accept(context, player);
                return 1;
            }).then(Commands.argument("target", EntityArgument.player()).executes(context -> {
                var entities = EntityArgument.getPlayer(context, "target");
                action.accept(context, entities);
                return 1;
            }));

        return List.of(command);
    }

    @Override
    public boolean enabled() {
        return config.isEnabled();
    }

    @FunctionalInterface
    public interface EntitySelectorAction {
        void accept(CommandContext<CommandSourceStack> context, ServerPlayer players);
    }
}
