package dev.ftb.mods.ftbessentials.util;

import dev.ftb.mods.ftblibrary.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class InventoryUtil {
    public static NonNullList<ItemStack> getItemsInInventory(Level level, BlockPos pos, Direction side) {
        return Platform.get().transfer().simple().blockEntity().getItems(level, pos, side);
    }

    public static boolean putItemsInInventory(List<ItemStack> items, Level level, BlockPos pos, Direction side) {
        return Platform.get().transfer().simple().blockEntity().putItems(items, level, pos, side);
    }
}
