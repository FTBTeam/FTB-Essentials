package dev.ftb.mods.ftbessentials.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ftb.mods.ftblibrary.util.TimeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.Set;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class TeleportPos {
	public static final Codec<TeleportPos> CODEC = RecordCodecBuilder.create(builder -> builder.group(
			ResourceKey.codec(Registries.DIMENSION).fieldOf("dim").forGetter(p -> p.dimensionId),
			BlockPos.CODEC.fieldOf("pos").forGetter(TeleportPos::getPos),
			Codec.FLOAT.optionalFieldOf("yRot").forGetter(p -> p.yRot),
			Codec.FLOAT.optionalFieldOf("xRot").forGetter(p -> p.xRot),
			Codec.LONG.fieldOf("time").forGetter(p -> p.time)
	).apply(builder, TeleportPos::new));

	private final ResourceKey<Level> dimensionId;
	private final BlockPos pos;
	public final Optional<Float> yRot;
	public final Optional<Float> xRot;
	private final long time;

    private TeleportPos(ResourceKey<Level> dimensionId, BlockPos pos, Optional<Float> yRot, Optional<Float> xRot, long time) {
        this.dimensionId = dimensionId;
        this.pos = pos;
        this.yRot = yRot;
        this.xRot = xRot;
        this.time = time;
    }

	public TeleportPos(ResourceKey<Level> dimensionId, BlockPos pos) {
		this(dimensionId, pos, null, null);
	}

	public TeleportPos(Level world, BlockPos p, Float yRot, Float xRot) {
		this(world.dimension(), p, yRot, xRot);
	}

	public TeleportPos(ResourceKey<Level> dimensionId, BlockPos pos, Float yRot, Float xRot) {
		this.dimensionId = dimensionId;
		this.pos = pos;
		this.yRot = Optional.ofNullable(yRot);
		this.xRot = Optional.ofNullable(xRot);
		this.time = System.currentTimeMillis();
	}

	public TeleportPos(Entity entity) {
		this(entity.level(), entity.blockPosition(), entity.getYRot(), entity.getXRot());
	}

	public static TeleportPos fromNBT(CompoundTag tag) {
		return CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
	}

	public Tag toNBT() {
		return CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
	}

	public TeleportPos safeForPlayer(ServerPlayer player) {
		ServerLevel level = player.level().getServer().getLevel(dimensionId);
		if (level == null) return this;  // shouldn't happen

		return tryFindSafePos(level, Direction.NORTH, Direction.WEST)
				.or(() -> tryFindSafePos(level, Direction.SOUTH, Direction.EAST))
				.orElse(this);
	}

	private Optional<TeleportPos> tryFindSafePos(ServerLevel level, Direction dir1, Direction dir2) {
		for (BlockPos p0 : BlockPos.spiralAround(pos, 16, dir1, dir2)) {
			for (int yOff = -3; yOff <= 3; yOff++) {
				BlockPos p1 = p0.relative(Direction.Axis.Y, yOff);
				BlockPos p2 = p1.above();
				if (!level.getBlockState(p1).isSuffocating(level, p1) && !level.getBlockState(p2).isSuffocating(level, p2)) {
					return Optional.of(new TeleportPos(dimensionId, p1.immutable(), yRot, xRot, time));
				}
			}
		}
		return Optional.empty();
	}

	public TeleportResult checkDimensionBlacklist(Player player) {
		if (!DimensionFilter.isDimensionOKTo(dimensionId)) {
			return TeleportResult.DIMENSION_NOT_ALLOWED_TO;
		} else if(!DimensionFilter.isDimensionOKFrom(player.level().dimension())) {
			return TeleportResult.DIMENSION_NOT_ALLOWED_FROM;
		}
		return TeleportResult.SUCCESS;
	}

	public TeleportResult teleport(ServerPlayer player) {
		ServerLevel level = player.level().getServer().getLevel(dimensionId);
		if (level == null) {
			return TeleportResult.DIMENSION_NOT_FOUND;
		}

		int xpLevel = player.experienceLevel;
		float xrot = xRot.orElse(player.getXRot());
		float yrot = yRot.orElse(player.getYRot());
		player.teleportTo(level, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, Set.of(), yrot, xrot, false);
		player.setExperienceLevels(xpLevel);
		return TeleportResult.SUCCESS;
	}

	public String distanceString(TeleportPos origin) {
		if (origin.dimensionId == dimensionId) {
			double dx = pos.getX() - origin.pos.getX();
			double dz = pos.getZ() - origin.pos.getZ();
			return (int) Math.sqrt(dx * dx + dz * dz) + "m";
		} else {
			Identifier s = dimensionId.identifier();

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
		return pos.toShortString().replaceAll(",", "");
	}

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

		static TeleportResult failed(Component msg) {
			return player -> {
				player.displayClientMessage(msg, false);
				return 0;
			};
		}

		TeleportResult DIMENSION_NOT_FOUND = failed(Component.translatable("ftbessentials.dimension_not_found"));

		TeleportResult UNKNOWN_DESTINATION = failed(Component.translatable("ftbessentials.unknown_dest"));

		TeleportResult DIMENSION_NOT_ALLOWED_FROM = failed(Component.translatable("ftbessentials.teleport.not_from_here"));

		TeleportResult DIMENSION_NOT_ALLOWED_TO = failed(Component.translatable("ftbessentials.teleport.not_to_here"));

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
			player.displayClientMessage(Component.translatable("ftbessentials.teleport.on_cooldown", secStr), false);
			return 0;
		}
	}
}
