package dev.ftb.mods.ftbessentials.net;

import dev.ftb.mods.ftbessentials.FTBEssentials;
import me.shedaniel.architectury.networking.simple.MessageType;
import me.shedaniel.architectury.networking.simple.SimpleNetworkManager;

/**
 * @author LatvianModder
 */
public interface FTBEssentialsNet {
	SimpleNetworkManager NET = SimpleNetworkManager.create(FTBEssentials.MOD_ID);

	MessageType UPDATE_TAB_NAME = NET.registerS2C("update_tab_name", UpdateTabNameMessage::new);

	static void init() {
	}
}
