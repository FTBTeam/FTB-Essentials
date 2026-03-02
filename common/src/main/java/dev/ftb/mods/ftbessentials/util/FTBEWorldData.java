package dev.ftb.mods.ftbessentials.util;

import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.kit.KitManager;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public class FTBEWorldData {
	private static final LevelResource FTBESSENTIALS_DIRECTORY = new LevelResource("ftbessentials");
	private static final String DATA_FILE = "data.snbt";

	@Nullable
	private static FTBEWorldData instance;

	private final MinecraftServer server;
	private boolean needSave;

	private final SavedTeleportManager.WarpManager warpManager;
	private final Map<UUID, Long> muteTimeouts;

	public FTBEWorldData(MinecraftServer s) {
		server = s;
		warpManager = new SavedTeleportManager.WarpManager(this);
		muteTimeouts = new HashMap<>();
	}

	public static FTBEWorldData getInstance() {
		return Objects.requireNonNull(instance);
	}

	public static void ifAvailable(Consumer<FTBEWorldData> consumer) {
		if (instance != null) consumer.accept(instance);
	}

	public static void startup(MinecraftServer server) {
		instance = new FTBEWorldData(server);
		instance.load();
	}

	public static void shutdown() {
		instance = null;
	}

	public SavedTeleportManager.WarpManager warpManager() {
		return warpManager;
	}

	public Path mkdirs(String path) {
		Path dir = server.getWorldPath(FTBESSENTIALS_DIRECTORY);

		if (!path.isEmpty()) {
			dir = dir.resolve(path);
		}

		if (Files.notExists(dir)) {
			try {
				Files.createDirectories(dir);
			} catch (Exception ex) {
				throw new RuntimeException("Could not create FTB Essentials data directory: " + ex);
			}
		}

		return dir;
	}

	public void markDirty() {
		needSave = true;
	}

	public void saveIfChanged() {
		if (needSave) {
            try {
                SNBT.tryWrite(mkdirs("").resolve(DATA_FILE), toNBT());
            } catch (IOException e) {
				FTBEssentials.LOGGER.error("can't write {} : {} / {}", DATA_FILE, e.getClass().getName(), e.getMessage());
            }
            needSave = false;
		}
	}

	public void load() {
		try {
			Path dataFile = mkdirs("").resolve(DATA_FILE);
			if (!Files.exists(dataFile)) {
				// save a default file
				SNBT.tryWrite(dataFile, toNBT());
			} else {
				loadNBT(SNBT.tryRead(dataFile));
			}
		} catch (Exception ex) {
			FTBEssentials.LOGGER.error("Failed to load world data from {}: {} / {}", DATA_FILE, ex.getClass().getName(), ex.getMessage());
		}
	}

	private SNBTCompoundTag toNBT() {
		SNBTCompoundTag tag = new SNBTCompoundTag();

		tag.put("warps", warpManager.writeNBT());

		SNBTCompoundTag mutesTag = new SNBTCompoundTag();
		muteTimeouts.forEach((id, until) -> mutesTag.putLong(id.toString(), until));
		tag.put("mute_timeouts", mutesTag);

		tag.put("kits", KitManager.getInstance().save(server.registryAccess()));

		return tag;
	}

	public void loadNBT(CompoundTag tag) {
		warpManager.readNBT(tag.getCompoundOrEmpty("warps"));

		muteTimeouts.clear();
		CompoundTag mutesTag = tag.getCompoundOrEmpty("mute_timeouts");
		for (String key : mutesTag.keySet()) {
			mutesTag.getLong(key).ifPresent(l -> muteTimeouts.put(UUID.fromString(key), l));
		}

		KitManager.getInstance().load(tag.getCompoundOrEmpty("kits"), server.registryAccess());
	}

	public void tickMuteTimeouts(MinecraftServer server) {
		long now = System.currentTimeMillis();
		Set<UUID> toExpire = new HashSet<>();
		muteTimeouts.forEach((id, expiry) -> {
			if (now >= expiry) {
				toExpire.add(id);
			}
		});
		toExpire.forEach(id -> {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player != null) {
				player.displayClientMessage(player.getDisplayName().copy().append(" is no longer muted"), false);
			}

			FTBEPlayerData.getOrCreate(server, id).ifPresent(data -> {
				data.setMuted(false);
				data.saveIfChanged();
				FTBEssentials.LOGGER.info("auto-unmuted {} - timeout expired", id);
			});

			muteTimeouts.remove(id);
			markDirty();
		});
	}

	public void setMuteTimeout(ServerPlayer player, long until) {
		if (until > 0) {
			muteTimeouts.put(player.getUUID(), until);
		} else {
			muteTimeouts.remove(player.getUUID());
		}
		markDirty();
	}

	public Optional<Long> getMuteTimeout(ServerPlayer player) {
		return Optional.ofNullable(muteTimeouts.get(player.getUUID()));
	}
}
