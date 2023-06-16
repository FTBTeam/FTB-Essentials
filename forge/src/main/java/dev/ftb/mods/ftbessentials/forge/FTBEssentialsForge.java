package dev.ftb.mods.ftbessentials.forge;

import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.FTBEWorldData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.IExtensionPoint.DisplayTest;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;

@Mod(FTBEssentials.MOD_ID)
public class FTBEssentialsForge {
	public FTBEssentialsForge() {
		ModLoadingContext.get().registerExtensionPoint(DisplayTest.class, () -> new DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));

		MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::playerName);
		MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::playerNameLow);
		MinecraftForge.EVENT_BUS.addListener(this::playerLoad);
		MinecraftForge.EVENT_BUS.addListener(this::playerSaved);
		MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::vanillaTeleportCommand);

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

	public void playerLoad(PlayerEvent.LoadFromFile event) {
		if (FTBEWorldData.instance != null) {
			FTBEPlayerData.getOrCreate(event.getEntity()).ifPresent(FTBEPlayerData::load);
		}
	}

	public void playerSaved(PlayerEvent.SaveToFile event) {
		if (FTBEWorldData.instance != null) {
			FTBEPlayerData.getOrCreate(event.getEntity()).ifPresent(FTBEPlayerData::saveNow);
		}
	}

	public void vanillaTeleportCommand(EntityTeleportEvent.TeleportCommand event) {
		if (event.getEntity() instanceof ServerPlayer sp && !FTBEConfig.BACK_ON_DEATH_ONLY.get()) {
			FTBEPlayerData.addTeleportHistory(sp);
		}
	}
}