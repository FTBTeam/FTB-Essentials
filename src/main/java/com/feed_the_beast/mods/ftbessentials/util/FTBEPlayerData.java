package com.feed_the_beast.mods.ftbessentials.util;

import com.feed_the_beast.mods.ftbessentials.FTBEConfig;
import com.feed_the_beast.mods.ftbessentials.FTBEssentials;
import com.feed_the_beast.mods.ftbessentials.net.FTBEssentialsNet;
import com.feed_the_beast.mods.ftbessentials.net.UpdateTabNamePacket;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class FTBEPlayerData
{
	public static final Map<UUID, FTBEPlayerData> MAP = new HashMap<>();

	public static FTBEPlayerData get(GameProfile profile)
	{
		FTBEPlayerData data = MAP.get(profile.getId());

		if (data == null)
		{
			data = new FTBEPlayerData(profile.getId());

			if (profile.getName() != null && !profile.getName().isEmpty())
			{
				data.name = profile.getName();
			}

			MAP.put(profile.getId(), data);
		}

		return data;
	}

	public static FTBEPlayerData get(PlayerEntity player)
	{
		return get(player.getGameProfile());
	}

	public static void addTeleportHistory(ServerPlayerEntity player, RegistryKey<World> dimension, BlockPos pos)
	{
		get(player).addTeleportHistory(player, new TeleportPos(dimension, pos));
	}

	public static void addTeleportHistory(ServerPlayerEntity player)
	{
		addTeleportHistory(player, player.world.getDimensionKey(), player.getPosition());
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

	public final CooldownTeleporter backTeleporter;
	public final CooldownTeleporter spawnTeleporter;
	public final CooldownTeleporter warpTeleporter;
	public final CooldownTeleporter homeTeleporter;
	public final CooldownTeleporter tpaTeleporter;
	public final CooldownTeleporter rtpTeleporter;
	public final LinkedList<TeleportPos> teleportHistory;

	public FTBEPlayerData(UUID u)
	{
		uuid = u;
		name = "Unknown";
		save = false;

		muted = false;
		fly = false;
		god = false;
		nick = "";
		lastSeen = new TeleportPos(World.OVERWORLD, BlockPos.ZERO);
		homes = new LinkedHashMap<>();
		recording = 0;

		backTeleporter = new CooldownTeleporter(this, FTBEConfig::getBackCooldown);
		spawnTeleporter = new CooldownTeleporter(this, FTBEConfig::getSpawnCooldown);
		warpTeleporter = new CooldownTeleporter(this, FTBEConfig::getWarpCooldown);
		homeTeleporter = new CooldownTeleporter(this, FTBEConfig::getHomeCooldown);
		tpaTeleporter = new CooldownTeleporter(this, FTBEConfig::getTpaCooldown);
		rtpTeleporter = new CooldownTeleporter(this, FTBEConfig::getRtpCooldown);
		teleportHistory = new LinkedList<>();
	}

	public void save()
	{
		save = true;
	}

	public JsonObject toJson()
	{
		JsonObject json = new JsonObject();
		json.addProperty("muted", muted);
		json.addProperty("fly", fly);
		json.addProperty("god", god);
		json.addProperty("nick", nick);
		json.add("lastSeen", lastSeen.toJson());
		json.addProperty("recording", recording);

		JsonArray tph = new JsonArray();

		for (TeleportPos pos : teleportHistory)
		{
			tph.add(pos.toJson());
		}

		json.add("teleportHistory", tph);

		JsonObject hm = new JsonObject();

		for (Map.Entry<String, TeleportPos> h : homes.entrySet())
		{
			hm.add(h.getKey(), h.getValue().toJson());
		}

		json.add("homes", hm);

		return json;
	}

	public void fromJson(JsonObject json)
	{
		muted = json.has("muted") && json.get("muted").getAsBoolean();
		fly = json.has("fly") && json.get("fly").getAsBoolean();
		god = json.has("god") && json.get("god").getAsBoolean();
		nick = json.has("nick") ? json.get("nick").getAsString() : "";
		recording = json.has("recording") ? json.get("recording").getAsInt() : 0;

		if (json.has("lastSeen"))
		{
			lastSeen = new TeleportPos(json.get("lastSeen").getAsJsonObject());
		}

		teleportHistory.clear();

		if (json.has("teleportHistory"))
		{
			for (JsonElement e : json.get("teleportHistory").getAsJsonArray())
			{
				teleportHistory.add(new TeleportPos(e.getAsJsonObject()));
			}
		}

		homes.clear();

		if (json.has("homes"))
		{
			for (Map.Entry<String, JsonElement> e : json.get("homes").getAsJsonObject().entrySet())
			{
				homes.put(e.getKey(), new TeleportPos(e.getValue().getAsJsonObject()));
			}
		}
	}

	public void addTeleportHistory(ServerPlayerEntity player, TeleportPos pos)
	{
		teleportHistory.add(pos);

		while (teleportHistory.size() > FTBEConfig.getMaxBack(player))
		{
			teleportHistory.removeFirst();
		}

		save();
	}

	public void load()
	{
		try
		{
			Path dir = FTBEWorldData.instance.mkdirs("playerdata");
			Path file = dir.resolve(uuid + ".json");

			if (Files.exists(file))
			{
				try (BufferedReader reader = Files.newBufferedReader(file))
				{
					fromJson(FTBEssentials.GSON.fromJson(reader, JsonObject.class));
				}
			}
		}
		catch (Exception ex)
		{
			FTBEssentials.LOGGER.error("Failed to load player data for " + uuid + ":" + name + ": " + ex);
			ex.printStackTrace();
		}
	}

	public void saveNow()
	{
		if (!save)
		{
			return;
		}

		try
		{
			JsonObject json = toJson();
			Path dir = FTBEWorldData.instance.mkdirs("playerdata");
			Path file = dir.resolve(uuid + ".json");

			try (BufferedWriter writer = Files.newBufferedWriter(file))
			{
				FTBEssentials.GSON.toJson(json, writer);
			}

			save = false;
		}
		catch (Exception ex)
		{
			FTBEssentials.LOGGER.error("Failed to save player data for " + uuid + ":" + name + ": " + ex);
			ex.printStackTrace();
		}
	}

	public void sendTabName()
	{
		sendTabName(null);
	}

	public void sendTabName(@Nullable ServerPlayerEntity to)
	{
		FTBEssentialsNet.MAIN.send(to == null ? PacketDistributor.ALL.noArg() : PacketDistributor.PLAYER.with(() -> to), new UpdateTabNamePacket(uuid, name, nick, recording, false));
	}
}