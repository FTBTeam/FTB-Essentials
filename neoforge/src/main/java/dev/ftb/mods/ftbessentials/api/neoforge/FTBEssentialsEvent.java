package dev.ftb.mods.ftbessentials.api.neoforge;

import dev.ftb.mods.ftbessentials.api.event.TeleportEvent;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

public class FTBEssentialsEvent {
    public static class Teleport extends EntityTeleportEvent.TeleportCommand {
        public Teleport(Entity entity, double targetX, double targetY, double targetZ) {
            super(entity, targetX, targetY, targetZ);
        }

        public Teleport(TeleportEvent.Data data) {
            super(data.player(), data.dest().x(), data.dest().y(), data.dest().z());
        }
    }
}
