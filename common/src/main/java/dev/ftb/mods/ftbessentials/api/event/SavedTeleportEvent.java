package dev.ftb.mods.ftbessentials.api.event;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.ftb.mods.ftbessentials.api.TeleportDestination;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface SavedTeleportEvent {
    /**
     * See {@link #onAdded(String, TeleportDestination, ServerPlayer, UUID)}
     */
    Event<SavedTeleportEvent> ADDED = EventFactory.createLoop();
    /**
     * See {@link #onDeleted(String, TeleportDestination, UUID)}
     */
    Event<SavedTeleportEvent> DELETED = EventFactory.createLoop();
    /**
     * See {@link PreTeleport#onTeleport(String, ServerPlayer, TeleportDestination, UUID)}
     */
    Event<PreTeleport> PRE_TELEPORT = EventFactory.createCompoundEventResult();

    /**
     * Fired after a saved destination (home or warp) has been added.
     *
     * @param name the name of the destination
     * @param dest the destination that was added
     * @param player the player who added the destination
     * @param owningPlayer the player's UUID for a home destination, or null for a global warp destination
     */
    void onAdded(String name, TeleportDestination dest, ServerPlayer player, @Nullable UUID owningPlayer);

    /**
     * Fired after a saved destination (home or warp) has been deleted.
     *
     * @param name the name of the destination
     * @param destination the destination that was removed
     * @param owningPlayer the player's UUID for a home destination, or null for a global warp destination
     */
    void onDeleted(String name, TeleportDestination destination, @Nullable UUID owningPlayer);

    @FunctionalInterface
    interface PreTeleport {
        /**
         * Fired when a player is about to teleport to a saved destination (home or warp). This event allows
         * the destination to be modified, or the teleportation to be prevented entirely.
         * <ul>
         * <li>To modify the destination, return {@link CompoundEventResult#interruptTrue(Object)} with a new
         * {@link TeleportDestination.Outcome} - see {@link TeleportDestination#success()}</li>
         * <li>To prevent teleportation, return {@link CompoundEventResult#interruptFalse(Object)} - see {@link TeleportDestination#failed(Component)}</li>
         * <li>To proceed with the default behavior, return {@link CompoundEventResult#pass()}</li>
         * </ul>
         * See also {@link TeleportEvent}, which is fired <em>after</em> this event, and provides a second opportunity to
         * prevent teleportation.
         *
         * @param name the name of the saved destination
         * @param player the player about to teleport
         * @param dest the planned teleport destination
         * @param owningPlayer UUID of the player who owns the destination; non-null for a player home, null for a global warp
         * @return the event result (see above)
         */
        CompoundEventResult<TeleportDestination.Outcome> onTeleport(String name, ServerPlayer player, TeleportDestination dest, @Nullable UUID owningPlayer);
    }
}
