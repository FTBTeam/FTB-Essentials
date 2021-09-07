package dev.ftb.mods.ftbessentials.net;

import dev.ftb.mods.ftbessentials.FTBEssentials;
import me.shedaniel.architectury.networking.NetworkManager;
import me.shedaniel.architectury.networking.simple.BaseS2CMessage;
import me.shedaniel.architectury.networking.simple.MessageType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * @author LatvianModder
 */
public class UpdateTabNameMessage extends BaseS2CMessage {
	public final UUID uuid;
	public final String name;
	public final String nickname;
	public final int recording;
	public final boolean afk;

	public UpdateTabNameMessage(UUID id, String n, String nn, int r, boolean a) {
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
		recording = buf.readByte();
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
		buf.writeByte(recording);
		buf.writeBoolean(afk);
	}

	@Override
	public void handle(NetworkManager.PacketContext packetContext) {
		FTBEssentials.PROXY.updateTabName(this);
	}
}