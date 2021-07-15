package dev.ftb.mods.ftbessentials.config;

import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;

public class TimedCommandConfig extends ToggleableConfig {
	public final PermissionBasedIntValue cooldown;
	public final String cooldownPermission;
	// TODO: Implement warmup configs too

	public TimedCommandConfig(SNBTConfig parent, String name, int defaultCooldown) {
		super(parent, name);
		cooldownPermission = String.format("ftbessentials.%s.cooldown", name);
		cooldown = new PermissionBasedIntValue(
				config.getInt("cooldown", defaultCooldown).range(0, 604800),
				cooldownPermission,
				String.format("Cooldown between /%s commands (in seconds)", name)
		);
	}

	public TimedCommandConfig(SNBTConfig parent, String name, int defaultCooldown, String... comment) {
		this(parent, name, defaultCooldown);
		config.comment(comment);
	}

	@Override
	public TimedCommandConfig comment(String... comment) {
		super.comment(comment);
		return this;
	}
}
