package dev.ftb.mods.ftbessentials.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * @author LatvianModder
 */
@Cancelable
public class RTPEvent extends PlayerEvent {
	private final ServerLevel world;
	private final ServerPlayer serverPlayer;
	private final BlockPos pos;
	private final int attempt;

	public RTPEvent(ServerLevel w, ServerPlayer player, BlockPos p, int a) {
		super(player);
		world = w;
		serverPlayer = player;
		pos = p;
		attempt = a;
	}

	public ServerLevel getServerWorld() {
		return world;
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
