package dev.ftb.mods.ftbessentials.config;

import dev.ftb.mods.ftblibrary.config.value.BooleanValue;
import dev.ftb.mods.ftblibrary.config.value.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permissions;

import java.util.function.Predicate;

public class ToggleableConfig implements Predicate<CommandSourceStack> {
	public final String name;
	public final Config config;
	public final BooleanValue enabled;

	public ToggleableConfig(Config parent, String name) {
		this(parent, name, true);
	}

	public ToggleableConfig(Config parent, String name, boolean def) {
		this.name = name;
		config = parent.addGroup(name);
		enabled = config.addBoolean("enabled", def);
	}

	public boolean isEnabled() {
		return enabled.get();
	}

	public ToggleableConfig comment(String... comment) {
		config.comment(comment);
		return this;
	}

	@Override
	public boolean test(CommandSourceStack stack) {
		return isEnabled();
	}

	public Predicate<CommandSourceStack> enabledAndOp() {
		return stack -> test(stack) && stack.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}
}
