package dev.ftb.mods.ftbessentials.fabric;

import dev.ftb.mods.ftbessentials.api.TeleportDestination;
import dev.ftb.mods.ftbessentials.api.event.RTPEvent;
import dev.ftb.mods.ftbessentials.api.event.SavedTeleportEvent;
import dev.ftb.mods.ftbessentials.api.event.TeleportEvent;
import dev.ftb.mods.ftblibrary.util.result.DataOutcome;
import dev.ftb.mods.ftblibrary.util.result.Outcome;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.chat.Component;

public class FTBEssentialsEvents {
    public static Event<TeleportEvent> TELEPORT = EventFactory.createArrayBacked(TeleportEvent.class,
            callbacks -> data -> {
                for (var c : callbacks) {
                    if (c.teleport(data).isFail()) {
                        return DataOutcome.fail(Component.translatable("ftbessentials.teleport_prevented"));
                    }
                }
                return DataOutcome.pass();
            }
    );

    public static Event<RTPEvent> RTP = EventFactory.createArrayBacked(RTPEvent.class,
            callbacks -> data -> {
                for (var c : callbacks) {
                    if (c.teleport(data).isFail()) {
                        return Outcome.FAIL;
                    }
                }
                return Outcome.PASS;
            }
    );

    /// See [SavedTeleportEvent]
    public static final Event<SavedTeleportEvent> SAVED_TELEPORT = EventFactory.createArrayBacked(SavedTeleportEvent.class,
            callbacks -> data -> {
                for (SavedTeleportEvent c : callbacks) {
                    c.accept(data);
                }
            }
    );

    public static Event<SavedTeleportEvent.PreTeleport> SAVED_TELEPORT_PRE_TELEPORT = EventFactory.createArrayBacked(SavedTeleportEvent.PreTeleport.class,
            callbacks -> data -> {
                TeleportDestination.Outcome outcome = data.dest().success();
                for (var c : callbacks) {
                    outcome = c.preTeleport(data);
                    if (!outcome.success()) {
                        break;
                    }
                }
                return outcome;
            }
    );
}
