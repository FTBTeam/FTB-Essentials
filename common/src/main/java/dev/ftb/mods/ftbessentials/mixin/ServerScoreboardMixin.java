package dev.ftb.mods.ftbessentials.mixin;

import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerScoreboard.class)
public class ServerScoreboardMixin {
    @Final
    @Shadow
    private MinecraftServer server;

    @Inject(method = "onTeamChanged", at = @At("RETURN"))
    void ftb$onTeamChanged(PlayerTeam team, CallbackInfo ci) {
        FTBEPlayerData.sendPlayerTabsForScoreboardTeam(server, team);
    }

    @Inject(method = "addPlayerToTeam", at = @At(value = "RETURN", ordinal = 0))
    void ftb$addPlayerToTeam(String player, PlayerTeam team, CallbackInfoReturnable<Boolean> cir) {
        FTBEPlayerData.sendPlayerTabToAll(server, player);
    }

    @Inject(method = "removePlayerFromTeam", at = @At("RETURN"))
    void ftb$removePlayerFromTeam(String player, PlayerTeam team, CallbackInfo ci) {
        FTBEPlayerData.sendPlayerTabToAll(server, player);
    }
}
