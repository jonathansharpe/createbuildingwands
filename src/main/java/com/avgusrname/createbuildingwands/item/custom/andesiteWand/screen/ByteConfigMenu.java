package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;

import java.util.HashMap;
import java.util.Map;


public class ByteConfigMenu extends AbstractContainerMenu {

    private final ItemStack wandStack;
    private final InteractionHand hand;

    protected ByteConfigMenu(int id, Inventory inv, InteractionHand hand) {
        super(ModMenuTypes.BYTE_CONFIG.get(), id);
        this.hand = hand;
        this.wandStack = inv.player.getItemInHand(hand);
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
