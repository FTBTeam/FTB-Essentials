package dev.ftb.mods.ftbessentials.neoforge;

import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.neoforge.WarmupCooldownTeleporterImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod(FTBEssentials.MOD_ID)
public class FTBEssentialsNeoForge {
	public FTBEssentialsNeoForge() {
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::playerName);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::playerNameLow);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::vanillaTeleportCommand);

		FTBEssentials.init();
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
		if (event.getEntity() instanceof ServerPlayer sp && !FTBEConfig.BACK_ON_DEATH_ONLY.get() && !(event instanceof WarmupCooldownTeleporterImpl.EssentialsTeleport)) {
			FTBEPlayerData.addTeleportHistory(sp);
		}
	}
}
