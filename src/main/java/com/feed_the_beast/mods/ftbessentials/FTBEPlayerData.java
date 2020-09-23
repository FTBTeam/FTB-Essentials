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
	public boolean fly;
	public boolean god;
	public String nick;

	public FTBEPlayerData(UUID u)
	{
		uuid = u;
		name = "Unknown";
		save = false;

		muted = false;
		fly = false;
		god = false;
		nick = "";
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
		return json;
	}

	public void fromJson(JsonObject json)
	{
		if (json.has("muted"))
		{
			muted = json.get("muted").getAsBoolean();
		}

		if (json.has("fly"))
		{
			fly = json.get("fly").getAsBoolean();
		}

		if (json.has("god"))
		{
			god = json.get("god").getAsBoolean();
		}

		if (json.has("nick"))
		{
			nick = json.get("nick").getAsString();
		}
	}
}