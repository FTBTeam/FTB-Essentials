package dev.ftb.mods.ftbessentials.util;

import com.mojang.authlib.GameProfile;
import dev.architectury.hooks.level.entity.PlayerHooks;
import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.net.UpdateTabNameMessage;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author LatvianModder
 */
public class FTBEPlayerData {
	public static final Map<UUID, FTBEPlayerData> MAP = new HashMap<>();

	@Nullable
	public static FTBEPlayerData get(@Nullable GameProfile profile) {
		if (profile == null || profile.getId() == null || profile.getName() == null) {
			return null;
		}

		FTBEPlayerData data = MAP.get(profile.getId());

		if (data == null) {
			data = new FTBEPlayerData(profile.getId());

			if (profile.getName() != null && !profile.getName().isEmpty()) {
				data.name = profile.getName();
			}

			MAP.put(profile.getId(), data);
		}

		return data;
	}

	@Nullable
	public static FTBEPlayerData get(Player player) {
		return PlayerHooks.isFake(player) ? null : get(player.getGameProfile());
	}

	public static void addTeleportHistory(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos) {
		FTBEPlayerData data = get(player);

		if (data != null) {
			data.addTeleportHistory(player, new TeleportPos(dimension, pos));
		}
	}

	public static void addTeleportHistory(ServerPlayer player) {
		addTeleportHistory(player, player.level.dimension(), player.blockPosition());
	}

	public final UUID uuid;
	public String name;
	private boolean save;

	public boolean muted;
	public boolean fly;
	public boolean god;
	public String nick;
	public TeleportPos lastSeen;
	public final LinkedHashMap<String, TeleportPos> homes;
	public int recording;

	public final WarmupCooldownTeleporter backTeleporter;
	public final WarmupCooldownTeleporter spawnTeleporter;
	public final WarmupCooldownTeleporter warpTeleporter;
	public final WarmupCooldownTeleporter homeTeleporter;
	public final WarmupCooldownTeleporter tpaTeleporter;
	public final WarmupCooldownTeleporter rtpTeleporter;
	public final LinkedList<TeleportPos> teleportHistory;

	public FTBEPlayerData(UUID u) {
		uuid = u;
		name = "Unknown";
		save = false;

		muted = false;
		fly = false;
		god = false;
		nick = "";
		lastSeen = new TeleportPos(Level.OVERWORLD, BlockPos.ZERO);
		homes = new LinkedHashMap<>();
		recording = 0;

		backTeleporter = new WarmupCooldownTeleporter(this, FTBEConfig.BACK::getCooldown, FTBEConfig.BACK::getWarmup, true);
		spawnTeleporter = new WarmupCooldownTeleporter(this, FTBEConfig.SPAWN::getCooldown, FTBEConfig.SPAWN::getWarmup);
		warpTeleporter = new WarmupCooldownTeleporter(this, FTBEConfig.WARP::getCooldown, FTBEConfig.WARP::getWarmup);
		homeTeleporter = new WarmupCooldownTeleporter(this, FTBEConfig.HOME::getCooldown, FTBEConfig.HOME::getWarmup);
		tpaTeleporter = new WarmupCooldownTeleporter(this, FTBEConfig.TPA::getCooldown, FTBEConfig.TPA::getWarmup);
		rtpTeleporter = new WarmupCooldownTeleporter(this, FTBEConfig.RTP::getCooldown, FTBEConfig.RTP::getWarmup);
		teleportHistory = new LinkedList<>();
	}

	public void save() {
		save = true;
	}

	public SNBTCompoundTag write() {
		SNBTCompoundTag json = new SNBTCompoundTag();
		json.putBoolean("muted", muted);
		json.putBoolean("fly", fly);
		json.putBoolean("god", god);
		json.putString("nick", nick);
		json.put("last_seen", lastSeen.write());
		json.putInt("recording", recording);

		ListTag tph = new ListTag();

		for (TeleportPos pos : teleportHistory) {
			tph.add(pos.write());
		}

		json.put("teleport_history", tph);

		SNBTCompoundTag hm = new SNBTCompoundTag();

		for (Map.Entry<String, TeleportPos> h : homes.entrySet()) {
			hm.put(h.getKey(), h.getValue().write());
		}

		json.put("homes", hm);

		return json;
	}

	public void read(CompoundTag tag) {
		muted = tag.getBoolean("muted");
		fly = tag.getBoolean("fly");
		god = tag.getBoolean("god");
		nick = tag.getString("nick");
		recording = tag.getInt("recording");
		lastSeen = tag.contains("last_seen") ? new TeleportPos(tag.getCompound("last_seen")) : null;

		teleportHistory.clear();

		ListTag th = tag.getList("teleport_history", Tag.TAG_COMPOUND);

		for (int i = 0; i < th.size(); i++) {
			teleportHistory.add(new TeleportPos(th.getCompound(i)));
		}

		homes.clear();

		CompoundTag h = tag.getCompound("homes");

		for (String key : h.getAllKeys()) {
			homes.put(key, new TeleportPos(h.getCompound(key)));
		}
	}

	public void addTeleportHistory(ServerPlayer player, TeleportPos pos) {
		teleportHistory.add(pos);

		while (teleportHistory.size() > FTBEConfig.MAX_BACK.get(player)) {
			teleportHistory.removeFirst();
		}

		save();
	}

	public void popTeleportHistory() {
		if (!teleportHistory.isEmpty()) {
			teleportHistory.removeLast();
		} else {
			FTBEssentials.LOGGER.warn("attempted to pop empty back history for {}", uuid);
		}
	}

	public void load() {
		CompoundTag tag = SNBT.read(FTBEWorldData.instance.mkdirs("playerdata").resolve(uuid + ".snbt"));

		if (tag != null) {
			read(tag);
		}
	}

	public void saveNow() {
		if (save && SNBT.write(FTBEWorldData.instance.mkdirs("playerdata").resolve(uuid + ".snbt"), write())) {
			save = false;
		}
	}

	public void sendTabName(MinecraftServer server) {
		new UpdateTabNameMessage(uuid, name, nick, recording, false).sendToAll(server);
	}

	public void sendTabName(ServerPlayer to) {
		new UpdateTabNameMessage(uuid, name, nick, recording, false).sendTo(to);
	}
}