package com.avgusrname.createbuildingwands.item.andesiteWand.screen.multiState;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
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
import java.util.Map;

public class StandardHalfLayerConfigMenu extends HalfLayerConfigMenu {

    public StandardHalfLayerConfigMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        super(ModMenuTypes.STANDARD_HALF_LAYER_CONFIG_MENU.get(), containerId, playerInventory, hand, 2);
        loadSlotsFromWand();
    }

    public StandardHalfLayerConfigMenu(int pContainerId, Inventory pPlayerInventory, FriendlyByteBuf pExtraData) {
        this(pContainerId, pPlayerInventory, WandUtils.deserializeHand(pExtraData));
    }

    public static WandMaterialComponent createEmptyDefault() {
        CreateBuildingWands.LOGGER.info("the current component is empty, setting to defaults");
        Map<String, String> defaults = new HashMap<>();

        defaults.put(NEGATIVE_LAYERS_KEY, String.valueOf(DEFAULT_LAYERS));
        defaults.put(POSITIVE_LAYERS_KEY, String.valueOf(DEFAULT_LAYERS));
        defaults.put(AXIS_KEY, DEFAULT_AXIS);
        defaults.put(HALF_KEY, DEFAULT_HALF);

        return new WandMaterialComponent(new CompoundTag(), defaults);
    }

    @Override
    public @NotNull MenuType<?> getType() { return ModMenuTypes.STANDARD_HALF_LAYER_CONFIG_MENU.get(); }
}
