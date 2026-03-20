package dev.ftb.mods.ftbessentials.api.event;

import dev.ftb.mods.ftblibrary.platform.event.TypedEvent;
import dev.ftb.mods.ftblibrary.util.result.DataOutcome;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@FunctionalInterface
public interface TeleportEvent {
    TypedEvent<Data,DataOutcome<Component>> TYPE = TypedEvent.of(TeleportEvent.Data.class);

    DataOutcome<Component> teleport(Data data);

    record Data(ServerPlayer player, Vec3 dest) {}
}
