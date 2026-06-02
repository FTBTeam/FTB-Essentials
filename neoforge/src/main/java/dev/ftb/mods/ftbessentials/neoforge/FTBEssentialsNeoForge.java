package dev.ftb.mods.ftbessentials.neoforge;

import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.api.event.RTPEvent;
import dev.ftb.mods.ftbessentials.api.event.TeleportEvent;
import dev.ftb.mods.ftbessentials.api.neoforge.FTBEssentialsEvent;
import dev.ftb.mods.ftbessentials.config.FTBEStartupConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftblibrary.platform.event.NativeEventPosting;
import dev.ftb.mods.ftblibrary.util.result.DataOutcome;
import dev.ftb.mods.ftblibrary.util.result.Outcome;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(FTBEssentials.MOD_ID)
public class FTBEssentialsNeoForge {
	public FTBEssentialsNeoForge() {
		var essentials = new FTBEssentials();

		IEventBus bus = NeoForge.EVENT_BUS;

		bus.addListener(EventPriority.HIGHEST, this::playerName);
		bus.addListener(EventPriority.LOWEST, this::playerNameLow);
		bus.addListener(EventPriority.LOWEST, this::vanillaTeleportCommand);

		bus.addListener(ServerAboutToStartEvent.class, event -> essentials.eventHandler.serverAboutToStart(event.getServer()));
		bus.addListener(ServerStoppedEvent.class, event -> essentials.eventHandler.serverStopped(event.getServer()));
		bus.addListener(LevelEvent.Save.class, event -> essentials.eventHandler.serverSave(event.getLevel().getServer()));
		bus.addListener(ServerTickEvent.Post.class, event -> essentials.eventHandler.serverTickPost(event.getServer()));
		bus.addListener(RegisterCommandsEvent.class, event -> essentials.eventHandler.registerCommands(
				event.getDispatcher(), event.getBuildContext(), event.getCommandSelection()
		));
		bus.addListener(PlayerEvent.PlayerLoggedInEvent.class, event ->
				essentials.eventHandler.playerLoggedIn((ServerPlayer) event.getEntity())
		);
		bus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event ->
				essentials.eventHandler.playerLoggedOut((ServerPlayer) event.getEntity())
		);
		bus.addListener(PlayerEvent.Clone.class, event ->
				essentials.eventHandler.onPlayerDeath((ServerPlayer) event.getOriginal(), (ServerPlayer) event.getEntity(), !event.isWasDeath())
		);
		bus.addListener(LivingDamageEvent.Post.class, event ->
				essentials.eventHandler.onPlayerHurt(event.getEntity(), event.getInflictedDamage(), event.getBlockedDamage() > 0f)
		);
		bus.addListener(EventPriority.HIGHEST, ServerChatEvent.class, event -> {
			if (essentials.eventHandler.allowChat(event.getPlayer()).isFail()) {
				event.setCanceled(true);
			}
		});

		registerNativeEventPosting(bus);
	}

	private static void registerNativeEventPosting(IEventBus bus) {
		NativeEventPosting.get().registerEventWithResult(TeleportEvent.TYPE, data -> {
			FTBEssentialsEvent.Teleport event = new FTBEssentialsEvent.Teleport(data);
			bus.post(event);
			return event.isCanceled() ? DataOutcome.fail(Component.translatable("ftbessentials.teleport_prevented")) : DataOutcome.pass();
		});
		NativeEventPosting.get().registerEventWithResult(RTPEvent.TYPE, data -> {
			FTBEssentialsEvent.RTP event = new FTBEssentialsEvent.RTP(data);
			bus.post(event);
			return event.isCanceled() ? Outcome.FAIL : Outcome.PASS;
		});
	}

	public void playerName(PlayerEvent.NameFormat event) {
		if (event.getEntity() instanceof ServerPlayer sp) {
			FTBEPlayerData.getOrCreate(sp).ifPresent(data -> {
				if (!data.getNick().isEmpty()) event.setDisplayname(Component.literal(data.getNick()));
			});
		}
	}

	public void playerNameLow(PlayerEvent.NameFormat event) {
		if (event.getEntity() instanceof ServerPlayer sp) {
			FTBEPlayerData.getOrCreate(sp).ifPresent(data -> {
				if (data.getRecording() != FTBEPlayerData.RecordingStatus.NONE) {
					event.setDisplayname(Component.literal("⏺ ").withStyle(data.getRecording().getStyle())
							.append(event.getDisplayname()));
				}
			});
		}
	}

	public void vanillaTeleportCommand(EntityTeleportEvent.TeleportCommand event) {
		// ignore teleport events that we ourselves fired
		if (event.getEntity() instanceof ServerPlayer sp && !FTBEStartupConfig.BACK_ON_DEATH_ONLY.get() && !(event instanceof FTBEssentialsEvent.Teleport)) {
			FTBEPlayerData.addTeleportHistory(sp);
		}
	}
}
