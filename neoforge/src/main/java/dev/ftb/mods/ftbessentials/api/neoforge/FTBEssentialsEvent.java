package dev.ftb.mods.ftbessentials.api.neoforge;

import dev.ftb.mods.ftbessentials.api.event.RTPEvent;
import dev.ftb.mods.ftbessentials.api.event.TeleportEvent;
import dev.ftb.mods.ftblibrary.api.neoforge.BaseEventWithData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

public class FTBEssentialsEvent {
    public static class Teleport extends EntityTeleportEvent.TeleportCommand {
        public Teleport(Entity entity, ServerLevel targetLevel, double targetX, double targetY, double targetZ) {
            super(entity, targetLevel, targetX, targetY, targetZ);
        }

        public Teleport(TeleportEvent.Data data) {
            super(data.player(), data.targetLevel(), data.dest().x(), data.dest().y(), data.dest().z());
        }
    }

    public static class RTP extends BaseEventWithData<RTPEvent.Data> implements ICancellableEvent {
        public RTP(RTPEvent.Data data) {
            super(data);
        }
    }
}
