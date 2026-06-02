package dev.ftb.mods.ftbessentials.api.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.ftb.mods.ftbessentials.api.TeleportDestination;
import net.minecraft.server.level.ServerPlayer;

public class TeleportImmediateEvent {
    public static Event<TeleportImmediate> TELEPORT = EventFactory.createLoop();

    @FunctionalInterface
    public interface TeleportImmediate {
        /**
         * Fired when the player is just about to teleport, when any possible teleportation warmup is complete.
         * <p>
         * Teleportation can be prevented by returning a failed outcome e.g. {@code destination.failed(reason)},
         * or a modified destination e.g. {@code detination.success(newdest)}.
         *
         * @param player the player about to teleport
         * @param destination the teleport destination
         * @return the event outcome
         */
        TeleportDestination.Outcome teleport(ServerPlayer player, TeleportDestination destination);
    }
}
