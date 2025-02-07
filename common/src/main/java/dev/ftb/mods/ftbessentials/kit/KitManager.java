package dev.ftb.mods.ftbessentials.kit;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbessentials.commands.impl.kit.KitCommand;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.FTBEWorldData;
import dev.ftb.mods.ftbessentials.util.InventoryUtil;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.function.Supplier;

public enum KitManager {
    INSTANCE;

    private final Map<String, Kit> allKits = new HashMap<>();

    public static KitManager getInstance() {
        return INSTANCE;
    }

    public void load(CompoundTag kits, HolderLookup.Provider provider) {
        allKits.clear();
        kits.getAllKeys().forEach(key -> allKits.put(key, Kit.fromNBT(key, kits.getCompound(key), provider)));
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        return Util.make(new CompoundTag(), tag -> allKits.forEach((name, kit) -> tag.put(name, kit.toNBT(provider))));
    }

    public Optional<Kit> get(String kitName) {
        return Optional.ofNullable(allKits.get(kitName));
    }

    public Collection<Kit> allKits() {
        return Collections.unmodifiableCollection(allKits.values());
    }

    public void giveKitToPlayer(String kitName, ServerPlayer player) throws CommandSyntaxException {
        Kit kit = get(kitName).orElseThrow(() -> KitCommand.NO_SUCH_KIT.create(kitName));
        var playerData = FTBEPlayerData.getOrCreate(player).orElse(null);
        if (playerData != null) {
            kit.giveToPlayer(player, playerData, true);
        }
    }

    public void deleteKit(String kitName) throws CommandSyntaxException {
        if (!allKits.containsKey(kitName)) {
            throw KitCommand.NO_SUCH_KIT.create(kitName);
        }

        allKits.remove(kitName);

        FTBEPlayerData.cleanupKitCooldowns(kitName);

        FTBEWorldData.instance.markDirty();
    }

    public void createFromPlayerInv(String kitName, ServerPlayer player, long cooldownSecs, boolean hotbarOnly) throws CommandSyntaxException {
        if (hotbarOnly) {
            NonNullList<ItemStack> items = NonNullList.create();
            for (int i = 0; i < 9; i++) {
                items.add(player.getInventory().items.get(i));
            }
            createKit(kitName, cooldownSecs, () -> items);
        } else {
            createKit(kitName, cooldownSecs, () -> player.getInventory().items);
        }
    }

    public void createFromBlockInv(String kitName, Level level, BlockPos pos, Direction side, long cooldownSecs) throws CommandSyntaxException {
        createKit(kitName, cooldownSecs, () -> InventoryUtil.getItemsInInventory(level, pos, side));
    }

    private void createKit(String kitName, long cooldownSecs, Supplier<NonNullList<ItemStack>> itemSupplier) throws CommandSyntaxException {
        List<ItemStack> items = itemSupplier.get().stream()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
        if (items.isEmpty()) {
            throw KitCommand.NO_ITEMS_TO_ADD.create();
        }

        addKit(new Kit(kitName, items, cooldownSecs, false), false);
    }

    public void addKit(Kit kit, boolean overwrite) throws CommandSyntaxException {
        if (!overwrite && allKits.containsKey(kit.getKitName())) {
            throw KitCommand.ALREADY_EXISTS.create(kit.getKitName());
        }

        allKits.put(kit.getKitName(), kit);

        FTBEWorldData.instance.markDirty();
    }
}
