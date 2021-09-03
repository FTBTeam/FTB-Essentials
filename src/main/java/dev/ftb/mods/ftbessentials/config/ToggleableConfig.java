package dev.ftb.mods.ftbessentials.config;

import dev.ftb.mods.ftblibrary.snbt.config.BooleanValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;

public class ToggleableConfig {
	public final String name;
	public final SNBTConfig config;
	public final BooleanValue enabled;

	public ToggleableConfig(SNBTConfig parent, String name) {
		this(parent, name, true);
	}

	public ToggleableConfig(SNBTConfig parent, String name, boolean def) {
		this.name = name;
		config = parent.getGroup(name);
		enabled = config.getBoolean("enabled", def);
	}

	public boolean isEnabled() {
		return enabled.get();
	}

	public ToggleableConfig comment(String... comment) {
		config.comment(comment);
		return this;
	}
}
