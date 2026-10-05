package dev.ftb.mods.ftbessentials.api;

import dev.ftb.mods.ftbessentials.api.event.SavedTeleportEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;

/// Represents a teleport destination used by the various teleportation commands in the mod.
///
/// @param dimension the dimension
/// @param pos       the block position
/// @param yRot      the player's Y rotation (yaw) on arrival (if empty, use player's current yaw)
/// @param xRot      the player's X rotation (pitch) on arrival (if empty, use player's current pitch)
public record TeleportDestination(
        ResourceKey<Level> dimension,
        BlockPos pos,
        Optional<Float> yRot,
        Optional<Float> xRot
) {
    public TeleportDestination withPos(BlockPos newPos) {
        return new TeleportDestination(dimension, newPos, yRot, xRot);
    }

    /// Return a successful outcome for this destination. See also [dev.ftb.mods.ftbessentials.api.event.SavedTeleportEvent.PreTeleport#preTeleport(SavedTeleportEvent.PreTeleport.Data)].
    ///
    /// @return a successful outcome
    public Outcome success() {
        return new Outcome(true, this, Component.empty());
    }

    /// Return a successful outcome with a new destination.
    ///
    /// @return a successful outcome
    public Outcome success(TeleportDestination newDest) {
        return new Outcome(true, newDest, Component.empty());
    }

    /// Return a successful outcome for this destination. See also [dev.ftb.mods.ftbessentials.api.event.SavedTeleportEvent.PreTeleport#preTeleport(SavedTeleportEvent.PreTeleport.Data)].
    ///
    /// @param reason the failure reason to report to the player
    /// @return a failed outcome
    public Outcome failed(Component reason) {
        return new Outcome(false,this, reason);
    }

    public record Outcome(boolean success, TeleportDestination dest, Component reason) {
    }
}
