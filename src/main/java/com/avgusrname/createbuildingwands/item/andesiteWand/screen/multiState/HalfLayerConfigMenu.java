package com.avgusrname.createbuildingwands.item.andesiteWand.screen.multiState;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandMaterialComponent;
import com.copycatsplus.copycats.foundation.copycat.multistate.MaterialItemStorage;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.awt.*;
import java.util.List;

public abstract class HalfLayerConfigMenu extends MultiStateConfigMenu {
    public static final List<String> ORDERED_KEYS = List.of("negative_layers", "positive_layers");
    public static final String NEGATIVE_LAYERS_KEY = ORDERED_KEYS.getFirst();
    public static final String POSITIVE_LAYERS_KEY = ORDERED_KEYS.get(1);
    public static final int DEFAULT_LAYERS = 1;
    public static final int MIN_LAYERS = 1;
    public static final int MAX_LAYERS = 8;
    public static final String AXIS_KEY = "axis";
    public static final String DEFAULT_AXIS = "x";
    public static final String HALF_KEY = "half";
    public static final String DEFAULT_HALF = "bottom";
    public static final String FACING_KEY = "facing";
    public static final String DEFAULT_FACING = "south";

    protected HalfLayerConfigMenu(MenuType<?> type, int containerId, Inventory playerInventory, InteractionHand hand, int slotCount) {
        super(type, containerId, playerInventory, hand, slotCount);
    }

    @Override
    protected int[] getComponentCoordinates(int index) {
        int x = PANEL_INNER_X;
        int y = PANEL_INNER_Y + (index * (BTN_HEIGHT + V_SPACING));
        return new int[]{x, y};
    }

    @Override
    protected int getInventoryY() { return 100; }

    @Override
    protected int getHotbarY() { return 158; }

    @Override
    protected void onMaterialChanged(int slot, ItemStack stack) {
        saveSlotMaterialToWand(ORDERED_KEYS.get(slot), stack);
    }

    @Override
    protected boolean isPartActive(int slot) {
        String key = ORDERED_KEYS.get(slot);
        String value = getCurrentComponent(this.wandItem).blockStateProps().get(key);
        CreateBuildingWands.LOGGER.info("[WandDebug] isPartActive slot={}, key={}, value={}", slot, key, value);
        if (value == null) return false;
        try {
            return Integer.parseInt(value) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isPartActiveFromComponent(WandMaterialComponent component, int slot) {
        String key = ORDERED_KEYS.get(slot);
        String value = component.blockStateProps().get(key);
        if (value == null) return false;
        try {
            return Integer.parseInt(value) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void handleServerToggle(int slotId) {
        if (this.wandItem.isEmpty()) return;

        String partKey = ORDERED_KEYS.get(slotId);
        WandMaterialComponent current = getCurrentComponent(this.wandItem);

        boolean currentlyActive = isPartActive(slotId);
        boolean nowActive = !currentlyActive;

        WandMaterialComponent updated;
        if (nowActive) {
            updated = current.withBlockStateProp(partKey, String.valueOf(DEFAULT_LAYERS));
        } else {
            updated = current.withBlockStateProp(partKey, "0");
            HolderLookup.Provider registries = player.level().registryAccess();
            MaterialItemStorage storage = updated.toStorage(wandItem.get(ModDataComponents.WAND_BLOCK_COPYCAT), registries);
            storage.storeMaterialItem(partKey, new MaterialItemStorage.MaterialItem(
                    AllBlocks.COPYCAT_BASE.getDefaultState(), ItemStack.EMPTY
            ));
            updated = WandMaterialComponent.fromStorage(storage, updated.blockStateProps(), registries);
            materialSlotHandler.setStackInSlot(slotId, ItemStack.EMPTY);
        }

        this.wandItem.set(ModDataComponents.WAND_MATERIALS.get(), updated);
        this.broadcastChanges();
    }

    public void handleServerLayerChange(String layerKey, int value) {
        if (this.wandItem.isEmpty()) return;
        int newLayers = Math.clamp(value, MIN_LAYERS, MAX_LAYERS);
        this.wandItem.set(ModDataComponents.WAND_MATERIALS.get(),
                getCurrentComponent(this.wandItem).withBlockStateProp(layerKey, String.valueOf(newLayers)));
        this.broadcastChanges();
    }

    public void handleServerFacingChange(String direction) {
        if (this.wandItem.isEmpty()) return;

        WandMaterialComponent current = getCurrentComponent(this.wandItem);

        this.wandItem.set(ModDataComponents.WAND_MATERIALS.get(),
                current.withBlockStateProp(FACING_KEY, direction));
        this.broadcastChanges();
    }

    public void handleServerHalfChange(String half) {
        if (this.wandItem.isEmpty()) return;
        this.wandItem.set(ModDataComponents.WAND_MATERIALS.get(),
                getCurrentComponent(this.wandItem).withBlockStateProp(HALF_KEY, half));
        this.broadcastChanges();
    }

    public void handleServerAxisChange(String axis) {
        if (this.wandItem.isEmpty()) return;

        WandMaterialComponent current = getCurrentComponent(this.wandItem);

        this.wandItem.set(ModDataComponents.WAND_MATERIALS.get(),
                current.withBlockStateProp(AXIS_KEY, axis));
        this.broadcastChanges();
    }

    public int getNegativeLayers() {
        return Integer.parseInt(getCurrentComponent(this.wandItem).blockStateProps()
                .getOrDefault(NEGATIVE_LAYERS_KEY, String.valueOf(DEFAULT_LAYERS)));
    }

    public int getPositiveLayers() {
        return Integer.parseInt(getCurrentComponent(this.wandItem).blockStateProps()
                .getOrDefault(POSITIVE_LAYERS_KEY, String.valueOf(DEFAULT_LAYERS)));
    }
    @Override
    public List<String> getOrderedKeys() { return ORDERED_KEYS; }
}
