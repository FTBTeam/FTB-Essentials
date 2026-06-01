package dev.ftb.mods.ftbessentials.api;

import dev.ftb.mods.ftblibrary.util.TimeUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.LongSupplier;

@FunctionalInterface
public interface TeleportResult {
    int runCommand(ServerPlayer player);

    default boolean isSuccess() {
        return false;
    }

    static TeleportResult failed(Component msg) {
        return player -> {
            player.displayClientMessage(msg, false);
            return 0;
        };
    }

    TeleportResult DIMENSION_NOT_FOUND = failed(Component.translatable("ftbessentials.dimension_not_found"));
    TeleportResult UNKNOWN_DESTINATION = failed(Component.translatable("ftbessentials.unknown_dest"));
    TeleportResult PREVENTED = failed(Component.translatable("ftbessentials.teleport_prevented"));
    TeleportResult DIMENSION_NOT_ALLOWED_FROM = failed(Component.translatable("ftbessentials.teleport.not_from_here"));
    TeleportResult DIMENSION_NOT_ALLOWED_TO = failed(Component.translatable("ftbessentials.teleport.not_to_here"));
    TeleportResult SUCCESS = new TeleportResult() {
        @Override
        public int runCommand(ServerPlayer player) {
            return 1;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }
    };

    @FunctionalInterface
    interface OnCooldown extends TeleportResult {
        long getCooldown();

        @Override
        default int runCommand(ServerPlayer player) {
            String secStr = TimeUtils.prettyTimeString(getCooldown() / 1000L);
            player.displayClientMessage(Component.translatable("ftbessentials.teleport.on_cooldown", secStr), false);
            return 0;
        }

        static OnCooldown create(LongSupplier longSupplier) {
            return longSupplier::getAsLong;
        }
    }
}
