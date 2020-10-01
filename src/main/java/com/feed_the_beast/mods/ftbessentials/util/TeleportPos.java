package com.feed_the_beast.mods.ftbessentials.util;

import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

/**
 * @author LatvianModder
 */
public class TeleportPos
{
	@FunctionalInterface
	public interface TeleportResult
	{
		TeleportResult SUCCESS = new TeleportResult()
		{
			@Override
			public int runCommand(ServerPlayerEntity player)
			{
				return 1;
			}

			@Override
			public boolean isSuccess()
			{
				return true;
			}
		};

		TeleportResult DIMENSION_NOT_FOUND = player -> {
			player.sendStatusMessage(new StringTextComponent("Dimension not found!"), false);
			return 0;
		};

		int runCommand(ServerPlayerEntity player);

		default boolean isSuccess()
		{
			return false;
		}
	}

	@FunctionalInterface
	public interface CooldownTeleportResult extends TeleportResult
	{
		long getCooldown();

		@Override
		default int runCommand(ServerPlayerEntity player)
		{
			player.sendStatusMessage(new StringTextComponent("Can't teleport yet! Cooldown: " + CooldownTeleporter.prettyTimeString(getCooldown() / 1000L)), false);
			return 0;
		}
	}

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
		pos = new BlockPos(json.get("x").getAsInt(), json.get("y").getAsInt(), json.get("z").getAsInt());
		time = json.get("time").getAsLong();
	}

	public TeleportResult teleport(ServerPlayerEntity player)
	{
		ServerWorld world = player.server.getWorld(dimension);

		if (world == null)
		{
			return TeleportResult.DIMENSION_NOT_FOUND;
		}

		player.teleport(world, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, player.rotationYaw, player.rotationPitch);
		return TeleportResult.SUCCESS;
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

	public String distanceString(TeleportPos origin)
	{
		if (origin.dimension == dimension)
		{
			double dx = pos.getX() - origin.pos.getX();
			double dz = pos.getZ() - origin.pos.getZ();
			return (int) Math.sqrt(dx * dx + dz * dz) + "m";
		}
		else
		{
			ResourceLocation s = dimension.getLocation();

			if (s.getNamespace().equals("minecraft"))
			{
				switch (s.getPath())
				{
					case "overworld":
						return "Overworld";
					case "the_nether":
						return "The Nether";
					case "the_end":
						return "The End";
					default:
						return s.getPath();
				}
			}

			return s.getPath() + " [" + s.getNamespace() + "]";
		}
	}
}
