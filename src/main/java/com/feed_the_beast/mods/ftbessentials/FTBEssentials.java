package com.feed_the_beast.mods.ftbessentials;

import com.feed_the_beast.mods.ftbessentials.net.FTBEssentialsNet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.common.Mod;

/**
 * @author LatvianModder
 */
@Mod(FTBEssentials.MOD_ID)
@Mod.EventBusSubscriber(modid = FTBEssentials.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FTBEssentials
{
	public static final String MOD_ID = "ftbessentials";
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().setLenient().create();

	public static FTBEssentials instance;

	public FTBEssentials()
	{
		instance = this;
		FTBEssentialsNet.init();
	}
}