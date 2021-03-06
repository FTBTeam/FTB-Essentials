package dev.ftb.mods.ftbessentials.command;

import dev.ftb.mods.ftbessentials.FTBEssentials;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @author LatvianModder
 */
@Mod.EventBusSubscriber(modid = FTBEssentials.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FTBEssentialsCommands {
	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		HomeCommands.register(event.getDispatcher());
		WarpCommands.register(event.getDispatcher());
		TeleportCommands.register(event.getDispatcher());
		TPACommands.register(event.getDispatcher());
		CheatCommands.register(event.getDispatcher());
		MiscCommands.register(event.getDispatcher());
	}
}
