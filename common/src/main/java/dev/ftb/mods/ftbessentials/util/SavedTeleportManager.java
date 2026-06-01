package dev.ftb.mods.ftbessentials.util;

import dev.ftb.mods.ftbessentials.api.TeleportResult;
import dev.ftb.mods.ftbessentials.api.event.SavedTeleportEvent;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public abstract class SavedTeleportManager {
    private final Map<String,TeleportPos> destinations = new HashMap<>();

    public void addDestination(String name, TeleportPos dest, ServerPlayer player) {
        String nameLower = name.toLowerCase();
        if (destinations.size() >= getMaxSize(player) && !destinations.containsKey(nameLower)) {
            throw new TooManyDestinationsException();
        }
        destinations.put(nameLower, dest);
        SavedTeleportEvent.ADDED.invoker().onAdded(name, dest.asDestination(), player, owningPlayer());
        onChanged();
    }

    public boolean deleteDestination(String name) {
        TeleportPos removed = destinations.remove(name.toLowerCase());
        if (removed != null) {
            SavedTeleportEvent.DELETED.invoker().onDeleted(name, removed.asDestination(), owningPlayer());
            onChanged();
            return true;
        }
        return false;
    }

    public TeleportResult teleportTo(String name, ServerPlayer player, WarmupCooldownTeleporter teleporter) {
        TeleportPos pos = destinations.get(name.toLowerCase());
        if (pos == null) {
            return TeleportResult.UNKNOWN_DESTINATION;
        }

        var result = SavedTeleportEvent.PRE_TELEPORT.invoker().onTeleport(name, player, pos.asDestination(), owningPlayer());
        if (result.isFalse()) {
            return TeleportResult.failed(result.object().reason());
        }

        TeleportPos newPos = result.isEmpty() ? pos : TeleportPos.fromDestination(result.object().dest());

        return newPos != null ? teleporter.teleport(player, p -> newPos) : TeleportResult.UNKNOWN_DESTINATION;
    }

    public Stream<DestinationEntry> destinations() {
        return destinations.entrySet().stream().map(e -> new DestinationEntry(e.getKey(), e.getValue()));
    }

    public CompoundTag writeNBT() {
        SNBTCompoundTag tag = new SNBTCompoundTag();
        destinations.forEach((name, dest) -> tag.put(name, dest.write()));
        return tag;
    }

    public void readNBT(CompoundTag tag) {
        destinations.clear();
        for (String key : tag.getAllKeys()) {
            destinations.put(key, new TeleportPos(tag.getCompound(key)));
        }
    }

    public Set<String> getNames() {
        return destinations.keySet();
    }

    protected int getMaxSize(ServerPlayer player) {
        return Integer.MAX_VALUE;
    }

    protected abstract void onChanged();

    @Nullable
    protected abstract UUID owningPlayer();

    public static class HomeManager extends SavedTeleportManager {
        private final FTBEPlayerData playerData;

        public HomeManager(FTBEPlayerData playerData) {
            this.playerData = playerData;
        }

        @Override
        protected int getMaxSize(ServerPlayer player) {
            return FTBEConfig.MAX_HOMES.get(player);
        }

        @Override
        protected void onChanged() {
            playerData.markDirty();
        }

        @Override
        protected UUID owningPlayer() {
            return playerData.getUuid();
        }
    }

    public static class WarpManager extends SavedTeleportManager {
        private final FTBEWorldData worldData;

        public WarpManager(FTBEWorldData worldData) {
            this.worldData = worldData;
        }

        @Override
        protected void onChanged() {
            worldData.markDirty();
        }

        @Override
        protected UUID owningPlayer() {
            return null;
        }
    }

    public record DestinationEntry(String name, TeleportPos destination) {
    }

    public static class TooManyDestinationsException extends RuntimeException {
    }
}
