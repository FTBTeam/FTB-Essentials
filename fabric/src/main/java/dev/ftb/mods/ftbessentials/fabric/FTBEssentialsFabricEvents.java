package dev.ftb.mods.ftbessentials.fabric;

import dev.ftb.mods.ftbessentials.api.event.TeleportEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;


public class FTBEssentialsFabricEvents {
    public static Event<TeleportEvent> TELEPORT = EventFactory.createArrayBacked(TeleportEvent.class,
            callbacks -> data -> {
                return null;
            }
//                    Arrays.stream(callbacks).anyMatch(event -> event.teleport(data))
    );

}
