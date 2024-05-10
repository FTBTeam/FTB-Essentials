package dev.ftb.mods.ftbessentials.net;

import dev.ftb.mods.ftblibrary.util.NetworkHelper;

public class FTBEssentialsNet {
	public static void init() {
		NetworkHelper.registerS2C(UpdateTabNameMessage.TYPE, UpdateTabNameMessage.STREAM_CODEC, UpdateTabNameMessage::handle);
	}
}
