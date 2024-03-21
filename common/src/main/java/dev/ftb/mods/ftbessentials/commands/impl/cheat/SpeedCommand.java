package dev.ftb.mods.ftbessentials.commands.impl.cheat;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbessentials.commands.FTBCommand;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class SpeedCommand implements FTBCommand {
    private static final UUID ESSENTIALS_SPEED_UUID = UUID.fromString("3a8a9187-94ab-4272-99c0-ca764a19f8f1");

    @Override
    public boolean enabled() {
        return FTBEConfig.SPEED.isEnabled();
    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> register() {
        return List.of(literal("speed")
                .executes(context -> speed(context.getSource(), Attributes.MOVEMENT_SPEED, context.getSource().getPlayerOrException()))
                .then(argument("boost_percent", IntegerArgumentType.integer(-100, 2000))
                        .requires(cs -> cs.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> speed(context.getSource(), Attributes.MOVEMENT_SPEED, context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "boost_percent")))
                        .then(argument("player", EntityArgument.player())
                                .executes(context -> speed(context.getSource(), Attributes.MOVEMENT_SPEED, EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "boost_percent")))
                        )
                )
        );
    }

    private static int speed(CommandSourceStack source, Attribute attr, ServerPlayer player) {
        AttributeInstance attrInstance = player.getAttribute(attr);

        showSpeed(source, player, attrInstance);

        return 1;
    }


    private static int speed(CommandSourceStack source, Attribute attr, ServerPlayer target, int boostPct) {
        AttributeInstance attrInstance = target.getAttribute(attr);

        if (attrInstance != null) {
            float speedMult = boostPct / 100f;
            attrInstance.removeModifier(ESSENTIALS_SPEED_UUID);
            if (speedMult != 0f) {
                attrInstance.addPermanentModifier(new AttributeModifier(ESSENTIALS_SPEED_UUID,
                        "FTB Essentials speed boost", speedMult, AttributeModifier.Operation.MULTIPLY_BASE
                ));
            }
            showSpeed(source, target, attrInstance);
        }

        return 1;
    }

    private static void showSpeed(CommandSourceStack source, ServerPlayer target, AttributeInstance attrInstance) {
        Component msg;
        if (attrInstance != null && attrInstance.getModifier(ESSENTIALS_SPEED_UUID) != null) {
            double speedMult = attrInstance.getModifier(ESSENTIALS_SPEED_UUID).getAmount();
            int boostPct = (int) (speedMult * 100);
            msg = Component.literal("Speed boost for ")
                    .append(target.getDisplayName())
                    .append(" (").append(Component.translatable(attrInstance.getAttribute().getDescriptionId())).append(") = " + boostPct + "%");
        } else {
            msg = Component.literal("No speed boost for ").append(target.getDisplayName());
        }
        source.sendSuccess(() -> msg, false);
        if (!source.isPlayer() || source.getPlayer() != target) {
            target.displayClientMessage(msg, false);
        }
    }
}
