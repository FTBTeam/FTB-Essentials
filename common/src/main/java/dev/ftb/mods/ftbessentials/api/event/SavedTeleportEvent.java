package dev.ftb.mods.ftbessentials.api.event;

import dev.ftb.mods.ftbessentials.api.TeleportDestination;
import dev.ftb.mods.ftblibrary.platform.event.TypedEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

@FunctionalInterface
public interface SavedTeleportEvent extends Consumer<SavedTeleportEvent.Data> {
    /// Fired after a saved destination (home or warp) has been added or deleted.
    ///
    /// @param name the name of the destination
    /// @param dest the destination that was added
    /// @param owningPlayerId the player's UUID for a home destination, or null for a global warp destination
    /// @param adding true if adding a destination, false if deleting a destination
    record Data(String name, TeleportDestination dest, @Nullable UUID owningPlayerId, boolean adding) {
    }

    @FunctionalInterface
    interface PreTeleport {
        TypedEvent<PreTeleport.Data, TeleportDestination.Outcome> TYPE = TypedEvent.of(PreTeleport.Data.class);

        /// Fired when a player is about to teleport to a saved destination (home or warp). This event allows
        /// the destination to be modified, or the teleportation to be prevented entirely.
        ///
        ///   - To modify the destination, return [TeleportDestination#success(TeleportDestination)] with a new destination
        ///   - To prevent teleportation, return [TeleportDestination#failed(Component)] with a reason for the failure
        ///   - To proceed with the default behavior, return [TeleportDestination#success()] with no argument
        ///
        /// See also [TeleportEvent], which is fired _after_ this event, and provides a second opportunity to
        /// prevent teleportation.
        ///
        /// @param data the teleport data
        /// @return the outcome, see above
        TeleportDestination.Outcome preTeleport(PreTeleport.Data data);

        /// @param name the name of the saved destination
        /// @param player the player about to teleport
        /// @param dest the planned teleport destination
        /// @param owningPlayer UUID of the player who owns the destination; non-null for a player home, null for a global warp
        record Data(String name, ServerPlayer player, TeleportDestination dest, @Nullable UUID owningPlayer) {
        }
    }
}
