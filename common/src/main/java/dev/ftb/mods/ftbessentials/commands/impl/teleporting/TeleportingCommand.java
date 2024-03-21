package dev.ftb.mods.ftbessentials.commands.impl.teleporting;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public class TeleportingCommand implements FTBCommand {
    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> register() {
        return null;
    }
}
