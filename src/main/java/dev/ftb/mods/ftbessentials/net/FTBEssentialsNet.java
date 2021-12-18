package dev.ftb.mods.ftbessentials.net;

import dev.architectury.networking.simple.MessageType;
import dev.architectury.networking.simple.SimpleNetworkManager;
import dev.ftb.mods.ftbessentials.FTBEssentials;

/**
 * @author LatvianModder
 */
public interface FTBEssentialsNet {
	SimpleNetworkManager NET = SimpleNetworkManager.create(FTBEssentials.MOD_ID);

	MessageType UPDATE_TAB_NAME = NET.registerS2C("update_tab_name", UpdateTabNameMessage::new);

	static void init() {
	}
}
