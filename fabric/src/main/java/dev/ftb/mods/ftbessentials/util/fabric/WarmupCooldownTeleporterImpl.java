package dev.ftb.mods.ftbessentials.util.fabric;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class WarmupCooldownTeleporterImpl {
    public static boolean firePlatformTeleportEvent(ServerPlayer player, Vec3 pos) {
        // TODO don't think there's a FAPI event for this; update here if one gets added
        return true;
    }
}
