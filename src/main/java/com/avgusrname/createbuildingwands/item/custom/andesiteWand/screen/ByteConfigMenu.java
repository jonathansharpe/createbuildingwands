package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;

import java.util.HashMap;
import java.util.Map;

public class ByteConfigMenu extends AbstractContainerMenu {

    private final ItemStack wandStack;
    private final InteractionHand hand;
    private final HolderLookup.Provider registries;

    public ByteConfigMenu(int id, Inventory inv, InteractionHand hand) {
        super(ModMenuTypes.BYTE_CONFIG.get(), id);
        this.hand = hand;
        this.wandStack = inv.player.getItemInHand(hand);
        this.registries = inv.player.registryAccess();
    }

    public boolean getByte(String propertyName) {
        BlockItemStateProperties properties = wandStack.get(DataComponents.BLOCK_STATE);
        if (properties == null) return false;

        String value = properties.properties().get(propertyName);
        return "true".equals(value);
    }

    public void toggleByte(String propertyName) {
        BlockItemStateProperties current = wandStack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);

        Map<String, String> newMap = new HashMap<>(current.properties());

        boolean currentVal = "true".equals(newMap.get(propertyName));
        newMap.put(propertyName, String.valueOf(!currentVal));

        wandStack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(newMap));
    }

    public ItemStack getMaterialForPart(String propertyName) {
        CustomData customData = wandStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return ItemStack.EMPTY;

        CompoundTag tag = customData.copyTag();
        if (tag.contains("PartMaterials")) {
            CompoundTag materials = tag.getCompound("PartMaterials");
            if (materials.contains(propertyName)) {
                return ItemStack.parseOptional(registries, materials.getCompound(propertyName));
            }
        }
        return ItemStack.EMPTY;
    }

    public void setMaterialForPart(String propertyName, ItemStack stack) {
        CustomData customData = wandStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        CompoundTag materials;
        if (tag.contains("PartMaterials")) {
            materials = tag.getCompound("PartMaterials");
        }
        else {
            materials = new CompoundTag();
        }

        if (stack.isEmpty()) {
            materials.remove(propertyName);
        }
        else {
            materials.put(propertyName, stack.save(registries));
        }

        tag.put("PartMaterials", materials);
        wandStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    // will likely leave this un implemented because idk how you can shift-click into a specific slot without putting it in all of them
    @Override
    public ItemStack quickMoveStack(Player p, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p) {
        return p.getItemInHand(hand).equals(wandStack);
    }
    
}
