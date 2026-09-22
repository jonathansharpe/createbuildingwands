package com.avgusrname.createbuildingwands.item.andesiteWand.screen;

import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.avgusrname.createbuildingwands.util.WandUtils;
import com.copycatsplus.copycats.foundation.copycat.multistate.MaterialItemStorage;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SlabConfigMenu extends MultiStateConfigMenu {
    public static final List<String> ORDERED_KEYS = List.of("bottom", "top");
    public static final String TYPE_KEY = "type";
    public static final String AXIS_KEY = "axis";
    public static final String DEFAULT_AXIS = "y";

    public SlabConfigMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        super(ModMenuTypes.SLAB_CONFIG_MENU.get(), containerId, playerInventory, hand, 2);
        loadSlotsFromWand();
    }

    public SlabConfigMenu(int pContainerId, Inventory pPlayerInventory, FriendlyByteBuf pExtraData) {
        this(pContainerId, pPlayerInventory, WandUtils.deserializeHand(pExtraData));
    }

    @Override
    public void handleServerToggle(int slotId) {
        if (this.wandItem.isEmpty()) return;

        String partKey = ORDERED_KEYS.get(slotId);
        WandMaterialComponent current = getCurrentComponent();

        boolean currentlyActive = current.isActive(partKey);
        boolean nowActive = !currentlyActive;

        WandMaterialComponent updated = current.withPartActive(partKey, nowActive);

        boolean topActive = "true".equals(updated.blockStateProps().get("top"));
        boolean bottomActive = "true".equals(updated.blockStateProps().get("bottom"));

        String type;
        if (topActive && bottomActive) type = "double";
        else if (topActive) type = "top";
        else if (bottomActive) type = "bottom";
        else type = null;

        if (type != null) {
            updated = updated.withBlockStateProp(TYPE_KEY, type);
        } else {
            Map<String, String> props = new HashMap<>(updated.blockStateProps());
            props.remove(TYPE_KEY);
            updated = new WandMaterialComponent(updated.materialData(), props);
        }

        if (!nowActive) {
            HolderLookup.Provider registries = player.level().registryAccess();
            MaterialItemStorage storage = updated.toStorage(registries);
            storage.storeMaterialItem(partKey, new MaterialItemStorage.MaterialItem(
                    AllBlocks.COPYCAT_BASE.getDefaultState(), ItemStack.EMPTY
            ));
            updated = WandMaterialComponent.fromStorage(storage, updated.blockStateProps(), registries);
            materialSlotHandler.setStackInSlot(slotId, ItemStack.EMPTY);;
        }

        this.wandItem.set(ModDataComponents.WAND_MATERIALS.get(), updated);
        this.broadcastChanges();
    }

    @Override
    protected boolean isPartActive(int slot) {
        return getCurrentComponent().isActive(ORDERED_KEYS.get(slot));
    }

    @Override
    protected int[] getComponentCoordinates(int index) {
        int x = PANEL_INNER_X;
        int y = PANEL_INNER_Y + (index * (BTN_HEIGHT + V_SPACING));
        return new int[]{x, y};
    }

    @Override
    protected int getInventoryY() {
        return 100;
    }

    @Override
    protected int getHotbarY() {
        return 158;
    }

    @Override
    protected void onMaterialChanged(int slot, ItemStack stack) {
        saveSlotMaterialToWand(ORDERED_KEYS.get(slot), stack);
    }

    public void handleAxisChange(String axis) {
        if (this.wandItem.isEmpty()) return;

        WandMaterialComponent current = wandItem.getOrDefault(
                ModDataComponents.WAND_MATERIALS.get(),
                WandMaterialComponent.createEmptyDefault()
        );

        this.wandItem.set(ModDataComponents.WAND_MATERIALS.get(),
                current.withBlockStateProp(AXIS_KEY, axis));
        this.broadcastChanges();
    }

    public String getCurrentAxis() {
        WandMaterialComponent component = wandItem.getOrDefault(
                ModDataComponents.WAND_MATERIALS.get(),
                WandMaterialComponent.createEmptyDefault()
        );
        return component.blockStateProps().getOrDefault(AXIS_KEY, DEFAULT_AXIS);
    }

    @Override
    public List<String> getOrderedKeys() {
        return ORDERED_KEYS;
    }

    @Override
    public @NotNull MenuType<?> getType() { return ModMenuTypes.SLAB_CONFIG_MENU.get(); }

}
