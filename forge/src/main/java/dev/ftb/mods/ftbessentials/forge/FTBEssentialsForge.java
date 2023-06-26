package dev.ftb.mods.ftbessentials.forge;

import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
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

import static dev.ftb.mods.ftbessentials.FTBEssentials.RECORDING_STYLE;
import static dev.ftb.mods.ftbessentials.FTBEssentials.STREAMING_STYLE;

@Mod(FTBEssentials.MOD_ID)
public class FTBEssentialsForge {
	public FTBEssentialsForge() {
		ModLoadingContext.get().registerExtensionPoint(DisplayTest.class, () -> new DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));

		MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::playerName);
		MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::playerNameLow);
		MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::vanillaTeleportCommand);

		FTBEssentials.init();
	}

	public void playerName(PlayerEvent.NameFormat event) {
		if (event.getEntity() instanceof ServerPlayer sp) {
			FTBEPlayerData data = FTBEPlayerData.get(sp);

			if (data != null && !data.nick.isEmpty()) {
				Component name = Component.literal(data.nick);
				event.setDisplayname(name);
			}
		}
	}

	public void playerNameLow(PlayerEvent.NameFormat event) {
		if (event.getEntity() instanceof ServerPlayer) {
			FTBEPlayerData data = FTBEPlayerData.get(event.getEntity());

			if (data != null && data.recording > 0) {
				event.setDisplayname(Component.literal("\u23FA ").withStyle(data.recording == 1 ? RECORDING_STYLE : STREAMING_STYLE)
						.append(event.getDisplayname()));
			}
		}
	}

	public void vanillaTeleportCommand(EntityTeleportEvent.TeleportCommand event) {
		if (event.getEntity() instanceof ServerPlayer sp && !FTBEConfig.BACK_ON_DEATH_ONLY.get()) {
			FTBEPlayerData.addTeleportHistory(sp);
		}
	}
}