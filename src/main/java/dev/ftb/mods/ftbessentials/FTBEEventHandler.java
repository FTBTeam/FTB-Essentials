package dev.ftb.mods.ftbessentials;

import dev.ftb.mods.ftbessentials.command.FTBEssentialsCommands;
import dev.ftb.mods.ftbessentials.command.TPACommands;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.FTBEWorldData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;
import me.shedaniel.architectury.hooks.LevelResourceHooks;
import me.shedaniel.architectury.platform.Platform;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * @author LatvianModder
 */
@Mod.EventBusSubscriber(modid = FTBEssentials.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FTBEEventHandler {
	public static final LevelResource CONFIG_FILE = LevelResourceHooks.create("serverconfig/ftbessentials.snbt");
	public static final Style RECORDING_STYLE = Style.EMPTY.applyFormat(ChatFormatting.RED);
	public static final Style STREAMING_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x9146FF));

	@SubscribeEvent
	public static void serverAboutToStart(FMLServerAboutToStartEvent event) {
		Path configFilePath = event.getServer().getWorldPath(CONFIG_FILE);
		Path defaultConfigFilePath = Platform.getConfigFolder().resolve("../defaultconfigs/ftbessentials-server.snbt");

		if (Files.exists(defaultConfigFilePath)) {
			if (!Files.exists(configFilePath)) {
				try {
					Files.copy(defaultConfigFilePath, configFilePath);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		} else {
			SNBTConfig defaultConfigFile = SNBTConfig.create(FTBEssentials.MOD_ID);
			defaultConfigFile.comment("Default config file that will be copied to world's serverconfig/ftbessentials.snbt location");
			defaultConfigFile.comment("Copy values you wish to override in here");
			defaultConfigFile.comment("Example:");
			defaultConfigFile.comment("");
			defaultConfigFile.comment("{");
			defaultConfigFile.comment("	misc: {");
			defaultConfigFile.comment("		enderchest: {");
			defaultConfigFile.comment("			enabled: false");
			defaultConfigFile.comment("		}");
			defaultConfigFile.comment("	}");
			defaultConfigFile.comment("}");

			defaultConfigFile.save(defaultConfigFilePath);
		}

		FTBEConfig.CONFIG.load(configFilePath);
		FTBEPlayerData.MAP.clear();
		FTBEWorldData.instance = new FTBEWorldData(event.getServer());

		try {
			SNBTCompoundTag tag = SNBT.read(FTBEWorldData.instance.mkdirs("").resolve("data.snbt"));

			if (tag != null) {
				FTBEWorldData.instance.read(tag);
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
	public static void registerCommands(RegisterCommandsEvent event) {
		FTBEssentialsCommands.registerCommands(event.getDispatcher());
	}

	@SubscribeEvent
	public static void worldSaved(WorldEvent.Save event) {
		if (FTBEWorldData.instance != null && FTBEWorldData.instance.save) {
			if (SNBT.write(FTBEWorldData.instance.mkdirs("").resolve("data.snbt"), FTBEWorldData.instance.write())) {
				FTBEWorldData.instance.save = false;
			}
		}
	}

	@SubscribeEvent
	public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());

		if (data == null) {
			return;
		}

		data.lastSeen = new TeleportPos(event.getPlayer());
		data.save();

		for (FTBEPlayerData d : FTBEPlayerData.MAP.values()) {
			d.sendTabName((ServerPlayer) event.getPlayer());
		}
	}

	@SubscribeEvent
	public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());

		if (data == null) {
			return;
		}

		data.lastSeen = new TeleportPos(event.getPlayer());
		data.save();
	}

	@SubscribeEvent
	public static void playerLoad(PlayerEvent.LoadFromFile event) {
		if (FTBEWorldData.instance != null) {
			FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());

			if (data != null) {
				data.load();
			}
		}
	}

	@SubscribeEvent
	public static void playerSaved(PlayerEvent.SaveToFile event) {
		if (FTBEWorldData.instance != null) {
			FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());

			if (data != null) {
				data.saveNow();
			}
		}
	}

	@SubscribeEvent
	public static void playerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer) {
			FTBEPlayerData data = FTBEPlayerData.get(event.player);

			if (data == null) {
				return;
			}

			if (data.god && !event.player.abilities.invulnerable) {
				event.player.abilities.invulnerable = true;
				event.player.onUpdateAbilities();
			}

			if (data.fly && !event.player.abilities.mayfly) {
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

		if (data != null && data.muted) {
			event.setCanceled(true);
			event.getPlayer().displayClientMessage(new TextComponent("You can't use chat, you've been muted by an admin!").withStyle(ChatFormatting.RED), false);
		}
	}

	@SubscribeEvent
	public static void playerName(PlayerEvent.NameFormat event) {
		if (event.getPlayer() instanceof ServerPlayer) {
			FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());

			if (data != null && !data.nick.isEmpty()) {
				event.setDisplayname(new TextComponent(data.nick));
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void playerNameLow(PlayerEvent.NameFormat event) {
		if (event.getPlayer() instanceof ServerPlayer) {
			FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());

			if (data != null && data.recording > 0) {
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