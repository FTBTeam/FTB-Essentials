package com.feed_the_beast.mods.ftbessentials.net;

import com.feed_the_beast.mods.ftbessentials.FTBEssentials;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/**
 * @author LatvianModder
 */
public class FTBEssentialsNet
{
	public static SimpleChannel MAIN;
	private static final String MAIN_VERSION = "1";

	public static void init()
	{
		MAIN = NetworkRegistry.ChannelBuilder
				.named(new ResourceLocation(FTBEssentials.MOD_ID + ":main"))
				.clientAcceptedVersions(MAIN_VERSION::equals)
				.serverAcceptedVersions(MAIN_VERSION::equals)
				.networkProtocolVersion(() -> MAIN_VERSION)
				.simpleChannel();

		//MAIN.registerMessage(1, SelectTemperedJarRecipePacket.class, SelectTemperedJarRecipePacket::write, SelectTemperedJarRecipePacket::new, SelectTemperedJarRecipePacket::handle);
	}
}
