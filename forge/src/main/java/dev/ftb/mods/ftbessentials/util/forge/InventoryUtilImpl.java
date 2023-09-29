package dev.ftb.mods.ftbessentials.util.forge;

import dev.ftb.mods.ftbessentials.util.InventoryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.List;

public class InventoryUtilImpl {
    public static NonNullList<ItemStack> getItemsInInventory(Level level, BlockPos pos, Direction side) {
        BlockEntity be = InventoryUtil.requireBlockEntity(level, pos);

        NonNullList<ItemStack> items = NonNullList.create();
        be.getCapability(ForgeCapabilities.ITEM_HANDLER, side).ifPresent(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    items.add(stack);
                }
            }
        });

        return items;
    }

    public static boolean putItemsInInventory(List<ItemStack> items, Level level, BlockPos pos, Direction side) {
        BlockEntity be = InventoryUtil.requireBlockEntity(level, pos);

        IItemHandler handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER, side)
                .orElseThrow(() -> new IllegalArgumentException("No item handler for block entity"));

        for (ItemStack stack : items) {
            ItemStack excess = ItemHandlerHelper.insertItem(handler, stack.copy(), false);
            if (!excess.isEmpty()) {
                return false;
            }
        }

        return true;
    }
}
