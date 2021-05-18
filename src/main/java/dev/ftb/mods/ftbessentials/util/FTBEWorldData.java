package dev.ftb.mods.ftbessentials.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

	public JsonObject toJson() {
		JsonObject json = new JsonObject();

		JsonObject wm = new JsonObject();

		for (Map.Entry<String, TeleportPos> h : warps.entrySet()) {
			wm.add(h.getKey(), h.getValue().toJson());
		}

		json.add("warps", wm);

		return json;
	}

	public void fromJson(JsonObject json) {
		warps.clear();

		if (json.has("warps")) {
			for (Map.Entry<String, JsonElement> e : json.get("warps").getAsJsonObject().entrySet()) {
				warps.put(e.getKey(), new TeleportPos(e.getValue().getAsJsonObject()));
			}
		}
	}
}
