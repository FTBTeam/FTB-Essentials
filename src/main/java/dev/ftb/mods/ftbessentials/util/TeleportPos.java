package dev.ftb.mods.ftbessentials.util;

import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.util.TimeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * @author LatvianModder
 */
public class TeleportPos {
	@FunctionalInterface
	public interface TeleportResult {
		TeleportResult SUCCESS = new TeleportResult() {
			@Override
			public int runCommand(ServerPlayer player) {
				return 1;
			}

			@Override
			public boolean isSuccess() {
				return true;
			}
		};

		TeleportResult DIMENSION_NOT_FOUND = player -> {
			player.displayClientMessage(Component.literal("Dimension not found!"), false);
			return 0;
		};

		int runCommand(ServerPlayer player);

		default boolean isSuccess() {
			return false;
		}
	}

	@FunctionalInterface
	public interface CooldownTeleportResult extends TeleportResult {
		long getCooldown();

		@Override
		default int runCommand(ServerPlayer player) {
			String secStr = TimeUtils.prettyTimeString(getCooldown() / 1000L);
			player.displayClientMessage(Component.literal("Can't teleport yet! Cooldown: " + secStr), false);
			return 0;
		}
	}

	public final ResourceKey<Level> dimension;
	public final BlockPos pos;
	public long time;

	public TeleportPos(ResourceKey<Level> d, BlockPos p) {
		dimension = d;
		pos = p;
		time = System.currentTimeMillis();
	}

	public TeleportPos(Level world, BlockPos p) {
		this(world.dimension(), p);
	}

	public TeleportPos(Entity entity) {
		this(entity.level, entity.blockPosition());
	}

	public TeleportPos(CompoundTag tag) {
		dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(tag.getString("dim")));
		pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
		time = tag.getLong("time");
	}

	public TeleportResult teleport(ServerPlayer player) {
		ServerLevel world = player.server.getLevel(dimension);

		if (world == null) {
			return TeleportResult.DIMENSION_NOT_FOUND;
		}

		int lvl = player.experienceLevel;
		player.teleportTo(world, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, player.getYRot(), player.getXRot());
		player.setExperienceLevels(lvl);
		return TeleportResult.SUCCESS;
	}

	public SNBTCompoundTag write() {
		SNBTCompoundTag tag = new SNBTCompoundTag();
		tag.singleLine();
		tag.putString("dim", dimension.location().toString());
		tag.putInt("x", pos.getX());
		tag.putInt("y", pos.getY());
		tag.putInt("z", pos.getZ());
		tag.putLong("time", time);
		return tag;
	}

	public String distanceString(TeleportPos origin) {
		if (origin.dimension == dimension) {
			double dx = pos.getX() - origin.pos.getX();
			double dz = pos.getZ() - origin.pos.getZ();
			return (int) Math.sqrt(dx * dx + dz * dz) + "m";
		} else {
			ResourceLocation s = dimension.location();

			if (s.getNamespace().equals("minecraft")) {
				return switch (s.getPath()) {
					case "overworld" -> "Overworld";
					case "the_nether" -> "The Nether";
					case "the_end" -> "The End";
					default -> s.getPath();
				};
			}

			return s.getPath() + " [" + s.getNamespace() + "]";
		}
	}
}
