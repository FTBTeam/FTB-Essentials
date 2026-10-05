package dev.ftb.mods.ftbessentials.util;

import de.marhali.json5.Json5Object;
import dev.ftb.mods.ftbessentials.api.TeleportResult;
import dev.ftb.mods.ftbessentials.api.event.SavedTeleportEvent;
import dev.ftb.mods.ftbessentials.config.FTBEStartupConfig;
import dev.ftb.mods.ftblibrary.platform.event.NativeEventPosting;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public abstract class SavedTeleportManager {
    private final Map<String,TeleportPos> destinations = new HashMap<>();

    public void addDestination(String name, TeleportPos dest, ServerPlayer player) {
        String nameLower = name.toLowerCase(Locale.ROOT);
        if (destinations.size() >= getMaxSize(player) && !destinations.containsKey(nameLower)) {
            throw new TooManyDestinationsException();
        }
        destinations.put(nameLower, dest);
        NativeEventPosting.get().postEvent(new SavedTeleportEvent.Data(nameLower, dest.asDestination(), owningPlayer(), true));
        onChanged();
    }

    @Nullable
    protected abstract UUID owningPlayer();

    public boolean deleteDestination(String name) {
        String nameLower = name.toLowerCase(Locale.ROOT);
        TeleportPos removed = destinations.remove(nameLower);
        if (removed != null) {
            NativeEventPosting.get().postEvent(new SavedTeleportEvent.Data(nameLower, removed.asDestination(), owningPlayer(), false));
            onChanged();
            return true;
        }
        return false;
    }

    public TeleportResult teleportTo(String name, ServerPlayer player, WarmupCooldownTeleporter teleporter) {
        String nameLower = name.toLowerCase(Locale.ROOT);
        TeleportPos pos = destinations.get(nameLower);
        if (pos == null) {
            return TeleportResult.UNKNOWN_DESTINATION;
        }

        var outcome = NativeEventPosting.get().postEventWithResult(
                SavedTeleportEvent.PreTeleport.TYPE,
                new SavedTeleportEvent.PreTeleport.Data(nameLower, player, pos.asDestination(), owningPlayer())
        );
        if (!outcome.success()) {
            return TeleportResult.failed(outcome.reason());
        }

        return teleporter.teleport(player, _ -> TeleportPos.fromDestination(outcome.dest()));
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
        protected @Nullable UUID owningPlayer() {
            return playerData.getUuid();
        }

        @Override
        protected int getMaxSize(ServerPlayer player) {
            return FTBEStartupConfig.MAX_HOMES.get(player);
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
        protected @Nullable UUID owningPlayer() {
            return null;
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
