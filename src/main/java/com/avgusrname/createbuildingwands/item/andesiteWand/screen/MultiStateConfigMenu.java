package com.avgusrname.createbuildingwands.item.andesiteWand.screen;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class MultiStateConfigMenu extends AbstractContainerMenu {

    protected final ItemStack wandItem;
    protected final InteractionHand wandHand;
    protected final Player player;
    protected final MultiStateMaterialSlotHandler materialSlotHandler;
    public final int slotCount;

    public static final int BTN_WIDTH = 80;
    public static final int BTN_HEIGHT = 20;

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (index >= slotCount) {
            Slot sourceSlot = this.slots.get(index);
            if (sourceSlot.hasItem()) {
                ItemStack stackInSource = sourceSlot.getItem();
                ItemStack stackToMove = stackInSource.copyWithCount(1);

                for (int i = 0; i < slotCount; i++) {
                    if (this.materialSlotHandler.getStackInSlot(i).isEmpty()) {
                        ItemStack result = this.materialSlotHandler.insertItem(i, stackToMove, false);
                        if (!result.isEmpty()) {
                            this.broadcastChanges();
                            break;
                        }
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    protected MultiStateConfigMenu(MenuType<?> type, int containerId, Inventory playerInventory, InteractionHand hand, int slotCount) {
        super(type, containerId);
        this.wandHand = hand;
        this.wandItem = playerInventory.player.getItemInHand(hand);
        this.player = playerInventory.player;
        this.slotCount = slotCount;

        this.materialSlotHandler = new MultiStateMaterialSlotHandler(
                slotCount, this, wandItem, this::onMaterialChanged, this::isPartActive
        );

        for (int i = 0; i < slotCount; i++) {
            int[] coords = getComponentCoordinates(i);
            int slotX = coords[0] + BTN_WIDTH + 4;
            int slotY = coords[1] + 1;
            this.addSlot(new SlotItemHandler(materialSlotHandler, i, slotX, slotY));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, col * 18, getInventoryY() + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, getHotbarY()));
        }


    }
    protected abstract int[] getComponentCoordinates(int index);
    protected abstract int getInventoryY();
    protected abstract int getHotbarY();
    protected abstract void onMaterialChanged(int slot, ItemStack stack);
    protected abstract boolean isPartActive(int slot);
    public abstract List<String> getOrderedKeys();
}
