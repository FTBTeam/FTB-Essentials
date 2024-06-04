package dev.ftb.mods.ftbessentials.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData.RecordingStatus;
import dev.ftb.mods.ftblibrary.util.NetworkHelper;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record UpdateTabNameMessage(UUID uuid, String name, String nickname, RecordingStatus recording, boolean afk) implements CustomPacketPayload {
	public static final Type<UpdateTabNameMessage> TYPE = new Type<>(new ResourceLocation(FTBEssentials.MOD_ID, "update_tab_name"));

	public static StreamCodec<FriendlyByteBuf, UpdateTabNameMessage> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, UpdateTabNameMessage::uuid,
			ByteBufCodecs.STRING_UTF8, UpdateTabNameMessage::name,
			ByteBufCodecs.STRING_UTF8, UpdateTabNameMessage::nickname,
			NetworkHelper.enumStreamCodec(RecordingStatus.class), UpdateTabNameMessage::recording,
			ByteBufCodecs.BOOL, UpdateTabNameMessage::afk,
			UpdateTabNameMessage::new
	);

	public static void handle(UpdateTabNameMessage message, NetworkManager.PacketContext packetContext) {
		packetContext.queue(() -> FTBEssentials.PROXY.updateTabName(message));
	}

	@Override
	public Type<UpdateTabNameMessage> type() {
		return TYPE;
	}
}
