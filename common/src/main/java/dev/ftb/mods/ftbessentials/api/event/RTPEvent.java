package dev.ftb.mods.ftbessentials.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class RTPEvent {
	private final ServerLevel level;
	private final ServerPlayer serverPlayer;
	private final BlockPos pos;
	private final int attempt;

	public RTPEvent(ServerLevel level, ServerPlayer serverPlayer, BlockPos pos, int attempt) {
		this.level = level;
		this.serverPlayer = serverPlayer;
		this.pos = pos;
		this.attempt = attempt;
	}

	public ServerLevel getServerWorld() {
		return level;
	}

	public ServerPlayer getServerPlayer() {
		return serverPlayer;
	}

	public BlockPos getPos() {
		return pos;
	}

	public int getAttempt() {
		return attempt;
	}
}
