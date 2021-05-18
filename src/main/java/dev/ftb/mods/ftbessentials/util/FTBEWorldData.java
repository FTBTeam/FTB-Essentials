package dev.ftb.mods.ftbessentials.util;

import dev.ftb.mods.ftblibrary.snbt.OrderedCompoundTag;
import net.minecraft.nbt.CompoundTag;
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
	public static final LevelResource FTBESSENTIALS_DIRECTORY = new LevelResource("ftbessentials");

	public static FTBEWorldData instance;

	public final MinecraftServer server;
	public boolean save;

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

	public void save() {
		save = true;
	}

	public CompoundTag write() {
		CompoundTag tag = new OrderedCompoundTag();

		CompoundTag wm = new CompoundTag();

		for (Map.Entry<String, TeleportPos> h : warps.entrySet()) {
			wm.put(h.getKey(), h.getValue().write());
		}

		tag.put("warps", wm);

		return tag;
	}

	public void read(CompoundTag tag) {
		warps.clear();

		CompoundTag w = tag.getCompound("warps");

		for (String key : w.getAllKeys()) {
			warps.put(key, new TeleportPos(w.getCompound(key)));
		}
	}
}
