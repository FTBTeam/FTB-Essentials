package dev.ftb.mods.ftbessentials.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData.RecordingStatus;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class UpdateTabNameMessage extends BaseS2CMessage {
	public final UUID uuid;
	public final String name;
	public final String nickname;
	public final RecordingStatus recording;
	public final boolean afk;

	public UpdateTabNameMessage(UUID id, String n, String nn, RecordingStatus r, boolean a) {
		uuid = id;
		name = n;
		nickname = nn;
		recording = r;
		afk = a;
	}

	public UpdateTabNameMessage(FriendlyByteBuf buf) {
		uuid = new UUID(buf.readLong(), buf.readLong());
		name = buf.readUtf(Short.MAX_VALUE);
		nickname = buf.readUtf(Short.MAX_VALUE);
		recording = buf.readEnum(RecordingStatus.class);
		afk = buf.readBoolean();
	}

	@Override
	public MessageType getType() {
		return FTBEssentialsNet.UPDATE_TAB_NAME;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeLong(uuid.getMostSignificantBits());
		buf.writeLong(uuid.getLeastSignificantBits());
		buf.writeUtf(name, Short.MAX_VALUE);
		buf.writeUtf(nickname, Short.MAX_VALUE);
		buf.writeEnum(recording);
		buf.writeBoolean(afk);
	}

	@Override
	public void handle(NetworkManager.PacketContext packetContext) {
		FTBEssentials.PROXY.updateTabName(this);
	}
}
