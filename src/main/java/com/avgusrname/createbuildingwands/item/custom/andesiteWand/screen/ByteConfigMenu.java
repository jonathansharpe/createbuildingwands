package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

import java.util.HashMap;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ByteConfigMenu extends AbstractContainerMenu {

    private final ItemStack wandStack;
    private final InteractionHand hand;
    private final Player player;
    private final List<String> byteProperties;
    private final Map<String, ItemStackHandler> byteSlotHandlers = new HashMap<>();
    
    public ByteConfigMenu(int id, Inventory inv, InteractionHand hand) {
        super(ModMenuTypes.BYTE_CONFIG.get(), id);
        this.hand = hand;
        this.player = inv.player;
        this.wandStack = inv.player.getItemInHand(hand);
    }
}
