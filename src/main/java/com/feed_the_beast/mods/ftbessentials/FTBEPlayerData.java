package com.feed_the_beast.mods.ftbessentials;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class FTBEPlayerData
{
	public static final Map<UUID, FTBEPlayerData> MAP = new HashMap<>();

	public static FTBEPlayerData get(UUID id)
	{
		return MAP.computeIfAbsent(id, FTBEPlayerData::new);
	}

	public static FTBEPlayerData get(GameProfile profile)
	{
		FTBEPlayerData data = MAP.get(profile.getId());

		if (data == null)
		{
			data = new FTBEPlayerData(profile.getId());
			data.name = profile.getName();
			MAP.put(profile.getId(), data);
		}

		return data;
	}

	public static FTBEPlayerData get(PlayerEntity player)
	{
		return get(player.getGameProfile());
	}

	public final UUID uuid;
	public String name;
	public boolean save;

	public boolean muted;

	public FTBEPlayerData(UUID u)
	{
		uuid = u;
		name = "Unknown";
		save = false;
	}

	public void save()
	{
		save = true;
	}

	public JsonObject toJson()
	{
		JsonObject json = new JsonObject();
		json.addProperty("muted", muted);
		return json;
	}

	public void fromJson(JsonObject json)
	{
		muted = json.get("muted").getAsBoolean();
	}
}