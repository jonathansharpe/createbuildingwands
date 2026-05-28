package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen.ByteCornerData.ByteCopycatCorner;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ByteConfigMenu extends AbstractContainerMenu {
    public static final MenuType<ByteConfigMenu> TYPE = null;
    
    private final IItemHandler cornerItemHandler;
    private final int lockedWandSlotIndex;

    public ByteConfigMenu(int containerId, Inventory playerInventory, IItemHandler itemHandler, int lockedWandSlotIndex) {
        super(TYPE, containerId);
        this.cornerItemHandler = itemHandler;
        this.lockedWandSlotIndex = lockedWandSlotIndex;

        int btnWidth = 34;
        int btnHeight = 20;
        int spacing = 4;

        for (ByteCopycatCorner corner : ByteCopycatCorner.values()) {
            int i = corner.ordinal();

            int group = i / 4;
            int column = i % 2;
            int row = (i % 4) / 2;

            // EXACT mirror of your Screen layout coordinates, shifted right by 38 pixels
            // to put the item slot neatly to the right of the button text box
            int xPos = 12 + (group * (btnWidth * 2 + 16)) + (column * (btnWidth + spacing)) + 38;
            int yPos = 24 + (row * (btnHeight + spacing)) + 2;
            this.addSlot(new WandBlockSlot(itemHandler, i, xPos, yPos));
        }

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
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == -999) {
            super.clicked(slotId, button, clickType, player);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();

            // Index 0-7 are our custom corner input slots
            if (index < 8) {
                if (!this.moveItemStackTo(itemStack2, 8, 44, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // Index 8-43 represents player inventory space
            else {
                if (!(itemStack2.getItem() instanceof BlockItem) || !this.moveItemStackTo(itemStack2, 0, 8, false)) {
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

    public void handleServerToggle(ByteCopycatCorner corner) {
        int actualSlotIndex = this.lockedWandSlotIndex + 35;

        CreateBuildingWands.LOGGER.info("[WandDebug ByteConfigMenu] attempting to toggle the corner on the server");

        if (actualSlotIndex >= 0 && actualSlotIndex < this.slots.size()) {

            CreateBuildingWands.LOGGER.info("[WandDebug ByteConfigMenu] actualSlotIndex is within range yippee: {}", actualSlotIndex);
            ItemStack wandStack = this.slots.get(actualSlotIndex).getItem();

            if (!wandStack.isEmpty()) {
                CustomData.update(
                        DataComponents.CUSTOM_DATA,
                        wandStack,
                        tag -> {

                            int[] activeCorners = tag.getIntArray("active_corners");
                            if (activeCorners.length != 8) {
                                activeCorners = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
                            }

                            int index = corner.ordinal();
                            activeCorners[index] = (activeCorners[index] == 1) ? 0 : 1;

                            tag.putIntArray("active_corners", activeCorners);
                            CompoundTag materialData = tag.getCompound("material_data");
                            CompoundTag cornerTag = materialData.getCompound(corner.getNbtKey());
                            cornerTag.putByte("enableCT", (byte) 0);

                            CreateBuildingWands.LOGGER.info("[WandDebug ByteConfigMenu] tag value is: {}", tag);
                        });
            }
            this.broadcastChanges();
        }

    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public int getLockedWandSlotIndex() {
        return this.lockedWandSlotIndex;
    }

    public static MenuType<ByteConfigMenu> getTypeReference() {
        return ModMenuTypes.BYTE_CONFIG_MENU.get();
    }

    @Override
    public MenuType<?> getType() {
        return ModMenuTypes.BYTE_CONFIG_MENU.get();
    }

}