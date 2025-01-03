package dev.ftb.mods.ftbessentials.util.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

public class WarmupCooldownTeleporterImpl {
    public static boolean firePlatformTeleportEvent(ServerPlayer player, Vec3 pos) {
        EntityTeleportEvent.TeleportCommand event = new EntityTeleportEvent.TeleportCommand(player, pos.x, pos.y, pos.z);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }
}
