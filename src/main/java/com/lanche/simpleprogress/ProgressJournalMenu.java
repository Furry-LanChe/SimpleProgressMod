package com.lanche.simpleprogress;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class ProgressJournalMenu extends AbstractContainerMenu {

    public ProgressJournalMenu(int containerId, Inventory playerInventory) {
        super(ModMenus.PROGRESS_JOURNAL.get(), containerId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}