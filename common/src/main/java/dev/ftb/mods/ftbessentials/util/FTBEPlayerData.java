package dev.ftb.mods.ftbessentials.util;

import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import de.marhali.json5.Json5Primitive;
import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.config.FTBEStartupConfig;
import dev.ftb.mods.ftbessentials.net.UpdateTabNameMessage;
import dev.ftb.mods.ftblibrary.json5.Json5Util;
import dev.ftb.mods.ftblibrary.platform.Platform;
import dev.ftb.mods.ftblibrary.platform.network.Server2PlayNetworking;
import dev.ftb.mods.ftblibrary.util.NameMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
	@Nullable
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
		lastSeenPos = null;
		recording = RecordingStatus.NONE;

		kitUseTimes = new HashMap<>();
		homes = new SavedTeleportManager.HomeManager(this);

		backTeleporter = new WarmupCooldownTeleporter(this, FTBEStartupConfig.BACK::getCooldown, FTBEStartupConfig.BACK::getWarmup, true);
		spawnTeleporter = new WarmupCooldownTeleporter(this, FTBEStartupConfig.SPAWN::getCooldown, FTBEStartupConfig.SPAWN::getWarmup);
		warpTeleporter = new WarmupCooldownTeleporter(this, FTBEStartupConfig.WARP::getCooldown, FTBEStartupConfig.WARP::getWarmup);
		homeTeleporter = new WarmupCooldownTeleporter(this, FTBEStartupConfig.HOME::getCooldown, FTBEStartupConfig.HOME::getWarmup);
		tpaTeleporter = new WarmupCooldownTeleporter(this, FTBEStartupConfig.TPA::getCooldown, FTBEStartupConfig.TPA::getWarmup);
		rtpTeleporter = new WarmupCooldownTeleporter(this, FTBEStartupConfig.RTP::getCooldown, FTBEStartupConfig.RTP::getWarmup);
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

	public Optional<TeleportPos> getLastSeenPos() {
		return Optional.ofNullable(lastSeenPos);
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

	public static Optional<FTBEPlayerData> getOrCreate(MinecraftServer server, UUID playerId) {
		if (MAP.containsKey(playerId)) {
			return Optional.of(MAP.get(playerId));
		}

		// Check if the player file exists
        return server.services().profileResolver().fetchById(playerId)
				.map(profile -> MAP.computeIfAbsent(playerId, k -> new FTBEPlayerData(playerId, profile.name())));
    }

	public static Optional<FTBEPlayerData> getOrCreate(@Nullable Player player) {
		if (player == null || Platform.get().misc().isFakePlayer(player)) {
			return Optional.empty();
		}

		return Optional.of(MAP.computeIfAbsent(player.getUUID(), k -> new FTBEPlayerData(player.getUUID(), player.getGameProfile().name())));
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

	public static void sendPlayerTabsForScoreboardTeam(MinecraftServer server, PlayerTeam team) {
		// when a scoreboard team is modified (specifically, color changed)
		// send updates for each player in the team to the server
		team.getPlayers().forEach(name -> sendPlayerTabToAll(server, name));
	}

	public static void sendPlayerTabToAll(MinecraftServer server, String playerName) {
		// when a player joins or leaves a scoreboard team
		// send updates for that player to the server
		var sp = server.getPlayerList().getPlayer(playerName);
		if (sp != null) {
			getOrCreate(sp).ifPresent(data -> data.sendTabName(server));
		}
	}

	public static void forEachPlayer(Consumer<FTBEPlayerData> consumer) {
		MAP.values().forEach(consumer);
	}

	public void markDirty() {
		needSave = true;
	}

	public Json5Object toJson() {
		Json5Object json = new Json5Object();
		json.addProperty("muted", muted);
		json.addProperty("fly", canFly);
		json.addProperty("god", god);
		json.addProperty("nick", nick);
		if (lastSeenPos != null) json.add("last_seen", lastSeenPos.toJson());
		json.addProperty("recording", recording.getId());

		Json5Array tph = new Json5Array();
		for (TeleportPos pos : teleportHistory) {
			tph.add(pos.toJson());
		}
		json.add("teleport_history", tph);

		json.add("homes", homes.toJson());

		json.add("kit_use_times", Util.make(new Json5Object(), tag -> kitUseTimes.forEach(tag::addProperty)));

		return json;
	}

	public void readJson(Json5Object json) {
		// TODO codec
		muted = Json5Util.getBoolean(json, "muted").orElse(false);
		canFly = Json5Util.getBoolean(json, "fly").orElse(false);
		god = Json5Util.getBoolean(json, "god").orElse(false);
		nick = Json5Util.getString(json, "nick").orElse("");
		recording = Json5Util.getString(json, "recording")
				.map(rec -> RecordingStatus.NAME_MAP.map.getOrDefault(rec, RecordingStatus.NONE))
				.orElse(RecordingStatus.NONE);
		lastSeenPos = Json5Util.getJson5Object(json, "last_seen").map(TeleportPos::fromJson).orElse(null);

		teleportHistory.clear();
		if (json.get("teleport_history") instanceof Json5Array th) {
			th.forEach(el -> teleportHistory.add(TeleportPos.fromJson(el)));
		}

		kitUseTimes.clear();
		if (json.get("kit_use_times") instanceof Json5Object j) {
			j.asMap().forEach((name, el) -> {
				if (el instanceof Json5Primitive p && p.isNumber()) {
					kitUseTimes.put(name, p.getAsLong());
				}
			});
		}

		Json5Util.getJson5Object(json, "homes").ifPresent(homes::readJson);
	}

	public void addTeleportHistory(ServerPlayer player, TeleportPos pos) {
		teleportHistory.add(pos);

		while (teleportHistory.size() > FTBEStartupConfig.MAX_BACK.get(player)) {
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
		Path path = FTBEWorldData.getInstance().mkdirs(PLAYER_DATA_PATH).resolve(uuid + ".json5");
		try {
			if (Files.exists(path)) {
				readJson(Json5Util.tryRead(path));
			} else {
				FTBEssentials.LOGGER.info("player data for {} doesn't exist yet, will create", uuid);
			}
		} catch (IOException e) {
			FTBEssentials.LOGGER.error("can't read {} : {} / {}", path, e.getClass().getName(), e.getMessage());
		}
	}

	public void saveIfChanged() {
		if (needSave) {
			Path path = FTBEWorldData.getInstance().mkdirs(PLAYER_DATA_PATH).resolve(uuid + ".json5");
			try {
				Json5Util.tryWrite(path, (Json5Element) toJson());
            } catch (IOException e) {
				FTBEssentials.LOGGER.error("can't write {} : {} / {}", path, e.getClass().getName(), e.getMessage());
            }
            needSave = false;
		}
	}

	public void sendTabName(MinecraftServer server) {
		Server2PlayNetworking.sendToAllPlayers(server, new UpdateTabNameMessage(uuid, name, nick, recording, false));
	}

	public void sendTabName(ServerPlayer to) {
		Server2PlayNetworking.send(to, new UpdateTabNameMessage(uuid, name, nick, recording, false));
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

	/// Get all the known players UUID's from the playerdata folder
	/// @return a list of all the known players UUID's
	public static List<UUID> getAllKnownPlayers() {
		try (Stream<Path> files = Files.list(FTBEWorldData.getInstance().mkdirs(PLAYER_DATA_PATH))) {
			return files.filter(path -> path.toString().endsWith(".json5"))
					.map(path -> tryParseUUID(path.getFileName().toString().replace(".json5", "")))
					.filter(Optional::isPresent)
					.map(Optional::get)
					.toList();
		} catch (Exception ex) {
            LOGGER.error("Failed to get all known players: {}", ex.getMessage());
		}

		return Collections.emptyList();
	}

	/// Attempt to parse a UUID from a string without fatal errors
	/// @param inputUUID the string to parse
	///
	/// @return the UUID if it could be parsed, otherwise null
	private static Optional<UUID> tryParseUUID(String inputUUID) {
		try {
			return Optional.of(UUID.fromString(inputUUID));
		} catch (IllegalArgumentException ex) {
			return Optional.empty();
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
