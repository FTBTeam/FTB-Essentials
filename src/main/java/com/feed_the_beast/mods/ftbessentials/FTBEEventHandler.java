package com.feed_the_beast.mods.ftbessentials;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;

import javax.annotation.Nullable;
import java.io.BufferedReader;
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

		try
		{
			Path dir = mkdirs(event.getPlayer().getServer(), "playerdata");
			Path file = dir.resolve(event.getPlayerUUID() + ".json");

			if (Files.exists(file))
			{
				try (BufferedReader reader = Files.newBufferedReader(file))
				{
					data.fromJson(FTBEssentials.GSON.fromJson(reader, JsonObject.class));
				}
			}
		}
		catch (Exception ex)
		{
			FTBEssentials.LOGGER.error("Failed to save player data for " + data.uuid + ":" + data.name + ": " + ex);
		}
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
				Path file = dir.resolve(event.getPlayerUUID() + ".json");

				try (BufferedWriter writer = Files.newBufferedWriter(file))
				{
					FTBEssentials.GSON.toJson(json, writer);
				}

				data.save = false;
			}
			catch (Exception ex)
			{
				FTBEssentials.LOGGER.error("Failed to save player data for " + data.uuid + ":" + data.name + ": " + ex);
			}
		}
	}

	@SubscribeEvent
	public static void playerTick(TickEvent.PlayerTickEvent event)
	{
		if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayerEntity)
		{
			FTBEPlayerData data = FTBEPlayerData.get(event.player);

			if ((data.fly || data.god) && !event.player.abilities.allowFlying)
			{
				event.player.abilities.allowFlying = true;
				event.player.sendPlayerAbilities();
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void playerServerChatHighest(ServerChatEvent event)
	{
		FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());

		if (data.muted)
		{
			event.setCanceled(true);
			event.getPlayer().sendStatusMessage(new StringTextComponent("You can't use chat, you've been muted by an admin!").mergeStyle(TextFormatting.RED), false);
		}
	}

	@SubscribeEvent
	public static void playerName(PlayerEvent.NameFormat event)
	{
		if (event.getPlayer() instanceof ServerPlayerEntity)
		{
			FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());

			if (!data.nick.isEmpty())
			{
				event.setDisplayname(new StringTextComponent(data.nick));
			}
		}
	}
}