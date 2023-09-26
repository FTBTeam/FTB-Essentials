package dev.ftb.mods.ftbessentials.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class InventoryUtil {
    @ExpectPlatform
    public static NonNullList<ItemStack> getItemsInInventory(Level level, BlockPos pos, Direction side) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean putItemsInInventory(List<ItemStack> items, Level level, BlockPos pos, Direction side) {
        throw new AssertionError();
    }

    @NotNull
    public static BlockEntity requireBlockEntity(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            throw new IllegalArgumentException(String.format("No block entity at %s / [%d,%d,%d]", level.dimension().location(), pos.getX(), pos.getY(), pos.getZ()));
        }
        return be;
    }
}
