package dev.ftb.mods.ftbessentials.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * @author LatvianModder
 */
public class OtherPlayerInventory implements Container {
	public final ServerPlayer player;

	public OtherPlayerInventory(ServerPlayer p) {
		player = p;
	}

	@Override
	public int getContainerSize() {
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
	public ItemStack getItem(int index) {
		if (isInvalidSlot(index)) {
			return ItemStack.EMPTY;
		}

		int slot = getSlot(index);
		return slot == -1 ? ItemStack.EMPTY : player.inventory.getItem(slot);
	}

	@Override
	public ItemStack removeItem(int index, int count) {
		if (isInvalidSlot(index)) {
			return ItemStack.EMPTY;
		}

		int slot = getSlot(index);
		return slot == -1 ? ItemStack.EMPTY : player.inventory.removeItem(slot, count);
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		if (isInvalidSlot(index)) {
			return ItemStack.EMPTY;
		}

		int slot = getSlot(index);
		return slot == -1 ? ItemStack.EMPTY : player.inventory.removeItemNoUpdate(slot);
	}

	@Override
	public void setItem(int index, ItemStack is) {
		if (isInvalidSlot(index)) {
			return;
		}

		int slot = getSlot(index);

		if (slot != -1) {
			player.inventory.setItem(slot, is);
			setChanged();
		}
	}

	@Override
	public int getMaxStackSize() {
		return player.inventory.getMaxStackSize();
	}

	@Override
	public void setChanged() {
		player.inventory.setChanged();
		player.containerMenu.broadcastChanges();
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		if (isInvalidSlot(index)) {
			return false;
		}

		int slot = getSlot(index);
		return slot != -1 && player.inventory.canPlaceItem(slot, stack);
	}

	@Override
	public void clearContent() {
		player.inventory.clearContent();
	}
}