package dev.ftb.mods.ftbessentials.api.event;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TeleportEvent {
    public static Event<Teleport> TELEPORT = EventFactory.createCompoundEventResult();

    public interface Teleport {
        /**
         * Fired just before a player teleports (but before any warmup is started). This can be canceled to prevent
         * teleportation by returning {@link CompoundEventResult#interruptFalse(Object)} with a message to supply to the
         * player about why teleportation was prevented.
         *
         * @param player the player about to teleport
         * @return the event result
         */
        CompoundEventResult<Component> teleport(ServerPlayer player);
    }
}
