package com.avgusrname.createbuildingwands.item.andesiteWand.screen;

import com.avgusrname.createbuildingwands.component.ModDataComponents;

import com.copycatsplus.copycats.content.copycat.bytes.CopycatByteBlock;
import com.copycatsplus.copycats.foundation.copycat.multistate.MaterialItemStorage;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ByteConfigMenu extends MultiStateConfigMenu {

    public static final int SLOT_SIZE = 18;
    public static final int H_SPACING = 12;
    public static final int V_SPACING = 6;

    public static final int PANEL_INNER_X = 12;
    public static final int PANEL_INNER_Y = 18;

    public static final List<String> ORDERED_KEYS = List.of(
            CopycatByteBlock.BOTTOM_NW.getName(),
            CopycatByteBlock.BOTTOM_NE.getName(),
            CopycatByteBlock.BOTTOM_SW.getName(),
            CopycatByteBlock.BOTTOM_SE.getName(),
            CopycatByteBlock.TOP_NW.getName(),
            CopycatByteBlock.TOP_NE.getName(),
            CopycatByteBlock.TOP_SW.getName(),
            CopycatByteBlock.TOP_SE.getName()
    );

    /**
     * creates a container to store the information for the
     * @param pContainerId
     * @param pPlayerInventory
     * @param pHand
     */
    public ByteConfigMenu(int pContainerId, Inventory pPlayerInventory, InteractionHand pHand) {
        super(ModMenuTypes.BYTE_CONFIG_MENU.get(), pContainerId, pPlayerInventory, pHand, 8);
        loadSlotsFromWand();
    }

    private void loadSlotsFromWand() {
        WandMaterialComponent component = wandItem.getOrDefault(
                ModDataComponents.WAND_MATERIALS.get(),
                WandMaterialComponent.createEmptyDefault()
        );
        MaterialItemStorage storage = component.toStorage(player.level().registryAccess());

        for (int i = 0; i < ORDERED_KEYS.size(); i++) {
            MaterialItemStorage.MaterialItem item = storage.getMaterialItem(ORDERED_KEYS.get(i));
            if (item == null || item.consumedItem().isEmpty()) continue;
            materialSlotHandler.setStackInSlot(i, item.consumedItem().copy());
        }
    }

    @Override
    public List<String> getOrderedKeys() { return ORDERED_KEYS;}

    @Override
    protected void onMaterialChanged(int slot, ItemStack stack) {
        saveSlotMaterialToWand(ORDERED_KEYS.get(slot), stack);
    }

    @Override
    protected int[] getComponentCoordinates(int index) {
        int row = index % 4;
        int col = index / 4;

        int columnWidth = BTN_WIDTH + 4 + SLOT_SIZE + H_SPACING;

        int x = PANEL_INNER_X + (col * columnWidth);
        int y = PANEL_INNER_Y + (row * (BTN_HEIGHT + V_SPACING));

        return new int[]{x, y};
    }

    @Override
    protected int getInventoryY() { return 132; }

    @Override
    protected int getHotbarY() { return 190; }

    @Override
    protected boolean isPartActive(int slot) {
        WandMaterialComponent component = wandItem.getOrDefault(
                ModDataComponents.WAND_MATERIALS.get(),
                WandMaterialComponent.createEmptyDefault()
        );
        return component.isActive(ORDERED_KEYS.get(slot));
    }

    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        if (slotId >= 0 && slotId < slotCount) {
            Slot targetslot = this.slots.get(slotId);
            ItemStack carriedStack = this.getCarried();
            String propertyKey = ORDERED_KEYS.get(slotId);

            if (!carriedStack.isEmpty() && carriedStack.getItem() instanceof BlockItem blockItem) {
                if (clickType == ClickType.CLONE || clickType == ClickType.SWAP || clickType == ClickType.THROW) {
                    super.clicked(slotId, button, clickType, player);
                    return;
                }

                ItemStack ghostCopy = carriedStack.copyWithCount(1);
                ItemStack result = this.materialSlotHandler.insertItem(slotId, ghostCopy, false);

                if (!result.isEmpty()) {
                    targetslot.setChanged();
                    this.saveSlotMaterialToWand(propertyKey, ghostCopy);
                }
                return;

            } else if (carriedStack.isEmpty() && !targetslot.getItem().isEmpty()) {
                this.materialSlotHandler.setStackInSlot(slotId, ItemStack.EMPTY);
                targetslot.setChanged();
                this.saveSlotMaterialToWand(propertyKey, ItemStack.EMPTY);
                return;
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    /**
     * toggling a slot on the server
     * @param slotId the slot to toggle
     */
    public void handleServerToggle(int slotId) {
        if (!this.wandItem.isEmpty()) {

            String propertyKey = ORDERED_KEYS.get(slotId);
            WandMaterialComponent current = this.wandItem.getOrDefault(
                    ModDataComponents.WAND_MATERIALS.get(),
                    WandMaterialComponent.createEmptyDefault()
            );

            BooleanProperty prop = CopycatByteBlock.byByte(CopycatByteBlock.byteMap.get(propertyKey));
            boolean currentlyActive = current.isActive(propertyKey);
            boolean nowActive = !currentlyActive;

            WandMaterialComponent updated = current.withCornerActive(propertyKey, nowActive);

            if (!nowActive) {
                HolderLookup.Provider registries = player.level().registryAccess();
                MaterialItemStorage storage = updated.toStorage(registries);
                storage.storeMaterialItem(propertyKey, new MaterialItemStorage.MaterialItem(
                        AllBlocks.COPYCAT_BASE.getDefaultState(), ItemStack.EMPTY
                ));
                updated = WandMaterialComponent.fromStorage(storage, updated.activeParts(), registries);
                materialSlotHandler.setStackInSlot(slotId, ItemStack.EMPTY);
            }

            this.wandItem.set(ModDataComponents.WAND_MATERIALS.get(), updated);
            this.broadcastChanges();
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    public InteractionHand getWandHand() {
        return wandHand;
    }

    public ItemStack getWandItem() {
        return wandItem;
    }

    public static MenuType<ByteConfigMenu> getTypeReference() {
        return ModMenuTypes.BYTE_CONFIG_MENU.get();
    }

    @Override
    public @NotNull MenuType<?> getType() {
        return ModMenuTypes.BYTE_CONFIG_MENU.get();
    }

    public ByteConfigMenu(int pContainerId, Inventory pPlayerInventory, FriendlyByteBuf pExtraData) {
        this(pContainerId, pPlayerInventory, deserializeHand(pExtraData));
    }

    private static InteractionHand deserializeHand(FriendlyByteBuf pExtraData) {
        if (pExtraData != null) {
            return pExtraData.readEnum(InteractionHand.class);
        }
        return InteractionHand.MAIN_HAND;
    }

    /**
     * this method will apply a material to the given corner of the copycat byte
     * @param corner the corner to modify the data of
     * @param newConsumedItem the item stack representing the block to use as a material texture
     */
    private void saveSlotMaterialToWand(String propertyKey, ItemStack newConsumedItem) {
        if (this.wandItem.isEmpty()) return;

        WandMaterialComponent current = wandItem.getOrDefault(
                ModDataComponents.WAND_MATERIALS.get(),
                WandMaterialComponent.createEmptyDefault()
        );

        HolderLookup.Provider registries = player.level().registryAccess();
        MaterialItemStorage storage = current.toStorage(registries);

        BlockState newMaterial = AllBlocks.COPYCAT_BASE.getDefaultState();
        if (!newConsumedItem.isEmpty() && newConsumedItem.getItem() instanceof BlockItem blockItem) {
            newMaterial = blockItem.getBlock().defaultBlockState();
        }

        storage.storeMaterialItem(propertyKey, new MaterialItemStorage.MaterialItem(newMaterial, newConsumedItem.isEmpty() ? ItemStack.EMPTY : newConsumedItem.copyWithCount(1)));

        this.wandItem.set(ModDataComponents.WAND_MATERIALS.get(),
                WandMaterialComponent.fromStorage(storage, current.activeParts(), registries));

        this.broadcastChanges();
    }
}