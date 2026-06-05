package dev.ftb.mods.ftbessentials.client;

import dev.ftb.mods.ftbessentials.net.UpdateTabNameMessage;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData.RecordingStatus;
import dev.ftb.mods.ftblibrary.client.util.ClientUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class FTBEssentialsClient  {
	public static void updateTabName(UpdateTabNameMessage packet) {
		PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(packet.uuid());

		if (info == null) {
			return;
		}

		MutableComponent component = Component.literal("");
		if (packet.recording() != RecordingStatus.NONE) {
			component.append(Component.literal("⏺").withStyle(packet.recording().getStyle()));
		}

		MutableComponent nameComponent = Component.literal(packet.nickname().isEmpty() ? packet.name() : packet.nickname());

		var team = ClientUtils.getClientLevel().getScoreboard().getPlayersTeam(packet.name());
		if (team != null) {
			nameComponent.withStyle(team.getColor());
		}

		if (packet.afk()) {
			nameComponent.append(Component.literal(" [afk]").withStyle(ChatFormatting.GRAY));
		}

		component.append(nameComponent);
		info.setTabListDisplayName(component);
	}
}
