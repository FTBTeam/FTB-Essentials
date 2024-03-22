package dev.ftb.mods.ftbessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbessentials.commands.groups.*;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FTBCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(FTBCommands.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LOGGER.debug("Registering FTB Essentials commands");

        List<FTBCommand> commandGroups = Stream.of(
                FeatureCommands.COMMANDS,
                AdminCommands.COMMANDS,
                ChatCommands.COMMANDS,
                CheatCommands.COMMANDS,
                MiscCommands.COMMANDS,
                TeleportingCommands.COMMANDS
        ).flatMap(List::stream).toList();

        registerCommandsToDispatcher(dispatcher, commandGroups);
    }

    /**
     * Register a list of commands to a dispatcher with support for registering to a namespace instead
     * of the global commands namespace
     *
     * @param dispatcher The dispatcher to register the commands to
     * @param commands  The list of commands to register
     */
    private static void registerCommandsToDispatcher(CommandDispatcher<CommandSourceStack> dispatcher, List<FTBCommand> commands) {
        var namespace = Commands.literal("ftbessentials");
        var commandStack = new ArrayList<LiteralArgumentBuilder<CommandSourceStack>>();

        for (FTBCommand command : commands) {
            if (command.enabled()) {
                for (var builder : command.register()) {
                    if (FTBEConfig.REGISTER_TO_NAMESPACE.get()) {
                        namespace.then(builder);
                        commandStack.add(builder);
                    } else {
                        dispatcher.register(builder);
                    }
                }
            }
        }

        if (FTBEConfig.REGISTER_TO_NAMESPACE.get()) {
            // TODO: This should use alias instead of registering the command again
            //       I think this is possible?
            if (FTBEConfig.REGISTER_ALIAS_AS_WELL_AS_NAMESPACE.get()) {
                for (var builder : commandStack) {
                    dispatcher.register(builder);
                }
            }

            dispatcher.register(namespace);
        }
    }
}
