package com.avgusrname.createbuildingwands.item.andesiteWand.screen;

import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.avgusrname.createbuildingwands.util.WandUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SlabConfigMenu extends MultiStateConfigMenu {
    public static final List<String> ORDERED_KEYS = List.of("bottom", "top");

    public static final String AXIS_KEY = "axis";
    public static final String DEFAULT_AXIS = "y";

    protected SlabConfigMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        super(ModMenuTypes.SLAB_CONFIG_MENU.get(), containerId, playerInventory, hand, 2);
        loadSlotsFromWand();
    }

    public SlabConfigMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, WandUtils.deserializeHand(extraData));
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
    public boolean stillValid(@NotNull Player player) {
        return false;
    }
}
