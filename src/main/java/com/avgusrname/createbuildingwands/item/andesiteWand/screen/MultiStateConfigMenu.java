package com.avgusrname.createbuildingwands.item.andesiteWand.screen;

import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.copycatsplus.copycats.foundation.copycat.multistate.MaterialItemStorage;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.datafix.fixes.ItemStackTagFix;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.checkerframework.checker.units.qual.N;
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
    public static final int SLOT_SIZE = 18;
    public static final int H_SPACING = 12;
    public static final int V_SPACING = 6;
    public static final int PANEL_INNER_X = 12;
    public static final int PANEL_INNER_Y = 18;


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

    protected WandMaterialComponent getCurrentComponent() {
        return wandItem.getOrDefault(
                ModDataComponents.WAND_MATERIALS.get(),
                WandMaterialComponent.createEmptyDefault()
        );
    }

    protected void loadSlotsFromWand() {
        MaterialItemStorage storage = getCurrentComponent().toStorage(player.level().registryAccess());

        for (int i = 0; i < getOrderedKeys().size(); i++) {
            MaterialItemStorage.MaterialItem item = storage.getMaterialItem(getOrderedKeys().get(i));
            if (item == null || item.consumedItem().isEmpty()) continue;
            materialSlotHandler.setStackInSlot(i, item.consumedItem().copy());
        }
    }

    protected void saveSlotMaterialToWand(String propertyKey, ItemStack newConsumedItem) {
        if (this.wandItem.isEmpty()) return;

        WandMaterialComponent current = getCurrentComponent();
        HolderLookup.Provider registries = player.level().registryAccess();
        MaterialItemStorage storage = current.toStorage(registries);

        BlockState newMaterial = AllBlocks.COPYCAT_BASE.getDefaultState();
        if (!newConsumedItem.isEmpty() && newConsumedItem.getItem() instanceof BlockItem blockItem) {
            newMaterial = blockItem.getBlock().defaultBlockState();
        }

        storage.storeMaterialItem(propertyKey, new MaterialItemStorage.MaterialItem(
                newMaterial, newConsumedItem.isEmpty() ? ItemStack.EMPTY : newConsumedItem.copyWithCount(1)));

        this.wandItem.set(ModDataComponents.WAND_MATERIALS.get(),
                WandMaterialComponent.fromStorage(storage, current.blockStateProps(), registries));

        this.broadcastChanges();
    }

    public void handleServerToggle(int slotId) {
        if (this.wandItem.isEmpty()) return;

        String propertyKey = getOrderedKeys().get(slotId);
        WandMaterialComponent current = getCurrentComponent();

        boolean nowActive = !current.isActive(propertyKey);
        WandMaterialComponent updated = current.withPartActive(propertyKey, nowActive);

        if (!nowActive) {
            HolderLookup.Provider registries = player.level().registryAccess();
            MaterialItemStorage storage = updated.toStorage(registries);
            storage.storeMaterialItem(propertyKey, new MaterialItemStorage.MaterialItem(
                    AllBlocks.COPYCAT_BASE.getDefaultState(), ItemStack.EMPTY
            ));
            updated = WandMaterialComponent.fromStorage(storage, updated.blockStateProps(), registries);
            materialSlotHandler.setStackInSlot(slotId, ItemStack.EMPTY);
        }

        this.wandItem.set(ModDataComponents.WAND_MATERIALS.get(), updated);
        this.broadcastChanges();
    }

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

    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        if (slotId >= 0 && slotId < slotCount) {
            Slot targetSlot = this.slots.get(slotId);
            ItemStack carriedStack = this.getCarried();
            String propertyKey = getOrderedKeys().get(slotId);

            if (!carriedStack.isEmpty() && carriedStack.getItem() instanceof BlockItem) {
                if (clickType == ClickType.CLONE || clickType == ClickType.SWAP || clickType == ClickType.THROW) {
                    super.clicked(slotId, button, clickType, player);
                    return;
                }
                ItemStack ghostCopy = carriedStack.copyWithCount(1);
                ItemStack result = this.materialSlotHandler.insertItem(slotId, ghostCopy, false);
                if (!result.isEmpty()) {
                    targetSlot.setChanged();
                    this.saveSlotMaterialToWand(propertyKey, ghostCopy);
                }
                return;
            } else if (carriedStack.isEmpty() && !targetSlot.getItem().isEmpty()) {
                this.materialSlotHandler.setStackInSlot(slotId, ItemStack.EMPTY);
                targetSlot.setChanged();
                this.saveSlotMaterialToWand(propertyKey, ItemStack.EMPTY);
                return;
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    protected abstract int[] getComponentCoordinates(int index);
    protected abstract int getInventoryY();
    protected abstract int getHotbarY();
    protected abstract void onMaterialChanged(int slot, ItemStack stack);
    protected abstract boolean isPartActive(int slot);
    public abstract List<String> getOrderedKeys();
}
