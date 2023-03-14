package dev.ftb.mods.ftbessentials.config;

import dev.ftb.mods.ftbessentials.FTBEssentials;
import dev.ftb.mods.ftbessentials.FTBRanksIntegration;
import dev.ftb.mods.ftbessentials.LuckPermsIntegration;
import dev.ftb.mods.ftblibrary.snbt.config.IntValue;
import net.minecraft.server.level.ServerPlayer;

public class PermissionBasedIntValue {
	public final IntValue value;
	public final String permission;

	public PermissionBasedIntValue(IntValue value, String permission, String... comment) {
		this.value = value
				.comment(comment)
				.comment("You can override this with FTB Ranks using " + permission);
		this.permission = permission;
	}

	public int get(ServerPlayer player) {
		if (FTBEssentials.ranksMod) {
			return FTBRanksIntegration.getInt(player, value.get(), permission);
		} else if (FTBEssentials.luckpermsMod) {
			return LuckPermsIntegration.getInt(player, value.get(), permission);
		}

		return value.get();
	}

}
