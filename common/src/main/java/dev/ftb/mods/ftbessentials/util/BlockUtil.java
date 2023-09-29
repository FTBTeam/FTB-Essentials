package dev.ftb.mods.ftbessentials.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class BlockUtil {
    public static Optional<BlockHitResult> getFocusedBlock(ServerPlayer player, double maxDist) {
        Vec3 entityVec = player.getEyePosition(1f);
        Vec3 maxDistVec = entityVec.add(player.getViewVector(1F).scale(maxDist));
        ClipContext ctx = new ClipContext(entityVec, maxDistVec, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        BlockHitResult hitResult = player.level().clip(ctx);
        return hitResult.getType() == HitResult.Type.BLOCK ? Optional.of(hitResult) : Optional.empty();
    }
}
