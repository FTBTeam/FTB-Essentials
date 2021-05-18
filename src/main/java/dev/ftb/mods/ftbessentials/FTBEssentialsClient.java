package dev.ftb.mods.ftbessentials;

import dev.ftb.mods.ftbessentials.net.UpdateTabNamePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.TextComponent;

/**
 * @author LatvianModder
 */
public class FTBEssentialsClient extends FTBEssentialsCommon {
	@Override
	public void updateTabName(UpdateTabNamePacket packet) {
		PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(packet.uuid);

		if (info == null) {
			return;
		}

		TextComponent component = new TextComponent("");

		if (packet.recording > 0) {
			TextComponent component1 = new TextComponent("\u23FA");
			component1.withStyle(packet.recording == 1 ? FTBEEventHandler.RECORDING_STYLE : FTBEEventHandler.STREAMING_STYLE);
			component.append(component1);
		}

		TextComponent nameComponent = new TextComponent(packet.nickname.isEmpty() ? packet.name : packet.nickname);

		if (packet.afk) {
			nameComponent.withStyle(ChatFormatting.GRAY);
		}

		component.append(nameComponent);
		info.setTabListDisplayName(component);
	}
}
