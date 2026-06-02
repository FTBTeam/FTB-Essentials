package dev.ftb.mods.ftbessentials.util;

import dev.ftb.mods.ftbessentials.api.TeleportDestination;
import dev.ftb.mods.ftbessentials.api.TeleportResult;
import dev.ftb.mods.ftbessentials.api.event.TeleportImmediateEvent;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class TeleportPos {
	private final ResourceKey<Level> dimension;
	private final BlockPos pos;
	@Nullable
	public final Float yRot, xRot;
	@Nullable
	private final UUID playerId;

	public TeleportPos(ResourceKey<Level> d, BlockPos p) {
		this(d, p, null, null);
	}

	public TeleportPos(Level world, BlockPos p, @Nullable Float yRot, @Nullable Float xRot) {
		this(world.dimension(), p, yRot, xRot);
	}

	public TeleportPos(Entity entity) {
		this(entity.level().dimension(), entity.blockPosition(), entity.getYRot(), entity.getXRot(),
				entity instanceof Player p ? p.getUUID() : null);
	}

	public TeleportPos(CompoundTag tag, @Nullable UUID playerId) {
		dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(tag.getString("dim")));
		pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
		this.yRot = (tag.getTagType("yRot") == CompoundTag.TAG_FLOAT) ? tag.getFloat("yRot") : null;
		this.xRot = (tag.getTagType("xRot") == CompoundTag.TAG_FLOAT) ? tag.getFloat("xRot") : null;
		this.playerId = playerId;
	}

	private TeleportPos(ResourceKey<Level> d, BlockPos p, @Nullable Float yRot, @Nullable Float xRot) {
		this(d, p, yRot, xRot, null);
	}

	// canonical ctor
	private TeleportPos(ResourceKey<Level> dimension, BlockPos pos, @Nullable Float yRot, @Nullable Float xRot, @Nullable UUID playerId) {
		this.dimension = dimension;
		this.pos = pos;
		this.yRot = yRot;
		this.xRot = xRot;
		this.playerId = playerId;
	}

	@Nullable
	public static TeleportPos fromDestination(@Nullable TeleportDestination dest) {
        return dest == null ?
				null :
				new TeleportPos(dest.dimension(), dest.pos(),
						dest.yRot().orElse(null), dest.xRot().orElse(null),
						dest.playerId()
				);
    }

	TeleportDestination asDestination() {
		return new TeleportDestination(dimension, pos, Optional.ofNullable(yRot), Optional.ofNullable(xRot), playerId);
	}

	public TeleportPos safeForPlayer(ServerPlayer player) {
		assert player.getServer() != null;
		ServerLevel level = player.getServer().getLevel(dimension);
		if (level == null) return this;  // shouldn't happen

		return tryFindSafePos(level, Direction.NORTH, Direction.WEST)
				.or(() -> tryFindSafePos(level, Direction.SOUTH, Direction.EAST))
				.orElse(this);
	}

	private Optional<TeleportPos> tryFindSafePos(ServerLevel level, Direction dir1, Direction dir2) {
		for (BlockPos p0 : BlockPos.spiralAround(pos, 16, dir1, dir2)) {
			for (int yOff = 0; yOff <= 3; yOff++) {
				TeleportPos res = checkForPos(level, p0, yOff);
				if (res != null) return Optional.of(res);
			}
			for (int yOff = -1; yOff >= -3; yOff--) {
				TeleportPos res = checkForPos(level, p0, yOff);
				if (res != null) return Optional.of(res);
			}
		}
		return Optional.empty();
	}

	@Nullable
	private TeleportPos checkForPos(ServerLevel level, BlockPos basePos, int yOff) {
		BlockPos p1 = basePos.relative(Direction.Axis.Y, yOff);
		BlockPos p2 = p1.above();
        return !level.getBlockState(p1).isSuffocating(level, p1) && !level.getBlockState(p2).isSuffocating(level, p2) ?
				new TeleportPos(dimension, p1.immutable(), yRot, xRot) :
				null;
    }

	public TeleportResult checkDimensionBlacklist(Player player) {
		if (!DimensionFilter.isDimensionOKTo(this.dimension)) {
			return TeleportResult.DIMENSION_NOT_ALLOWED_TO;
		} else if(!DimensionFilter.isDimensionOKFrom(player.level().dimension())) {
			return TeleportResult.DIMENSION_NOT_ALLOWED_FROM;
		}
		return TeleportResult.SUCCESS;
	}

	public TeleportResult teleport(ServerPlayer player) {
		TeleportDestination oldDest = asDestination();
		var outcome = TeleportImmediateEvent.TELEPORT.invoker().teleport(player, oldDest);
		if (outcome.isFalse()) {
			return TeleportResult.failed(outcome.object().reason());
		}

		var newDest = outcome.isEmpty() ? oldDest : outcome.object().dest();
		TeleportPos newPos = newDest.equals(oldDest) ? this : TeleportPos.fromDestination(newDest);
		return newPos.actuallyTeleport(player);
	}

	private TeleportResult actuallyTeleport(ServerPlayer player) {
		ServerLevel level = player.server.getLevel(dimension);
		if (level == null) {
			return TeleportResult.DIMENSION_NOT_FOUND;
		}

		int xpLevel = player.experienceLevel;
		float xrot = (this.xRot == null) ? player.getXRot() : this.xRot;
		float yrot = (this.yRot == null) ? player.getYRot() : this.yRot;
		player.teleportTo(level, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, yrot, xrot);
		player.setExperienceLevels(xpLevel);
		return TeleportResult.SUCCESS;
	}

	public CompoundTag write() {
		SNBTCompoundTag tag = new SNBTCompoundTag();
		tag.singleLine();
		tag.putString("dim", dimension.location().toString());
		tag.putInt("x", pos.getX());
		tag.putInt("y", pos.getY());
		tag.putInt("z", pos.getZ());
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

	public BlockPos getPos() {
		return pos;
	}

	public String posAsString() {
		// Normal shortString would be 1, 2, 3 so we remove the commas
		return pos.toShortString().replace(",", "");
	}
}
