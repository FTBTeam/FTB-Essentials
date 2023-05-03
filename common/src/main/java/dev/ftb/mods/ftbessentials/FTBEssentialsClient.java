package dev.ftb.mods.ftbessentials;

import dev.ftb.mods.ftbessentials.net.UpdateTabNameMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * @author LatvianModder
 */
public class FTBEssentialsClient extends FTBEssentialsCommon {
	@Override
	public void updateTabName(UpdateTabNameMessage packet) {
		PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(packet.uuid);

		if (info == null) {
			return;
		}

		MutableComponent component = Component.literal("");

		if (packet.recording > 0) {
			MutableComponent component1 = Component.literal("\u23FA");
			component1.withStyle(packet.recording == 1 ? FTBEssentials.RECORDING_STYLE : FTBEssentials.STREAMING_STYLE);
			component.append(component1);
		}

		MutableComponent nameComponent = Component.literal(packet.nickname.isEmpty() ? packet.name : packet.nickname);

		if (packet.afk) {
			nameComponent.withStyle(ChatFormatting.GRAY);
		}

		component.append(nameComponent);
		info.setTabListDisplayName(component);
	}
}
