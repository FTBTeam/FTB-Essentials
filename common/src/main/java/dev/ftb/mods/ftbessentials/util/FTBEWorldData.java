package dev.ftb.mods.ftbessentials.util;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.kit.KitManager;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FTBEWorldData {
	private static final LevelResource FTBESSENTIALS_DIRECTORY = new LevelResource("ftbessentials");
	private static final String DATA_FILE = "data.snbt";

	public static FTBEWorldData instance;

	private final MinecraftServer server;
	private boolean needSave;

	private final SavedTeleportManager.WarpManager warpManager;
	private final Map<UUID,Long> muteTimeouts;

	public FTBEWorldData(MinecraftServer s) {
		server = s;
		warpManager = new SavedTeleportManager.WarpManager(this);
		muteTimeouts = new HashMap<>();
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

		tag.put("warps", warpManager.writeNBT());

		SNBTCompoundTag mutesTag = new SNBTCompoundTag();
		muteTimeouts.forEach((id, until) -> mutesTag.putLong(id.toString(), until));
		tag.put("mute_timeouts", mutesTag);

		tag.put("kits", KitManager.getInstance().save());

		return tag;
	}

	public void loadNBT(SNBTCompoundTag tag) {
		warpManager.readNBT(tag.getCompound("warps"));

		muteTimeouts.clear();
		SNBTCompoundTag mutesTag = tag.getCompound("mute_timeouts");
		for (String key : mutesTag.getAllKeys()) {
			muteTimeouts.put(UUID.fromString(key), mutesTag.getLong(key));
		}

		KitManager.getInstance().load(tag.getCompound("kits"));
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
			FTBEPlayerData.getOrCreate(new GameProfile(id, "")).ifPresent(data -> {
				data.setMuted(false);
				if (player == null) {
					data.saveIfChanged();  // ensure data for offline player is correct before they log in again
				}
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
