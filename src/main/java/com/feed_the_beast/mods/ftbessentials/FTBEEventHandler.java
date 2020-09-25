package com.feed_the_beast.mods.ftbessentials;

import com.feed_the_beast.mods.ftbessentials.util.FTBEPlayerData;
import com.feed_the_beast.mods.ftbessentials.util.FTBEWorldData;
import com.feed_the_beast.mods.ftbessentials.util.TeleportPos;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;

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
	@SubscribeEvent
	public static void serverAboutToStart(FMLServerAboutToStartEvent event)
	{
		FTBEPlayerData.MAP.clear();
		FTBEWorldData.instance = new FTBEWorldData(event.getServer());

		try
		{
			Path dir = FTBEWorldData.instance.mkdirs("");
			Path file = dir.resolve("world.json");

			if (Files.exists(file))
			{
				try (BufferedReader reader = Files.newBufferedReader(file))
				{
					FTBEWorldData.instance.fromJson(FTBEssentials.GSON.fromJson(reader, JsonObject.class));
				}
			}
		}
		catch (Exception ex)
		{
			FTBEssentials.LOGGER.error("Failed to load world data: " + ex);
			ex.printStackTrace();
		}
	}

	@SubscribeEvent
	public static void serverStopped(FMLServerStoppedEvent event)
	{
		FTBEWorldData.instance = null;
	}

	@SubscribeEvent
	public static void worldSaved(WorldEvent.Save event)
	{
		if (FTBEWorldData.instance != null && FTBEWorldData.instance.save)
		{
			try
			{
				JsonObject json = FTBEWorldData.instance.toJson();
				Path dir = FTBEWorldData.instance.mkdirs("");
				Path file = dir.resolve("world.json");

				try (BufferedWriter writer = Files.newBufferedWriter(file))
				{
					FTBEssentials.GSON.toJson(json, writer);
				}

				FTBEWorldData.instance.save = false;
			}
			catch (Exception ex)
			{
				FTBEssentials.LOGGER.error("Failed to save world data: " + ex);
				ex.printStackTrace();
			}
		}
	}

	@SubscribeEvent
	public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());
		data.lastSeen = new TeleportPos(event.getPlayer());
		data.save();
	}

	@SubscribeEvent
	public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
	{
		FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());
		data.lastSeen = new TeleportPos(event.getPlayer());
		data.save();
	}

	@SubscribeEvent
	public static void playerLoad(PlayerEvent.LoadFromFile event)
	{
		if (FTBEWorldData.instance == null)
		{
			return;
		}

		FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());

		try
		{
			Path dir = FTBEWorldData.instance.mkdirs("playerdata");
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
			FTBEssentials.LOGGER.error("Failed to load player data for " + data.uuid + ":" + data.name + ": " + ex);
			ex.printStackTrace();
		}
	}

	@SubscribeEvent
	public static void playerSaved(PlayerEvent.SaveToFile event)
	{
		FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());

		if (data.save && FTBEWorldData.instance != null)
		{
			try
			{
				JsonObject json = data.toJson();
				Path dir = FTBEWorldData.instance.mkdirs("playerdata");
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
				ex.printStackTrace();
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

	@SubscribeEvent
	public static void playerDeath(LivingDeathEvent event)
	{
		if (event.getEntity() instanceof ServerPlayerEntity)
		{
			FTBEPlayerData.addTeleportHistory((ServerPlayerEntity) event.getEntity());
		}
	}
}