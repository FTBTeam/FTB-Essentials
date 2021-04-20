package dev.ftb.mods.ftbessentials.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * @author LatvianModder
 */
public class OtherPlayerInventory implements IInventory {
	public final ServerPlayerEntity player;

	public OtherPlayerInventory(ServerPlayerEntity p) {
		player = p;
	}

	@Override
	public int getSizeInventory() {
		return 45;
	}

	@Override
	public boolean isEmpty() {
		return player.inventory.isEmpty();
	}

	public boolean isInvalidSlot(int index) {
		return index >= 4 && index < 8;
	}

	public int getSlot(int index) {
		if (index == 8) {
			return 40;
		} else if (index >= 0 && index <= 3) {
			return 39 - index;
		} else if (index >= 9 && index <= 35) {
			return index;
		} else if (index >= 36 && index <= 44) {
			return index - 36;
		}

		return -1;
	}

	@Override
	public ItemStack getStackInSlot(int index) {
		if (isInvalidSlot(index)) {
			return ItemStack.EMPTY;
		}

		int slot = getSlot(index);
		return slot == -1 ? ItemStack.EMPTY : player.inventory.getStackInSlot(slot);
	}

	@Override
	public ItemStack decrStackSize(int index, int count) {
		if (isInvalidSlot(index)) {
			return ItemStack.EMPTY;
		}

		int slot = getSlot(index);
		return slot == -1 ? ItemStack.EMPTY : player.inventory.decrStackSize(slot, count);
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		if (isInvalidSlot(index)) {
			return ItemStack.EMPTY;
		}

		int slot = getSlot(index);
		return slot == -1 ? ItemStack.EMPTY : player.inventory.removeStackFromSlot(slot);
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack is) {
		if (isInvalidSlot(index)) {
			return;
		}

		int slot = getSlot(index);

		if (slot != -1) {
			player.inventory.setInventorySlotContents(slot, is);
			markDirty();
		}
	}

	@Override
	public int getInventoryStackLimit() {
		return player.inventory.getInventoryStackLimit();
	}

	@Override
	public void markDirty() {
		player.inventory.markDirty();
		player.openContainer.detectAndSendChanges();
	}

	@Override
	public boolean isUsableByPlayer(PlayerEntity player) {
		return true;
	}

	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack) {
		if (isInvalidSlot(index)) {
			return false;
		}

		int slot = getSlot(index);
		return slot != -1 && player.inventory.isItemValidForSlot(slot, stack);
	}

	@Override
	public void clear() {
		player.inventory.clear();
	}
}