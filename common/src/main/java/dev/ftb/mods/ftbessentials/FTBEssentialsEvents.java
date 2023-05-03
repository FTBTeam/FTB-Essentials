package dev.ftb.mods.ftbessentials;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.EventResult;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class FTBEssentialsEvents {
    public static final Event<RTP> RTP_EVENT = EventFactory.createEventResult();

    public interface RTP {
        EventResult teleport(ServerLevel level, ServerPlayer serverPlayer, BlockPos pos, int attempt);
    }
}
