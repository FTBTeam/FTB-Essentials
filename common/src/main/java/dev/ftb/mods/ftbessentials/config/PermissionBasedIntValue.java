package dev.ftb.mods.ftbessentials.config;

import dev.ftb.mods.ftblibrary.config.value.IntValue;
import dev.ftb.mods.ftblibrary.integration.permissions.PermissionHelper;
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
		return PermissionHelper.getProvider().getIntegerPermission(player, permission, value.get());
	}

}
