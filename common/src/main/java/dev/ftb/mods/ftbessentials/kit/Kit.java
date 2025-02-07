package dev.ftb.mods.ftbessentials.kit;

import dev.ftb.mods.ftbessentials.integration.PermissionsHelper;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.util.TimeUtils;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Kit {
    private final String kitName;
    private final List<ItemStack> items;
    private final long cooldown; // seconds
    private final boolean autoGrant;

    public Kit(String kitName, Collection<ItemStack> items, long cooldown, boolean autoGrant) {
        this.kitName = kitName;
        this.items = List.copyOf(items);
        this.cooldown = cooldown;
        this.autoGrant = autoGrant;
    }

    public static Kit deepCopy(String kitName, Collection<ItemStack> items, long cooldownSecs, boolean autoGrant) {
        return new Kit(kitName, items.stream().map(ItemStack::copy).toList(), cooldownSecs, autoGrant);
    }

    public String getKitName() {
        return kitName;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public long getCooldown() {
        return cooldown;
    }

    public boolean isAutoGrant() {
        return autoGrant;
    }

    public CompoundTag toNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();

        ListTag list = new ListTag();
        items.forEach(stack -> list.add(saveStack(stack, provider)));

        tag.put("items", list);

        tag.putLong("cooldown", cooldown);
        if (autoGrant) tag.putBoolean("auto_grant", true);

        return tag;
    }

    private SNBTCompoundTag saveStack(ItemStack stack, HolderLookup.Provider provider) {
        var res = SNBTCompoundTag.of(stack.save(provider));
        res.singleLine();
        return res;
    }

    public static Kit fromNBT(String kitName, CompoundTag tag, HolderLookup.Provider provider) {
        List<ItemStack> items = new ArrayList<>();
        ListTag list = tag.getList("items", Tag.TAG_COMPOUND);
        list.forEach(el -> {
            if (el instanceof CompoundTag c) {
                ItemStack.parse(provider, c).ifPresent(items::add);
            }
        });
        return new Kit(kitName, items, tag.getLong("cooldown"), tag.getBoolean("auto_grant"));
    }

    public void giveToPlayer(ServerPlayer player, FTBEPlayerData playerData, boolean throwOnCooldown) {
        long now = System.currentTimeMillis();

        if (!checkForCooldown(player, playerData, now, throwOnCooldown)) {
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

    private boolean checkForCooldown(ServerPlayer player, FTBEPlayerData data, long now, boolean throwOnCooldown) {
        if (cooldown != 0) {
            long lastUsed = data.getLastKitUseTime(kitName);
            if (cooldown < 0L && lastUsed != 0L) {
                if (throwOnCooldown) {
                    throw new IllegalStateException("Kit " + kitName + " is a one-time use kit (already given to " + player.getGameProfile().getName() + ")");
                } else {
                    return true;
                }
            }
            long delta = (now - lastUsed) / 1000L;
            if (delta < cooldown) {
                if (throwOnCooldown) {
                    long remaining = cooldown - delta;
                    throw new IllegalStateException("Kit " + kitName + " is on cooldown - " + TimeUtils.prettyTimeString(remaining) + " remaining");
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public Kit withCooldown(long newCooldown) {
        return new Kit(kitName, items, newCooldown, autoGrant);
    }

    public Kit withAutoGrant(boolean newAutoGrant) {
        return new Kit(kitName, items, cooldown, newAutoGrant);
    }

    public boolean playerCanGetKit(@Nullable ServerPlayer player) {
        return player == null
                || player.hasPermissions(Commands.LEVEL_GAMEMASTERS)
                || checkPermissionNode(player, kitName);
    }

    public static boolean checkPermissionNode(@NotNull ServerPlayer player, String kitName) {
        return PermissionsHelper.getInstance().getBool(player, false, "ftbessentials.give_me_kit." + kitName);
    }
}
