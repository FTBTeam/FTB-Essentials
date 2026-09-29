package dev.ftb.mods.ftbessentials.api;

import dev.ftb.mods.ftblibrary.util.TimeUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface TeleportResult {
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

    static TeleportResult failed(Component msg) {
        return player -> {
            player.sendSystemMessage(msg);
            return 0;
        };
    }

    TeleportResult DIMENSION_NOT_FOUND = failed(Component.translatable("ftbessentials.dimension_not_found"));

    TeleportResult UNKNOWN_DESTINATION = failed(Component.translatable("ftbessentials.unknown_dest"));

    TeleportResult DIMENSION_NOT_ALLOWED_FROM = failed(Component.translatable("ftbessentials.teleport.not_from_here"));

    TeleportResult DIMENSION_NOT_ALLOWED_TO = failed(Component.translatable("ftbessentials.teleport.not_to_here"));

    int runCommand(ServerPlayer player);

    default boolean isSuccess() {
        return false;
    }

    @FunctionalInterface
    interface OnCooldown extends TeleportResult {
        long getCooldown();

        @Override
        default int runCommand(ServerPlayer player) {
            String secStr = TimeUtils.prettyTimeString(getCooldown() / 1000L);
            player.sendSystemMessage(Component.translatable("ftbessentials.teleport.on_cooldown", secStr));
            return 0;
        }
    }
}
