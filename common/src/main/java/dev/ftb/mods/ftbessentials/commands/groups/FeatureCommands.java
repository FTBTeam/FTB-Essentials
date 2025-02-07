package dev.ftb.mods.ftbessentials.commands.groups;

import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.commands.impl.kit.GiveMeKitCommand;
import dev.ftb.mods.ftbessentials.commands.impl.kit.KitCommand;

import java.util.List;

/**
 * This class is for registering general feature commands where the commands are exclusively
 * grouped with-in their own namespace. Kit commands are an example of this.
 */
public class FeatureCommands {
    public static final List<FTBCommand> COMMANDS = List.of(
            new KitCommand(),
            new GiveMeKitCommand()
    );
}
