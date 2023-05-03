package dev.ftb.mods.ftbessentials.integration;

import dev.ftb.mods.ftbranks.PlayerNameFormatting;
import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class FTBRanksIntegration {
	public static int getInt(ServerPlayer player, int def, String node) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, node).asInteger().orElse(def), 0);
	}

	public static Component getDisplayName(Player player, String nickName) {
		return PlayerNameFormatting.formatPlayerName(player, Component.literal(nickName));
	}
}
