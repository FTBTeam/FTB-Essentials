package dev.ftb.mods.ftbessentials.commands.impl.kit;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class GiveMeKitCommand implements FTBCommand {
    @Override
    public boolean enabled() {
        return FTBEConfig.KIT.isEnabled();
    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> register() {
        return List.of(literal("give_me_kit")
                .then(argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> KitCommand.suggestKits(ctx.getSource().getPlayer(), builder))
                        .executes(ctx -> KitCommand.giveKit(ctx.getSource(), StringArgumentType.getString(ctx, "name"), List.of(ctx.getSource().getPlayerOrException())))
                ));
    }
}
