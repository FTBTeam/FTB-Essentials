package dev.ftb.mods.ftbessentials.config;

import dev.ftb.mods.ftbessentials.integration.PermissionsHelper;
import dev.ftb.mods.ftblibrary.snbt.config.BooleanValue;
import net.minecraft.server.level.ServerPlayer;

public class PermissionBasedBooleanValue {
	public final BooleanValue value;
	public final String permission;

	public PermissionBasedBooleanValue(BooleanValue value, String permission, String... comment) {
		this.value = value
				.comment(comment)
				.comment("You can override this with FTB Ranks using " + permission);
		this.permission = permission;
	}

	public boolean get(ServerPlayer player) {
		return PermissionsHelper.getInstance().getBool(player, value.get(), permission);
	}

}
