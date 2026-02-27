package dev.ftb.mods.ftbessentials.util.neoforge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;

public class InventoryUtilImpl {
    public static NonNullList<ItemStack> getItemsInInventory(Level level, BlockPos pos, Direction side) {
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        NonNullList<ItemStack> items = NonNullList.create();

        if (handler != null) {
            for (int i = 0; i < handler.size(); i++) {
                ItemStack stack = handler.getResource(i).toStack();
                if (!stack.isEmpty()) {
                    items.add(stack);
                }
            }
        }

        return items;
    }

    public static boolean putItemsInInventory(List<ItemStack> items, Level level, BlockPos pos, Direction side) {
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        if (handler == null) {
            throw new IllegalArgumentException("No item handler at that blockpos & side");
        }

        try (Transaction tx = Transaction.openRoot()) {
            for (ItemStack stack : items) {
                if (handler.insert(ItemResource.of(stack), stack.getCount(), tx) != stack.getCount()) {
                    return false;
                }
            }
            tx.commit();
            return true;
        }
    }
}
