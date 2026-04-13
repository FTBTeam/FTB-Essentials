package dev.ftb.mods.ftbessentials.util;

import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import de.marhali.json5.Json5Primitive;
import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.kit.KitManager;
import dev.ftb.mods.ftblibrary.json5.Json5Util;
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
	private static final String DATA_FILE = "data.json5";

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

		try {
			Files.createDirectories(dir);
			return dir;
		} catch (IOException ex) {
			throw new RuntimeException("Could not create FTB Essentials data directory: " + ex.getMessage());
		}
	}

	public void markDirty() {
		needSave = true;
	}

	public void saveIfChanged() {
		if (needSave) {
			try {
				Json5Util.tryWrite(mkdirs("").resolve(DATA_FILE), (Json5Element) toJson());
			} catch (IOException e) {
				FTBEssentials.LOGGER.error("can't write {} : {} / {}", DATA_FILE, e.getClass().getName(), e.getMessage());
			}
			needSave = false;
		}
	}

	public void load() {
		try {
			Path dataFile = mkdirs("").resolve(DATA_FILE);
			if (Files.exists(dataFile)) {
				deserialize(Json5Util.tryRead(dataFile));
			} else {
				// save a default file
				Json5Util.tryWrite(dataFile, (Json5Element) toJson());
				FTBEssentials.LOGGER.info("created default world data file at {}", dataFile);
			}
		} catch (Exception ex) {
			FTBEssentials.LOGGER.error("Failed to load world data from {}: {} / {}", DATA_FILE, ex.getClass().getName(), ex.getMessage());
		}
	}

	private Json5Object toJson() {
		Json5Object tag = new Json5Object();

		tag.add("warps", warpManager.toJson());

		Json5Object mutesTag = new Json5Object();
		muteTimeouts.forEach((id, until) -> mutesTag.addProperty(id.toString(), until));
		tag.add("mute_timeouts", mutesTag);

		tag.add("kits", KitManager.getInstance().save(server.registryAccess()));

		return tag;
	}

	public void deserialize(Json5Object json) {
		Json5Util.getJson5Object(json, "warps").ifPresent(warpManager::readJson);

		muteTimeouts.clear();
		Json5Util.getJson5Object(json, "mute_timeouts").ifPresent(mutesTag -> {
			mutesTag.asMap().forEach((key, el) -> {
				if (el instanceof Json5Primitive p && p.isNumber()) {
					muteTimeouts.put(UUID.fromString(key), p.getAsLong());
				}
			});
		});

		KitManager.getInstance().load(json.get("kits"), server.registryAccess());
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
				player.sendSystemMessage(player.getDisplayName().copy().append(" is no longer muted"));
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
