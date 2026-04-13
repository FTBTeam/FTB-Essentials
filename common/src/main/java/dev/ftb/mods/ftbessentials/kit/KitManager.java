package dev.ftb.mods.ftbessentials.kit;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import dev.ftb.mods.ftbessentials.commands.impl.kit.KitCommand;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.FTBEWorldData;
import dev.ftb.mods.ftbessentials.util.InventoryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
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

    public void load(Json5Element kits, HolderLookup.Provider provider) {
        allKits.clear();
        if (kits instanceof Json5Object o) {
            o.asMap().forEach((key, el) -> allKits.put(key, Kit.fromJson(el, provider)));
        }
    }

    public Json5Object save(HolderLookup.Provider provider) {
        return Util.make(new Json5Object(), tag -> allKits.forEach((name, kit) -> tag.add(name, kit.toJson(provider))));
    }

    public Optional<Kit> get(String kitName) {
        return Optional.ofNullable(allKits.get(kitName));
    }

    public Map<String, Kit> allKits() {
        return Collections.unmodifiableMap(allKits);
    }

    public void giveKitToPlayer(String kitName, ServerPlayer player) throws CommandSyntaxException {
        Kit kit = get(kitName).orElseThrow(() -> KitCommand.NO_SUCH_KIT.create(kitName));

        var playerData = FTBEPlayerData.getOrCreate(player);
        if (playerData.isPresent()) {
            kit.giveToPlayer(kitName, player, playerData.get(), true);
        }
    }

    public void deleteKit(String kitName) throws CommandSyntaxException {
        if (!allKits.containsKey(kitName)) {
            throw KitCommand.NO_SUCH_KIT.create(kitName);
        }

        allKits.remove(kitName);

        FTBEPlayerData.cleanupKitCooldowns(kitName);

        FTBEWorldData.getInstance().markDirty();
    }

    public void createFromPlayerInv(String kitName, ServerPlayer player, long cooldownSecs, boolean hotbarOnly) throws CommandSyntaxException {
        if (hotbarOnly) {
            NonNullList<ItemStack> items = NonNullList.create();
            for (int i = 0; i < 9; i++) {
                items.add(player.getInventory().getItem(i));
            }
            createKit(kitName, cooldownSecs, () -> items);
        } else {
            createKit(kitName, cooldownSecs, () -> player.getInventory().getNonEquipmentItems());
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

        addKit(kitName, new Kit(items, cooldownSecs, false), false);
    }

    public void addKit(String kitName, Kit kit, boolean overwrite) throws CommandSyntaxException {
        if (!overwrite && allKits.containsKey(kitName)) {
            throw KitCommand.ALREADY_EXISTS.create(kitName);
        }

        allKits.put(kitName, kit);

        FTBEWorldData.getInstance().markDirty();
    }
}
