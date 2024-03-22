package dev.ftb.mods.ftbessentials.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbessentials.config.ToggleableConfig;
import net.minecraft.commands.CommandSourceStack;

import java.util.Collections;
import java.util.List;

public record SimpleConfigurableCommand(
        ToggleableConfig config,
        LiteralArgumentBuilder<CommandSourceStack> builder
) implements FTBCommand {
    @Override
    public boolean enabled() {
        return config.isEnabled();
    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> register() {
        return Collections.singletonList(builder);
    }
}
