package dev.ftb.mods.ftbessentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbessentials.api.records.TPARequest;
import dev.ftb.mods.ftbessentials.commands.FTBCommands;
import dev.ftb.mods.ftbessentials.commands.impl.teleporting.TPACommand;
import dev.ftb.mods.ftbessentials.kit.KitManager;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.FTBEWorldData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import dev.ftb.mods.ftbessentials.util.WarmupCooldownTeleporter;
import dev.ftb.mods.ftblibrary.platform.Platform;
import dev.ftb.mods.ftblibrary.util.TimeUtils;
import dev.ftb.mods.ftblibrary.util.result.Outcome;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

public class FTBEEventHandler {
	public FTBEEventHandler() {
	}

	public void serverAboutToStart(MinecraftServer minecraftServer) {
		FTBEWorldData.startup(minecraftServer);
	}

	public void serverStopped(MinecraftServer minecraftServer) {
		FTBEPlayerData.clear();
		FTBEWorldData.shutdown();
		TPACommand.clearRequests();
	}

	public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection commandSelection) {
		FTBCommands.register(dispatcher);
	}

	public void serverSave(MinecraftServer server) {
		FTBEWorldData.ifAvailable(worldData -> {
			worldData.saveIfChanged();
			FTBEPlayerData.saveAll();
		});
	}

	public void playerLoggedIn(ServerPlayer serverPlayer) {
		FTBEPlayerData.getOrCreate(serverPlayer).ifPresent(data -> {
			data.load();
			data.setLastSeenPos(new TeleportPos(serverPlayer));
			data.markDirty();

			if (!data.getNick().isEmpty()) {
				Platform.get().misc().refreshDisplayName(serverPlayer);
			}

			FTBEPlayerData.sendPlayerTabs(serverPlayer);

			KitManager.getInstance().allKits().forEach((kitName, kit) -> {
				if (kit.autoGrant()) {
                    try {
                        kit.giveToPlayer(kitName, serverPlayer, data, false);
                    } catch (CommandSyntaxException ignored) {
                    }
                }
			});
		});
	}

	public void playerLoggedOut(ServerPlayer serverPlayer) {
		FTBEPlayerData.getOrCreate(serverPlayer).ifPresent(data -> {
			data.setLastSeenPos(new TeleportPos(serverPlayer));
			data.saveIfChanged();
		});
	}

	private void tickPlayer(ServerPlayer player) {
		FTBEPlayerData.getOrCreate(player).ifPresent(data -> {
			var abilities = player.getAbilities();

			if (data.isGod() && !abilities.invulnerable) {
				abilities.invulnerable = true;
				player.onUpdateAbilities();
			}

			if (data.canFly() && !abilities.mayfly) {
				abilities.mayfly = true;
				player.onUpdateAbilities();
			}
		});
	}

	public void serverTickPost(MinecraftServer server) {
		long now = System.currentTimeMillis();

		Iterator<TPARequest> iterator = TPACommand.requests().values().iterator();

		while (iterator.hasNext()) {
			TPARequest r = iterator.next();

			if (now > r.created() + 60000L) {
				ServerPlayer source = server.getPlayerList().getPlayer(r.source().getUuid());
				ServerPlayer target = server.getPlayerList().getPlayer(r.target().getUuid());

				if (source != null) {
					source.sendSystemMessage(Component.translatable("ftbessentials.tpa.expired"));
				}

				if (target != null) {
					target.sendSystemMessage(Component.translatable("ftbessentials.tpa.expired"));
				}

				iterator.remove();
			}
		}

		if (server.getTickCount() % 20 == 0) {
			WarmupCooldownTeleporter.tickWarmups(server);
			FTBEWorldData.getInstance().tickMuteTimeouts(server);
		}

		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		for (ServerPlayer player : players) {
			tickPlayer(player);
		}
	}

	// FIXME this should run with HIGHEST priority
	// TODO: This is easy with Forge but I'm not sure how we do it with fabric.
    public Outcome allowChat(@Nullable ServerPlayer serverPlayer) {
		return FTBEPlayerData.getOrCreate(serverPlayer).map(data -> {
			if (data.isMuted()) {
				// serverPlayer must be non-null if we got the player data
				//noinspection DataFlowIssue
				serverPlayer.sendSystemMessage(Component.translatable("ftbessentials.muted").withStyle(ChatFormatting.RED));
				FTBEWorldData.getInstance().getMuteTimeout(serverPlayer).ifPresent(expiry -> {
					long left = (expiry - System.currentTimeMillis()) / 1000L;
					serverPlayer.sendSystemMessage(Component.translatable("ftbessentials.mute_expiry",
							TimeUtils.prettyTimeString(left)).withStyle(ChatFormatting.RED));
				});
				return Outcome.FAIL;
			}
			return Outcome.PASS;
		}).orElse(Outcome.PASS);
	}

	// TODO: wonGame is actually "alive" on fabric so we should check this functionality as it might be wrong
	//       post the move away from arch
	public void onPlayerDeath(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean wonGame) {
		// this is better than checking for living death event, because player cloning isn't cancellable
		// the player death event is cancellable, and we can't detect cancelled events with Architectury
		if (!wonGame) {
			// note: architectury changed the parameter order for the player clone event
			// we can work with old and new versions of arch if we check which player is dead, and use that location
			if (newPlayer.isAlive()) {
				oldPlayer.getLastDeathLocation().ifPresent(loc -> FTBEPlayerData.addTeleportHistory(oldPlayer));
			} else if (oldPlayer.isAlive()) {
				newPlayer.getLastDeathLocation().ifPresent(loc -> FTBEPlayerData.addTeleportHistory(newPlayer));
			}
		}
	}

	public void onPlayerHurt(LivingEntity livingEntity, float amount, boolean blocked) {
		if (livingEntity instanceof ServerPlayer sp && amount > 0f && !blocked) {
			WarmupCooldownTeleporter.cancelWarmup(sp);
		}
	}

	public void playerChangedDimension(ServerPlayer serverPlayer) {
		WarmupCooldownTeleporter.cancelWarmup(serverPlayer);
	}
}
