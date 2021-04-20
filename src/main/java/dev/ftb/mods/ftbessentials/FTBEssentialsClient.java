package dev.ftb.mods.ftbessentials;

import dev.ftb.mods.ftbessentials.net.UpdateTabNamePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

/**
 * @author LatvianModder
 */
public class FTBEssentialsClient extends FTBEssentialsCommon {
	@Override
	public void updateTabName(UpdateTabNamePacket packet) {
		NetworkPlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(packet.uuid);

		if (info == null) {
			return;
		}

		StringTextComponent component = new StringTextComponent("");

		if (packet.recording > 0) {
			StringTextComponent component1 = new StringTextComponent("\u23FA");
			component1.mergeStyle(packet.recording == 1 ? FTBEEventHandler.RECORDING_STYLE : FTBEEventHandler.STREAMING_STYLE);
			component.append(component1);
		}

		StringTextComponent nameComponent = new StringTextComponent(packet.nickname.isEmpty() ? packet.name : packet.nickname);

		if (packet.afk) {
			nameComponent.mergeStyle(TextFormatting.GRAY);
		}

		component.append(nameComponent);
		info.setDisplayName(component);
	}
}
