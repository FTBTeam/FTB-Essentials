package dev.ftb.mods.ftbessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

/**
 * @author LatvianModder
 */
public class FTBEssentialsCommands {
	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		HomeCommands.register(dispatcher);
		WarpCommands.register(dispatcher);
		TeleportCommands.register(dispatcher);
		TPACommands.register(dispatcher);
		CheatCommands.register(dispatcher);
		MiscCommands.register(dispatcher);
	}
}
