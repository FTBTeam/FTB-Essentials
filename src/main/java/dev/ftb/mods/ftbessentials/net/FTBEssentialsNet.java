package dev.ftb.mods.ftbessentials.net;

import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftblibrary.net.snm.PacketID;
import dev.ftb.mods.ftblibrary.net.snm.SimpleNetworkManager;

/**
 * @author LatvianModder
 */
public interface FTBEssentialsNet {
	SimpleNetworkManager NET = SimpleNetworkManager.create(FTBEssentials.MOD_ID);

	PacketID UPDATE_TAB_NAME = NET.registerS2C("update_tab_name", UpdateTabNamePacket::new);

	static void init() {
	}
}
