package dev.ftb.mods.ftbessentials.api.event;

import dev.ftb.mods.ftblibrary.util.result.DataOutcome;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

// TODO: This isn't mapped to a real event yet!
// TODO: Why does this not have a pos?
@FunctionalInterface
public interface TeleportEvent {
    DataOutcome<Component> teleport(Data data);

    record Data(ServerPlayer player, Vec3 dest) {}
}
