package dev.ftb.mods.ftbessentials.util;

import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.util.TimeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
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
			player.displayClientMessage(new TranslatableComponent("tip.ftbessentials.dimension_not_found"), false);
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
			player.displayClientMessage(new TranslatableComponent("tip.ftbessentials.can_not_teleport").append(new TextComponent(secStr)), false);
			return 0;
		}
	}

	public final ResourceKey<Level> dimension;
	public final BlockPos pos;
	public final Float yRot, xRot;
	public long time;

	public TeleportPos(ResourceKey<Level> d, BlockPos p) {
		this(d, p, null, null);
	}
	
	public TeleportPos(ResourceKey<Level> d, BlockPos p, Float yRot, Float xRot) {
		dimension = d;
		pos = p;
		this.yRot = yRot;
		this.xRot = xRot;
		time = System.currentTimeMillis();
	}

	public TeleportPos(Level world, BlockPos p, Float yRot, Float xRot) {
		this(world.dimension(), p, yRot, xRot);
	}

	public TeleportPos(Entity entity) {
		this(entity.level, entity.blockPosition(), entity.getYRot(), entity.getXRot());
	}

	public TeleportPos(CompoundTag tag) {
		dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(tag.getString("dim")));
		pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
		this.yRot = (tag.getTagType("yRot") == CompoundTag.TAG_FLOAT) ? tag.getFloat("yRot") : null;
		this.xRot = (tag.getTagType("xRot") == CompoundTag.TAG_FLOAT) ? tag.getFloat("xRot") : null;
		time = tag.getLong("time");
	}

	public TeleportResult teleport(ServerPlayer player) {
		ServerLevel world = player.server.getLevel(dimension);

		if (world == null) {
			return TeleportResult.DIMENSION_NOT_FOUND;
		}

		int lvl = player.experienceLevel;
		float xrot = (this.xRot == null) ? player.getXRot() : this.xRot;
		float yrot = (this.yRot == null) ? player.getYRot() : this.yRot;
		player.teleportTo(world, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, yrot, xrot);
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
		if (this.xRot != null) tag.putFloat("xRot", this.xRot);
		if (this.yRot != null) tag.putFloat("yRot", this.yRot);
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
