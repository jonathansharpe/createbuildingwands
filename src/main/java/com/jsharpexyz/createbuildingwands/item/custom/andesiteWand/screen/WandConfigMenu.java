package com.jsharpexyz.createbuildingwands.item.custom.andesiteWand.screen;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

import com.jsharpexyz.createbuildingwands.item.custom.andesiteWand.AndesiteWandItem;

import net.minecraft.network.FriendlyByteBuf;

public class WandConfigMenu extends AbstractContainerMenu{

    private final InteractionHand wandHand;
    private final Inventory playerInventory;

    public WandConfigMenu(int pContainerId, Inventory pPlayerInventory, InteractionHand wandHand) {
        super(ModMenuTypes.WAND_CONFIG_MENU.get(), pContainerId);
        this.wandHand = wandHand;
        this.playerInventory = pPlayerInventory;
    }

    public WandConfigMenu(int pContainerId, Inventory pPlayerInventory, FriendlyByteBuf pExtraData) {
        this(pContainerId, pPlayerInventory, deserializeHand(pExtraData));
    }

    private static InteractionHand deserializeHand(FriendlyByteBuf pExtraData) {
        if (pExtraData != null) {
            return pExtraData.readEnum(InteractionHand.class);
        }
        return InteractionHand.MAIN_HAND;
    }

    public InteractionHand getWandHand() {
        return wandHand;
    }
    
    public Inventory getPlayerInventory() {
        return playerInventory;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return null;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return pPlayer.getItemInHand(this.wandHand).getItem() instanceof AndesiteWandItem;
    }
}
