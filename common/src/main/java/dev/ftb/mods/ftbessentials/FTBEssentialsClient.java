package dev.ftb.mods.ftbessentials;

import dev.ftb.mods.ftbessentials.net.UpdateTabNameMessage;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData.RecordingStatus;
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
		if (packet.recording != RecordingStatus.NONE) {
			component.append(Component.literal("⏺").withStyle(packet.recording.getStyle()));
		}

		MutableComponent nameComponent = Component.literal(packet.nickname.isEmpty() ? packet.name : packet.nickname);
		if (packet.afk) {
			nameComponent.withStyle(ChatFormatting.GRAY);
		}

		component.append(nameComponent);
		info.setTabListDisplayName(component);
	}
}
