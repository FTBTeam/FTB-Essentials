package com.feed_the_beast.mods.ftbessentials;

import com.feed_the_beast.mods.ftbessentials.command.TPACommands;
import com.feed_the_beast.mods.ftbessentials.util.FTBEPlayerData;
import com.feed_the_beast.mods.ftbessentials.util.FTBEWorldData;
import com.feed_the_beast.mods.ftbessentials.util.TeleportPos;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
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
import java.util.Iterator;

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
		TPACommands.REQUESTS.clear();
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
		if (FTBEWorldData.instance != null)
		{
			FTBEPlayerData.get(event.getPlayer()).load();
		}
	}

	@SubscribeEvent
	public static void playerSaved(PlayerEvent.SaveToFile event)
	{
		if (FTBEWorldData.instance != null)
		{
			FTBEPlayerData.get(event.getPlayer()).saveNow();
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

	@SubscribeEvent
	public static void serverTick(TickEvent.ServerTickEvent event)
	{
		if (event.phase == TickEvent.Phase.END)
		{
			long now = System.currentTimeMillis();

			Iterator<TPACommands.TPARequest> iterator = TPACommands.REQUESTS.values().iterator();

			while (iterator.hasNext())
			{
				TPACommands.TPARequest r = iterator.next();

				if (now > r.created + 60000L)
				{
					ServerPlayerEntity source = r.server.getPlayerList().getPlayerByUUID(r.source.uuid);
					ServerPlayerEntity target = r.server.getPlayerList().getPlayerByUUID(r.target.uuid);

					if (source != null)
					{
						source.sendMessage(new StringTextComponent("TPA request expired!"), Util.DUMMY_UUID);
					}

					if (target != null)
					{
						target.sendMessage(new StringTextComponent("TPA request expired!"), Util.DUMMY_UUID);
					}

					iterator.remove();
				}
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

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void playerNameLow(PlayerEvent.NameFormat event)
	{
		if (event.getPlayer() instanceof ServerPlayerEntity)
		{
			FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());

			if (data.recording == 1)
			{
				event.setDisplayname(new StringTextComponent("").append(new StringTextComponent("\u23FA").mergeStyle(TextFormatting.RED)).appendString(" ").append(event.getDisplayname()));
			}
			else if (data.recording == 2)
			{
				event.setDisplayname(new StringTextComponent("").append(new StringTextComponent("\u23FA").mergeStyle(Style.EMPTY.setColor(Color.fromInt(0x9146FF)))).appendString(" ").append(event.getDisplayname()));
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