package dev.ftb.mods.ftbessentials.api;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/**
 * Represents a teleport destination used by the various teleportation commands in the mod.
 *
 * @param dimension the dimension
 * @param pos the block position
 * @param yRot the player's Y rotation (yaw) on arrival (if empty, use player's current yaw)
 * @param xRot the player's X rotation (pitch) on arrival (if empty, use player's current pitch)
 */
public record TeleportDestination(ResourceKey<Level> dimension, BlockPos pos, Optional<Float> yRot, Optional<Float> xRot) {
    /**
     * Return a successful outcome for this destination. See also {@link dev.ftb.mods.ftbessentials.api.event.SavedTeleportEvent.PreTeleport#onTeleport(String, ServerPlayer, TeleportDestination, UUID)}.
     *
     * @return a successful outcome
     */
    public Outcome success() {
        return new Outcome(true, this, Component.empty());
    }

    /**
     * Return a successful outcome for this destination. See also {@link dev.ftb.mods.ftbessentials.api.event.SavedTeleportEvent.PreTeleport#onTeleport(String, ServerPlayer, TeleportDestination, UUID)}.
     *
     * @param reason the failure reason to report to the player
     * @return a failed outcome
     */
    public Outcome failed(Component reason) {
        return new Outcome(false, this, reason);
    }

    public record Outcome(boolean success, TeleportDestination dest, Component reason) {
    }
}
