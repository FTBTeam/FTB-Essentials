package dev.ftb.mods.ftbessentials.util;

import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class FTBEWorldData {
	private static final LevelResource FTBESSENTIALS_DIRECTORY = new LevelResource("ftbessentials");
	private static final String DATA_FILE = "data.snbt";

	public static FTBEWorldData instance;

	private final MinecraftServer server;
	private boolean needSave;

	public final Map<String, TeleportPos> warps;

	public FTBEWorldData(MinecraftServer s) {
		server = s;
		warps = new LinkedHashMap<>();
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

	public void saveNow() {
		if (needSave && SNBT.write(mkdirs("").resolve(DATA_FILE), toNBT())) {
			needSave = false;
		}
	}

	public void load() {
		try {
			SNBTCompoundTag tag = SNBT.read(mkdirs("").resolve(DATA_FILE));
			if (tag != null) {
				loadNBT(tag);
			}
		} catch (Exception ex) {
			FTBEssentials.LOGGER.error("Failed to load world data: " + ex);
			ex.printStackTrace();
		}
	}

	private SNBTCompoundTag toNBT() {
		SNBTCompoundTag tag = new SNBTCompoundTag();

		SNBTCompoundTag wm = new SNBTCompoundTag();

		for (Map.Entry<String, TeleportPos> h : warps.entrySet()) {
			wm.put(h.getKey(), h.getValue().write());
		}

		tag.put("warps", wm);

		return tag;
	}

	public void loadNBT(SNBTCompoundTag tag) {
		warps.clear();

		SNBTCompoundTag w = tag.getCompound("warps");

		for (String key : w.getAllKeys()) {
			warps.put(key, new TeleportPos(w.getCompound(key)));
		}
	}
}
