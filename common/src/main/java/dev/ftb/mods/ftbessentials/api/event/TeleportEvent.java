package dev.ftb.mods.ftbessentials.api.event;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TeleportEvent {
    public static Event<Teleport> TELEPORT = EventFactory.createCompoundEventResult();

    public interface Teleport {
        CompoundEventResult<Component> teleport(ServerPlayer player);
    }
}
