package dev.ftb.mods.ftbessentials.util;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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
	private final Map<UUID,Long> muteTimeouts;

	public FTBEWorldData(MinecraftServer s) {
		server = s;
		warps = new LinkedHashMap<>();
		muteTimeouts = new HashMap<>();
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

		SNBTCompoundTag warpsTag = new SNBTCompoundTag();
		warps.forEach((key, value) -> warpsTag.put(key, value.write()));
		tag.put("warps", warpsTag);

		SNBTCompoundTag mutesTag = new SNBTCompoundTag();
		muteTimeouts.forEach((id, until) -> mutesTag.putLong(id.toString(), until));
		tag.put("mute_timeouts", mutesTag);

		return tag;
	}

	public void loadNBT(SNBTCompoundTag tag) {
		warps.clear();
		SNBTCompoundTag warpsTag = tag.getCompound("warps");
		for (String key : warpsTag.getAllKeys()) {
			warps.put(key, new TeleportPos(warpsTag.getCompound(key)));
		}

		muteTimeouts.clear();
		SNBTCompoundTag mutesTag = tag.getCompound("mute_timeouts");
		for (String key : mutesTag.getAllKeys()) {
			muteTimeouts.put(UUID.fromString(key), mutesTag.getLong(key));
		}
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
			FTBEPlayerData data = FTBEPlayerData.get(player == null ? new GameProfile(id, "?") : player.getGameProfile());
			if (data != null) {
				data.muted = false;
				data.markDirty();
				if (player == null) {
					data.saveNow();  // ensure data for offline player is correct before they log in again
				}
				FTBEssentials.LOGGER.info("auto-unmuted {} - timeout expired", id);
			} else {
				FTBEssentials.LOGGER.warn("can't auto-unmute {} - player id not known?", id);
			}
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
