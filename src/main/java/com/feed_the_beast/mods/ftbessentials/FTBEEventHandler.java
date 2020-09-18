package com.feed_the_beast.mods.ftbessentials;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author LatvianModder
 */
@Mod.EventBusSubscriber(modid = FTBEssentials.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FTBEEventHandler
{
	public static final FolderName FTBESSENTIALS_DIRECTORY = new FolderName("ftbessentials");

	private static Path mkdirs(@Nullable MinecraftServer server, String path)
	{
		if (server == null)
		{
			throw new NullPointerException("Could not create FTB Essentials data directory: Server is null");
		}

		Path dir = server.func_240776_a_(FTBESSENTIALS_DIRECTORY);

		if (!path.isEmpty())
		{
			dir = dir.resolve(path);
		}

		if (Files.notExists(dir))
		{
			try
			{
				Files.createDirectories(dir);
			}
			catch (Exception ex)
			{
				throw new RuntimeException("Could not create FTB Essentials data directory: " + ex);
			}
		}

		return dir;
	}

	@SubscribeEvent
	public static void serverAboutToStart(FMLServerAboutToStartEvent event)
	{
		FTBEPlayerData.MAP.clear();

		Path dir = mkdirs(event.getServer(), "");

		// TODO: Load warps
	}

	@SubscribeEvent
	public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		// FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());

		// TODO: Remove this?
	}

	@SubscribeEvent
	public static void playerLoad(PlayerEvent.LoadFromFile event)
	{
		FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());

		Path dir = mkdirs(event.getPlayer().getServer(), "playerdata");
	}

	@SubscribeEvent
	public static void playerSaved(PlayerEvent.SaveToFile event)
	{
		FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());

		if (data.save)
		{
			try
			{
				JsonObject json = data.toJson();
				Path dir = mkdirs(event.getPlayer().getServer(), "playerdata");

				try (BufferedWriter writer = Files.newBufferedWriter(event.getPlayerFile("ftbessentials.json").toPath()))
				{
					FTBEssentials.GSON.toJson(json, writer);
				}

				data.save = false;
			}
			catch (Exception ex)
			{
				System.err.println("Failed to save player data for " + data.uuid + ":" + data.name + ": " + ex);
			}
		}
	}
}