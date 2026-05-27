package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ByteConfigMenu extends AbstractContainerMenu {
    public static final MenuType<ByteConfigMenu> TYPE = null;
    
    private final IItemHandler testItemHandler;
    private final int lockedWandSlotIndex;

    public ByteConfigMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new ItemStackHandler(1), -1);
    }

    public ByteConfigMenu(int containerId, Inventory playerInventory, IItemHandler itemHandler, int lockedWandSlotIndex) {
        super(TYPE, containerId);
        this.testItemHandler = itemHandler;
        this.lockedWandSlotIndex = lockedWandSlotIndex;

        this.addSlot(new WandBlockSlot(itemHandler, 0, 111, 32));

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142) {
                @Override
                public boolean mayPickup(Player player) {
                    return this.getSlotIndex() != ByteConfigMenu.this.lockedWandSlotIndex;
                }
            });
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();

            if (index == 0) {
                if (!this.moveItemStackTo(itemStack2, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!(itemStack2.getItem() instanceof BlockItem) || !this.moveItemStackTo(itemStack2, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemStack2.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemStack2);
        }

        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}