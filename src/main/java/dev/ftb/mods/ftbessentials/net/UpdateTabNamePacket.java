package dev.ftb.mods.ftbessentials.net;

import dev.ftb.mods.ftbessentials.FTBEssentials;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class UpdateTabNamePacket {
	public final UUID uuid;
	public final String name;
	public final String nickname;
	public final int recording;
	public final boolean afk;

	public UpdateTabNamePacket(UUID id, String n, String nn, int r, boolean a) {
		uuid = id;
		name = n;
		nickname = nn;
		recording = r;
		afk = a;
	}

	public UpdateTabNamePacket(PacketBuffer buf) {
		uuid = new UUID(buf.readLong(), buf.readLong());
		name = buf.readString(Short.MAX_VALUE);
		nickname = buf.readString(Short.MAX_VALUE);
		recording = buf.readByte();
		afk = buf.readBoolean();
	}

	public void write(PacketBuffer buf) {
		buf.writeLong(uuid.getMostSignificantBits());
		buf.writeLong(uuid.getLeastSignificantBits());
		buf.writeString(name, Short.MAX_VALUE);
		buf.writeString(nickname, Short.MAX_VALUE);
		buf.writeByte(recording);
		buf.writeBoolean(afk);
	}

	public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> FTBEssentials.PROXY.updateTabName(this));
		context.get().setPacketHandled(true);
	}
}