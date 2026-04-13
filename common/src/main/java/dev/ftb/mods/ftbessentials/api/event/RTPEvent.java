package dev.ftb.mods.ftbessentials.api.event;

import dev.ftb.mods.ftblibrary.platform.event.TypedEvent;
import dev.ftb.mods.ftblibrary.util.result.DataOutcome;
import dev.ftb.mods.ftblibrary.util.result.Outcome;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface RTPEvent {
	TypedEvent<Data, Outcome> TYPE = TypedEvent.of(RTPEvent.Data.class);

	Outcome teleport(Data data);

	record Data(ServerLevel level, ServerPlayer serverPlayer, BlockPos pos, int attempt) {}
}
