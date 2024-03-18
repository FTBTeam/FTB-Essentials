package dev.ftb.mods.ftbessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public class CommandUtils {
    /**
     * TODO: Alias system
     *
     * @param dispatcher
     * @param commands
     */
    public static void registerCommandsToDispatcher(CommandDispatcher<CommandSourceStack> dispatcher, List<FTBCommand> commands) {
        for (FTBCommand command : commands) {
            if (command.enabled()) {
                for (var builder : command.register()) {
                    dispatcher.register(builder);
                }
            }
        }
    }

}
