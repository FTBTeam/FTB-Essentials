package dev.ftb.mods.ftbessentials.util;

import de.marhali.json5.Json5Object;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
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

    public Json5Object toJson() {
        Json5Object tag = new Json5Object();
        destinations.forEach((name, dest) -> tag.add(name, dest.toJson()));
        return tag;
    }

    public void readJson(Json5Object json) {
        destinations.clear();
        for (String key : json.keySet()) {
            destinations.put(key, TeleportPos.fromJson(json.get(key)));
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
