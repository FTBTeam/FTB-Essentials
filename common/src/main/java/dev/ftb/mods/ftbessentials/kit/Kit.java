package dev.ftb.mods.ftbessentials.kit;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.marhali.json5.Json5Element;
import dev.ftb.mods.ftbessentials.commands.impl.kit.KitCommand;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftblibrary.integration.permissions.PermissionHelper;
import dev.ftb.mods.ftblibrary.json5.Json5Ops;
import dev.ftb.mods.ftblibrary.util.TimeUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/// Represents one kit.
///
/// @param items the items in the kit
/// @param cooldown the cooldown between granting it to players
/// @param autoGrant if true, autogranted on player login
public record Kit(List<ItemStack> items, long cooldown, boolean autoGrant) {
    public static final Codec<Kit> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(Kit::items),
            Codec.LONG.fieldOf("cooldown").forGetter(Kit::cooldown),
            Codec.BOOL.optionalFieldOf("auto_grant", false).forGetter(Kit::autoGrant)
    ).apply(builder, Kit::new));

    public static Kit deepCopy(Collection<ItemStack> items, long cooldownSecs, boolean autoGrant) {
        return new Kit(items.stream().map(ItemStack::copy).toList(), cooldownSecs, autoGrant);
    }

    public static Kit fromJson(Json5Element tag, HolderLookup.Provider provider) {
        return CODEC.parse(provider.createSerializationContext(Json5Ops.INSTANCE), tag).getOrThrow();
    }

    public Json5Element toJson(HolderLookup.Provider provider) {
        return CODEC.encodeStart(provider.createSerializationContext(Json5Ops.INSTANCE), this).getOrThrow();
    }

    public void giveToPlayer(String kitName, ServerPlayer player, FTBEPlayerData playerData, boolean throwOnCooldown) throws CommandSyntaxException {
        long now = System.currentTimeMillis();

        if (!checkForCooldown(kitName, playerData, now, throwOnCooldown)) {
            items.forEach(stack -> {
                ItemStack stack1 = stack.copy();
                if (!player.getInventory().add(stack1)) {
                    ItemEntity itementity = player.drop(stack1, false);
                    if (itementity != null) {
                        itementity.setNoPickUpDelay();
                        itementity.setTarget(player.getUUID());
                    }
                }
            });

            if (cooldown != 0) {
                playerData.setLastKitUseTime(kitName, now);
            }
        }
    }

    private boolean checkForCooldown(String kitName, FTBEPlayerData data, long now, boolean throwOnCooldown) throws CommandSyntaxException {
        if (cooldown != 0) {
            long lastUsed = data.getLastKitUseTime(kitName);
            if (cooldown < 0L && lastUsed != 0L) {
                if (throwOnCooldown) {
                    throw KitCommand.ONE_TIME_ONLY.create(kitName, data.getName());
                } else {
                    return true;
                }
            }
            long delta = (now - lastUsed) / 1000L;
            if (delta < cooldown) {
                if (throwOnCooldown) {
                    throw KitCommand.ON_COOLDOWN.create(kitName, TimeUtils.prettyTimeString(cooldown - delta));
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public Kit withCooldown(long newCooldown) {
        return new Kit(items, newCooldown, autoGrant);
    }

    public Kit withAutoGrant(boolean newAutoGrant) {
        return new Kit(items, cooldown, newAutoGrant);
    }

    public static boolean playerCanGet(@Nullable ServerPlayer player, String kitName) {
        return player == null
                || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
                || checkPermissionNode(player, kitName);
    }

    public static boolean checkPermissionNode(ServerPlayer player, String kitName) {
        return PermissionHelper.getProvider().getBooleanPermission(player, "ftbessentials.give_me_kit." + kitName, false);
    }
}
