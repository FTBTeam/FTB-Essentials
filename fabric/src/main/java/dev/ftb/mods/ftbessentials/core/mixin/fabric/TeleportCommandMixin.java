package dev.ftb.mods.ftbessentials.core.mixin.fabric;

import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(TeleportCommand.class)
public class TeleportCommandMixin {
    @Inject(method = "performTeleport", at = @At("HEAD"))
    private static void onPerformTeleport(CommandSourceStack commandSourceStack, Entity entity, ServerLevel serverLevel, double d, double e, double f, Set<ClientboundPlayerPositionPacket.RelativeArgument> set, float g, float h, @Nullable TeleportCommand.LookAt lookAt, CallbackInfo ci) {
        if (entity instanceof ServerPlayer sp && !FTBEConfig.BACK_ON_DEATH_ONLY.get()) {
            FTBEPlayerData.addTeleportHistory(sp);
        }
    }
}
