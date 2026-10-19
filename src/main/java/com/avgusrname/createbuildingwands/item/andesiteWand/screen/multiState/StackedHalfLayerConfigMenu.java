package com.avgusrname.createbuildingwands.item.andesiteWand.screen.multiState;

import com.avgusrname.createbuildingwands.item.andesiteWand.screen.ModMenuTypes;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandMaterialComponent;
import com.avgusrname.createbuildingwands.util.WandUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackedHalfLayerConfigMenu extends HalfLayerConfigMenu {
    public static final String NEGATIVE_LAYERS_KEY = ORDERED_KEYS.getFirst();
    public static final String POSITIVE_LAYERS_KEY = ORDERED_KEYS.get(1);
    public static final int DEFAULT_LAYERS = 1;
    public static final int MIN_LAYERS = 1;
    public static final int MAX_LAYERS = 8;

    public StackedHalfLayerConfigMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        super(ModMenuTypes.STACKED_HALF_LAYER_CONFIG_MENU.get(), containerId, playerInventory, hand, 2);
        loadSlotsFromWand();
    }

    public StackedHalfLayerConfigMenu(int pContainerId, Inventory pPlayerInventory, FriendlyByteBuf pExtraData) {
        this(pContainerId, pPlayerInventory, WandUtils.deserializeHand(pExtraData));
    }

    public static WandMaterialComponent createEmptyDefault() {
        Map<String, String> defaults = new HashMap<>();

        defaults.put(NEGATIVE_LAYERS_KEY, String.valueOf(DEFAULT_LAYERS));
        defaults.put(POSITIVE_LAYERS_KEY, String.valueOf(DEFAULT_LAYERS));
        defaults.put(FACING_KEY, DEFAULT_FACING);

        return new WandMaterialComponent(new CompoundTag(), defaults);
    }

    @Override
    public @NotNull MenuType<?> getType() { return ModMenuTypes.STACKED_HALF_LAYER_CONFIG_MENU.get(); }
}
