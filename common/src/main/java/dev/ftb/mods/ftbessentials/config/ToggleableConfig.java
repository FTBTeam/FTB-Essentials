package dev.ftb.mods.ftbessentials.config;

import dev.ftb.mods.ftblibrary.snbt.config.BooleanValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;
import net.minecraft.commands.CommandSourceStack;

import java.util.function.Predicate;

public class ToggleableConfig implements Predicate<CommandSourceStack> {
	public final String name;
	public final SNBTConfig config;
	public final BooleanValue enabled;

	public ToggleableConfig(SNBTConfig parent, String name) {
		this(parent, name, true);
	}

	public ToggleableConfig(SNBTConfig parent, String name, boolean def) {
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
		return stack -> test(stack) && stack.hasPermission(2);
	}
}
