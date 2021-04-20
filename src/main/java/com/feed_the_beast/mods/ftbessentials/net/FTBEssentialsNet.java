package com.feed_the_beast.mods.ftbessentials.net;

import com.feed_the_beast.mods.ftbessentials.FTBEssentials;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.Predicate;

/**
 * @author LatvianModder
 */
public class FTBEssentialsNet {
	public static SimpleChannel MAIN;
	private static final String MAIN_VERSION = "1";

	public static void init() {
		Predicate<String> validator = v -> MAIN_VERSION.equals(v) || NetworkRegistry.ABSENT.equals(v) || NetworkRegistry.ACCEPTVANILLA.equals(v);

		MAIN = NetworkRegistry.ChannelBuilder
				.named(new ResourceLocation(FTBEssentials.MOD_ID + ":main"))
				.clientAcceptedVersions(validator)
				.serverAcceptedVersions(validator)
				.networkProtocolVersion(() -> MAIN_VERSION)
				.simpleChannel();

		MAIN.registerMessage(1, UpdateTabNamePacket.class, UpdateTabNamePacket::write, UpdateTabNamePacket::new, UpdateTabNamePacket::handle);
	}
}
