package dev.ftb.mods.ftbessentials.api.event;

import dev.ftb.mods.ftblibrary.util.result.Outcome;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

// TODO: This isn't mapped to a real event yet!
@FunctionalInterface
public interface RTPEvent {
	Outcome teleport(Data data);

	record Data(ServerLevel level, ServerPlayer serverPlayer, BlockPos pos, int attempt) {}
}
