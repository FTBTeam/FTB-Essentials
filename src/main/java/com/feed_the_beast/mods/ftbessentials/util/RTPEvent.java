package com.feed_the_beast.mods.ftbessentials.util;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * @author LatvianModder
 */
@Cancelable
public class RTPEvent extends PlayerEvent {
	private final ServerWorld world;
	private final ServerPlayerEntity serverPlayer;
	private final BlockPos pos;
	private final int attempt;

	public RTPEvent(ServerWorld w, ServerPlayerEntity player, BlockPos p, int a) {
		super(player);
		world = w;
		serverPlayer = player;
		pos = p;
		attempt = a;
	}

	public ServerWorld getServerWorld() {
		return world;
	}

	public ServerPlayerEntity getServerPlayer() {
		return serverPlayer;
	}

	public BlockPos getPos() {
		return pos;
	}

	public int getAttempt() {
		return attempt;
	}
}
