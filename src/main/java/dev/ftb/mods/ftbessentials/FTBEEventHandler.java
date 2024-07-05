package dev.ftb.mods.ftbessentials;

import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbessentials.command.FTBEssentialsCommands;
import dev.ftb.mods.ftbessentials.command.TPACommands;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.FTBEWorldData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import dev.ftb.mods.ftbessentials.util.WarmupCooldownTeleporter;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.nio.file.Path;
import java.util.Iterator;

/**
 * @author LatvianModder
 */
@Mod.EventBusSubscriber(modid = FTBEssentials.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FTBEEventHandler {
	public static final LevelResource CONFIG_FILE = new LevelResource("serverconfig/ftbessentials.snbt");
	public static final Style RECORDING_STYLE = Style.EMPTY.applyFormat(ChatFormatting.RED);
	public static final Style STREAMING_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x9146FF));

	@SubscribeEvent
	public static void serverAboutToStart(ServerAboutToStartEvent event) {
		Path configFilePath = event.getServer().getWorldPath(CONFIG_FILE);
		Path defaultConfigFilePath = Platform.getConfigFolder().resolve("../defaultconfigs/ftbessentials-server.snbt");

		FTBEConfig.CONFIG.load(configFilePath, defaultConfigFilePath, () -> new String[]{
				"Default config file that will be copied to world's serverconfig/ftbessentials.snbt location",
				"Copy values you wish to override in here",
				"Example:",
				"",
				"{",
				"	misc: {",
				"		enderchest: {",
				"			enabled: false",
				"		}",
				"	}",
				"}",
		});

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
	public static void serverStopped(ServerStoppedEvent event) {
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
			var data = FTBEPlayerData.get(event.player);
			var abilities = event.player.getAbilities();

			if (data == null) {
				return;
			}

			if (data.god && !abilities.invulnerable) {
				abilities.invulnerable = true;
				event.player.onUpdateAbilities();
			}

			if (data.fly && !abilities.mayfly) {
				abilities.mayfly = true;
				event.player.onUpdateAbilities();
			}
		}
	}

	@SubscribeEvent
	public static void serverTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			long now = System.currentTimeMillis();

			Iterator<TPACommands.TPARequest> iterator = TPACommands.REQUESTS.values().iterator();

			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			while (iterator.hasNext()) {
				TPACommands.TPARequest r = iterator.next();

				if (now > r.created() + 60000L) {
					ServerPlayer source = server.getPlayerList().getPlayer(r.source().uuid);
					ServerPlayer target = server.getPlayerList().getPlayer(r.target().uuid);

					if (source != null) {
						source.sendMessage(new TranslatableComponent("tip.ftbessentials.tpa_expired"), Util.NIL_UUID);
					}

					if (target != null) {
						target.sendMessage(new TranslatableComponent("tip.ftbessentials.tpa_expired"), Util.NIL_UUID);
					}

					iterator.remove();
				}
			}

			if (server.getTickCount() % 20 == 0) {
				WarmupCooldownTeleporter.tickWarmups(server);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void playerServerChatHighest(ServerChatEvent event) {
		FTBEPlayerData data = FTBEPlayerData.get(event.getPlayer());

		if (data != null && data.muted) {
			event.setCanceled(true);
			event.getPlayer().displayClientMessage(new TranslatableComponent("tip.ftbessentials.muted").withStyle(ChatFormatting.RED), false);
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

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void playerDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer sp) {
			FTBEPlayerData.addTeleportHistory(sp);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void vanillaTeleportCommand(EntityTeleportEvent.TeleportCommand event) {
		if (event.getEntity() instanceof ServerPlayer sp) {
			FTBEPlayerData.addTeleportHistory(sp);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onPlayerHurt(LivingHurtEvent event) {
		if (event.getEntity() instanceof ServerPlayer sp && event.getAmount() > 0f) {
			WarmupCooldownTeleporter.cancelWarmup(sp);
		}
	}

	@SubscribeEvent
	public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer sp) {
			WarmupCooldownTeleporter.cancelWarmup(sp);
		}
	}
}
