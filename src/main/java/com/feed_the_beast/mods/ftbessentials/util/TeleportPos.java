package com.feed_the_beast.mods.ftbessentials.util;

import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

/**
 * @author LatvianModder
 */
public class TeleportPos
{
	public final RegistryKey<World> dimension;
	public final BlockPos pos;
	public long time;

	public TeleportPos(RegistryKey<World> d, BlockPos p)
	{
		dimension = d;
		pos = p;
		time = System.currentTimeMillis();
	}

	public TeleportPos(World world, BlockPos p)
	{
		this(world.getDimensionKey(), p);
	}

	public TeleportPos(Entity entity)
	{
		this(entity.world, entity.getPosition());
	}

	public TeleportPos(JsonObject json)
	{
		dimension = RegistryKey.getOrCreateKey(Registry.WORLD_KEY, new ResourceLocation(json.get("dim").getAsString()));
		pos = new BlockPos(json.get("x").getAsInt(), json.get("t").getAsInt(), json.get("z").getAsInt());
		time = json.get("time").getAsLong();
	}

	public boolean teleport(ServerPlayerEntity player)
	{
		ServerWorld world = player.server.getWorld(dimension);

		if (world == null)
		{
			return false;
		}

		player.teleport(world, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, player.rotationYaw, player.rotationPitch);
		return true;
	}

	public JsonObject toJson()
	{
		JsonObject json = new JsonObject();
		json.addProperty("dim", dimension.getLocation().toString());
		json.addProperty("x", pos.getX());
		json.addProperty("y", pos.getY());
		json.addProperty("z", pos.getZ());
		json.addProperty("time", time);
		return json;
	}
}
