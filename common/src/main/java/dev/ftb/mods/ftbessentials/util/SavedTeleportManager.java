package dev.ftb.mods.ftbessentials.util;

import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public abstract class SavedTeleportManager {
    private final Map<String,TeleportPos> destinations = new HashMap<>();

    public void addDestination(String name, TeleportPos dest, ServerPlayer player) {
        String nameLower = name.toLowerCase();
        if (destinations.size() >= getMaxSize(player) && !destinations.containsKey(nameLower)) {
            throw new TooManyDestinationsException();
        }
        destinations.put(nameLower, dest);
        onChanged();
    }

    public boolean deleteDestination(String name) {
        if (destinations.remove(name.toLowerCase()) != null) {
            onChanged();
            return true;
        }
        return false;
    }

    public TeleportPos.TeleportResult teleportTo(String name, ServerPlayer player, WarmupCooldownTeleporter teleporter) {
        TeleportPos pos = destinations.get(name.toLowerCase());
        return pos != null ? teleporter.teleport(player, p -> pos) : TeleportPos.TeleportResult.UNKNOWN_DESTINATION;
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
    }

    public record DestinationEntry(String name, TeleportPos destination) {
    }

    public static class TooManyDestinationsException extends RuntimeException {
    }
}
