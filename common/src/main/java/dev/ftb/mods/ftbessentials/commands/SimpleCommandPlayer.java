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
        int targetedPermissionLevel,  // when using the command with a "target" player
        ToggleableConfig config,
        EntitySelectorAction action
) implements FTBCommand {
    public static SimpleCommandPlayer create(String name, int permissionLevel, ToggleableConfig config, EntitySelectorAction action) {
        return new SimpleCommandPlayer(name, permissionLevel, permissionLevel, config, action);
    }

    public static SimpleCommandPlayer create(String name, ToggleableConfig config, EntitySelectorAction action) {
        return create(name, Commands.LEVEL_ALL, config, action);
    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> register() {
        return List.of(
                Commands.literal(name).requires(cs -> cs.hasPermission(this.permissionLevel))
                        .executes(context -> action.accept(context, context.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .requires(cs -> cs.hasPermission(targetedPermissionLevel))
                                .executes(context -> action.accept(context, EntityArgument.getPlayer(context, "target")))
                        )
        );
    }

    @Override
    public boolean enabled() {
        return config.isEnabled();
    }

    @FunctionalInterface
    public interface EntitySelectorAction {
        int accept(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer);
    }
}
