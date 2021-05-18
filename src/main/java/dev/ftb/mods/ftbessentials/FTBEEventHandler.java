package dev.ftb.mods.ftbessentials;

import com.google.gson.JsonObject;
import dev.ftb.mods.ftbessentials.command.TPACommands;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.FTBEWorldData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
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
public class FTBEEventHandler {
	public static final Style RECORDING_STYLE = Style.EMPTY.applyFormat(ChatFormatting.RED);
	public static final Style STREAMING_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x9146FF));

	@SubscribeEvent
	public static void serverAboutToStart(FMLServerAboutToStartEvent event) {
		FTBEPlayerData.MAP.clear();
		FTBEWorldData.instance = new FTBEWorldData(event.getServer());

		try {
			Path dir = FTBEWorldData.instance.mkdirs("");
			Path file = dir.resolve("world.json");

			if (Files.exists(file)) {
				try (BufferedReader reader = Files.newBufferedReader(file)) {
					FTBEWorldData.instance.fromJson(FTBEssentials.GSON.fromJson(reader, JsonObject.class));
				}
			}
		} catch (Exception ex) {
			FTBEssentials.LOGGER.error("Failed to load world data: " + ex);
			ex.printStackTrace();
		}
	}

	@SubscribeEvent
	public static void serverStopped(FMLServerStoppedEvent event) {
		FTBEWorldData.instance = null;
		TPACommands.REQUESTS.clear();
	}

	@SubscribeEvent
	public static void worldSaved(WorldEvent.Save event) {
		if (FTBEWorldData.instance != null && FTBEWorldData.instance.save) {
			try {
				JsonObject json = FTBEWorldData.instance.toJson();
				Path dir = FTBEWorldData.instance.mkdirs("");
				Path file = dir.resolve("world.json");

				try (BufferedWriter writer = Files.newBufferedWriter(file)) {
					FTBEssentials.GSON.toJson(json, writer);
				}

				FTBEWorldData.instance.save = false;
			} catch (Exception ex) {
				FTBEssentials.LOGGER.error("Failed to save world data: " + ex);
				ex.printStackTrace();
			}
		}
	}

	@SubscribeEvent
	public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());
		data.lastSeen = new TeleportPos(event.getPlayer());
		data.save();

		for (FTBEPlayerData d : FTBEPlayerData.MAP.values()) {
			d.sendTabName((ServerPlayer) event.getPlayer());
		}
	}

	@SubscribeEvent
	public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());
		data.lastSeen = new TeleportPos(event.getPlayer());
		data.save();
	}

	@SubscribeEvent
	public static void playerLoad(PlayerEvent.LoadFromFile event) {
		if (FTBEWorldData.instance != null) {
			FTBEPlayerData.get(event.getPlayer()).load();
		}
	}

	@SubscribeEvent
	public static void playerSaved(PlayerEvent.SaveToFile event) {
		if (FTBEWorldData.instance != null) {
			FTBEPlayerData.get(event.getPlayer()).saveNow();
		}
	}

	@SubscribeEvent
	public static void playerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer) {
			FTBEPlayerData data = FTBEPlayerData.get(event.player);

			if ((data.fly || data.god) && !event.player.abilities.mayfly) {
				event.player.abilities.mayfly = true;
				event.player.onUpdateAbilities();
			}
		}
	}

	@SubscribeEvent
	public static void serverTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			long now = System.currentTimeMillis();

			Iterator<TPACommands.TPARequest> iterator = TPACommands.REQUESTS.values().iterator();

			while (iterator.hasNext()) {
				TPACommands.TPARequest r = iterator.next();

				if (now > r.created + 60000L) {
					ServerPlayer source = r.server.getPlayerList().getPlayer(r.source.uuid);
					ServerPlayer target = r.server.getPlayerList().getPlayer(r.target.uuid);

					if (source != null) {
						source.sendMessage(new TextComponent("TPA request expired!"), Util.NIL_UUID);
					}

					if (target != null) {
						target.sendMessage(new TextComponent("TPA request expired!"), Util.NIL_UUID);
					}

					iterator.remove();
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void playerServerChatHighest(ServerChatEvent event) {
		FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());

		if (data.muted) {
			event.setCanceled(true);
			event.getPlayer().displayClientMessage(new TextComponent("You can't use chat, you've been muted by an admin!").withStyle(ChatFormatting.RED), false);
		}
	}

	@SubscribeEvent
	public static void playerName(PlayerEvent.NameFormat event) {
		if (event.getPlayer() instanceof ServerPlayer) {
			FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());

			if (!data.nick.isEmpty()) {
				event.setDisplayname(new TextComponent(data.nick));
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void playerNameLow(PlayerEvent.NameFormat event) {
		if (event.getPlayer() instanceof ServerPlayer) {
			FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());

			if (data.recording > 0) {
				event.setDisplayname(new TextComponent("").append(new TextComponent("\u23FA").withStyle(data.recording == 1 ? RECORDING_STYLE : STREAMING_STYLE)).append(" ").append(event.getDisplayname()));
			}
		}
	}

	@SubscribeEvent
	public static void playerDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer) {
			FTBEPlayerData.addTeleportHistory((ServerPlayer) event.getEntity());
		}
	}
}