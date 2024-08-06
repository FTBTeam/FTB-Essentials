package dev.ftb.mods.ftbessentials.util;

import dev.architectury.hooks.level.entity.PlayerHooks;
import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.net.UpdateTabNameMessage;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.util.NetworkHelper;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class FTBEPlayerData {
	private static final Logger LOGGER = LoggerFactory.getLogger(FTBEPlayerData.class);

	private static final String PLAYER_DATA_PATH = "playerdata";
	private static final Map<UUID, FTBEPlayerData> MAP = new HashMap<>();

	private final UUID uuid;
	private final String name;

	private boolean needSave;

	private boolean muted;
	private boolean canFly;
	private boolean god;
	private String nick;
	private TeleportPos lastSeenPos;
	private final SavedTeleportManager.HomeManager homes;
	private RecordingStatus recording;
	private final Map<String,Long> kitUseTimes;

	public final WarmupCooldownTeleporter backTeleporter;
	public final WarmupCooldownTeleporter spawnTeleporter;
	public final WarmupCooldownTeleporter warpTeleporter;
	public final WarmupCooldownTeleporter homeTeleporter;
	public final WarmupCooldownTeleporter tpaTeleporter;
	public final WarmupCooldownTeleporter rtpTeleporter;

	public final LinkedList<TeleportPos> teleportHistory;

	public FTBEPlayerData(UUID uuid, String name) {
		this.uuid = uuid;
		this.name = name;

		needSave = false;

		muted = false;
		canFly = false;
		god = false;
		nick = "";
		lastSeenPos = new TeleportPos(Level.OVERWORLD, BlockPos.ZERO);
		recording = RecordingStatus.NONE;

		kitUseTimes = new HashMap<>();
		homes = new SavedTeleportManager.HomeManager(this);

		backTeleporter = new WarmupCooldownTeleporter(this, FTBEConfig.BACK::getCooldown, FTBEConfig.BACK::getWarmup, true);
		spawnTeleporter = new WarmupCooldownTeleporter(this, FTBEConfig.SPAWN::getCooldown, FTBEConfig.SPAWN::getWarmup);
		warpTeleporter = new WarmupCooldownTeleporter(this, FTBEConfig.WARP::getCooldown, FTBEConfig.WARP::getWarmup);
		homeTeleporter = new WarmupCooldownTeleporter(this, FTBEConfig.HOME::getCooldown, FTBEConfig.HOME::getWarmup);
		tpaTeleporter = new WarmupCooldownTeleporter(this, FTBEConfig.TPA::getCooldown, FTBEConfig.TPA::getWarmup);
		rtpTeleporter = new WarmupCooldownTeleporter(this, FTBEConfig.RTP::getCooldown, FTBEConfig.RTP::getWarmup);
		teleportHistory = new LinkedList<>();
	}

	public static void cleanupKitCooldowns(String kitName) {
		MAP.values().forEach(data -> data.setLastKitUseTime(kitName, 0L));
	}

	public UUID getUuid() {
		return uuid;
	}

	public String getName() {
		return name;
	}

	public boolean isMuted() {
		return muted;
	}

	public void setMuted(boolean muted) {
		if (muted != this.muted) {
			this.muted = muted;
			markDirty();
		}
	}

	public boolean canFly() {
		return canFly;
	}

	public void setCanFly(boolean canFly) {
		if (canFly != this.canFly) {
			this.canFly = canFly;
			markDirty();
		}
	}

	public boolean isGod() {
		return god;
	}

	public void setGod(boolean god) {
		if (god != this.god) {
			this.god = god;
			markDirty();
		}
	}

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		if (!nick.equals(this.nick)) {
			this.nick = nick;
			markDirty();
		}
	}

	public TeleportPos getLastSeenPos() {
		return lastSeenPos;
	}

	public void setLastSeenPos(TeleportPos lastSeenPos) {
		this.lastSeenPos = lastSeenPos;
		markDirty();
	}

	public RecordingStatus getRecording() {
		return recording;
	}

	public void setRecording(RecordingStatus recording) {
		if (recording != this.recording) {
			this.recording = recording;
			markDirty();
		}
	}

	public SavedTeleportManager.HomeManager homeManager() {
		return homes;
	}

	public static Optional<FTBEPlayerData> getOrCreate(MinecraftServer server, UUID playerId) {;
		if (MAP.containsKey(playerId)) {
			return Optional.of(MAP.get(playerId));
		}

		// Check if the player file exists
		if (server.getProfileCache() == null) {
			return Optional.empty();
		}

        return server.getProfileCache().get(playerId)
				.map(profile -> MAP.computeIfAbsent(playerId, k -> new FTBEPlayerData(playerId, profile.getName())));
    }

	public static Optional<FTBEPlayerData> getOrCreate(Player player) {
		if (player == null || PlayerHooks.isFake(player)) {
			return Optional.empty();
		}

		return Optional.of(MAP.computeIfAbsent(player.getUUID(), k -> new FTBEPlayerData(player.getUUID(), player.getGameProfile().getName())));
	}

	public static boolean playerExists(UUID playerId) {
		return MAP.containsKey(playerId);
	}

	public static void addTeleportHistory(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos) {
		getOrCreate(player).ifPresent(data -> data.addTeleportHistory(player, new TeleportPos(dimension, pos)));
	}

	public static void addTeleportHistory(ServerPlayer player) {
		addTeleportHistory(player, player.level().dimension(), player.blockPosition());
	}

	public static void clear() {
		MAP.clear();
	}

	public static void saveAll() {
		MAP.values().forEach(FTBEPlayerData::saveIfChanged);
	}

	public static void sendPlayerTabs(ServerPlayer serverPlayer) {
		MAP.values().forEach(d -> d.sendTabName(serverPlayer));
	}

	public static void forEachPlayer(Consumer<FTBEPlayerData> consumer) {
		MAP.values().forEach(consumer);
	}

	public void markDirty() {
		needSave = true;
	}

	public SNBTCompoundTag write() {
		SNBTCompoundTag nbt = new SNBTCompoundTag();
		nbt.putBoolean("muted", muted);
		nbt.putBoolean("fly", canFly);
		nbt.putBoolean("god", god);
		nbt.putString("nick", nick);
		nbt.put("last_seen", lastSeenPos.write());
		nbt.putString("recording", recording.getId());

		ListTag tph = new ListTag();

		for (TeleportPos pos : teleportHistory) {
			tph.add(pos.write());
		}

		nbt.put("teleport_history", tph);

		nbt.put("homes", homes.writeNBT());

		nbt.put("kit_use_times", Util.make(new CompoundTag(), tag -> kitUseTimes.forEach(tag::putLong)));

		return nbt;
	}

	public void read(CompoundTag tag) {
		muted = tag.getBoolean("muted");
		canFly = tag.getBoolean("fly");
		god = tag.getBoolean("god");
		nick = tag.getString("nick");
		recording = RecordingStatus.NAME_MAP.map.getOrDefault(tag.getString("recording"), RecordingStatus.NONE);
		lastSeenPos = tag.contains("last_seen") ? new TeleportPos(tag.getCompound("last_seen")) : null;

		teleportHistory.clear();

		ListTag th = tag.getList("teleport_history", Tag.TAG_COMPOUND);

		for (int i = 0; i < th.size(); i++) {
			teleportHistory.add(new TeleportPos(th.getCompound(i)));
		}

		kitUseTimes.clear();
		CompoundTag kitTag = tag.getCompound("kit_use_times");
		for (String name : kitTag.getAllKeys()) {
			kitUseTimes.put(name, kitTag.getLong(name));
		}

		homes.readNBT(tag.getCompound("homes"));
	}

	public void addTeleportHistory(ServerPlayer player, TeleportPos pos) {
		teleportHistory.add(pos);

		while (teleportHistory.size() > FTBEConfig.MAX_BACK.get(player)) {
			teleportHistory.removeFirst();
		}

		markDirty();
	}

	public void popTeleportHistory() {
		if (!teleportHistory.isEmpty()) {
			teleportHistory.removeLast();
			markDirty();
		} else {
			FTBEssentials.LOGGER.warn("attempted to pop empty back history for {}", uuid);
		}
	}

	public void load() {
		CompoundTag tag = SNBT.read(FTBEWorldData.instance.mkdirs(PLAYER_DATA_PATH).resolve(uuid + ".snbt"));

		if (tag != null) {
			read(tag);
		}
	}

	public void saveIfChanged() {
		if (needSave && SNBT.write(FTBEWorldData.instance.mkdirs(PLAYER_DATA_PATH).resolve(uuid + ".snbt"), write())) {
			needSave = false;
		}
	}

	public void sendTabName(MinecraftServer server) {
		NetworkHelper.sendToAll(server, new UpdateTabNameMessage(uuid, name, nick, recording, false));
	}

	public void sendTabName(ServerPlayer to) {
		NetworkHelper.sendTo(to, new UpdateTabNameMessage(uuid, name, nick, recording, false));
	}

	public long getLastKitUseTime(String kitName) {
		return kitUseTimes.getOrDefault(kitName, 0L);
	}

	public void setLastKitUseTime(String kitName, long when) {
		if (when == 0L) {
			if (kitUseTimes.remove(kitName) != null) {
				markDirty();
			}
		} else {
			kitUseTimes.put(kitName, when);
			markDirty();
		}
	}

	/**
	 * Get all the known players UUID's from the playerdata folder
	 * @return a list of all the known players UUID's
	 */
	public static List<UUID> getAllKnownPlayers() {
		try (Stream<Path> files = Files.list(FTBEWorldData.instance.mkdirs(PLAYER_DATA_PATH))) {
			return files.filter(path -> path.toString().endsWith(".snbt"))
					.map(path -> tryParseUUID(path.getFileName().toString().replace(".snbt", "")))
					.filter(Objects::nonNull)
					.toList();
		} catch (Exception ex) {
			LOGGER.error("Failed to get all known players: " + ex);
		}

		return Collections.emptyList();
	}

	/**
	 * Attempt to parse a UUID from a string without fatal errors
	 * @param inputUUID the string to parse
	 *
	 * @return the UUID if it could be parsed, otherwise null
	 */
	@Nullable
	private static UUID tryParseUUID(String inputUUID) {
		try {
			return UUID.fromString(inputUUID);
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	public enum RecordingStatus {
		NONE("none", Style.EMPTY),
		RECORDING("recording", FTBEssentials.RECORDING_STYLE),
		STREAMING("streaming", FTBEssentials.STREAMING_STYLE);

		public static final NameMap<RecordingStatus> NAME_MAP = NameMap.of(NONE, values()).create();

		private final String id;
		private final Style style;

		RecordingStatus(String id, Style style) {
			this.id = id;
			this.style = style;
		}

		public String getId() {
			return id;
		}

		public Style getStyle() {
			return style;
		}
	}
}
