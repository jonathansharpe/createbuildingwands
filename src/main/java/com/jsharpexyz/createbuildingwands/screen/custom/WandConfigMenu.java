package com.jsharpexyz.createbuildingwands.screen.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

import com.jsharpexyz.createbuildingwands.item.custom.andesiteWand.AndesiteWandItem;
import com.jsharpexyz.createbuildingwands.screen.ModMenuTypes;

import net.minecraft.network.FriendlyByteBuf;

public class WandConfigMenu extends AbstractContainerMenu{

    private final InteractionHand wandHand;

    public WandConfigMenu(int pContainerId, Inventory pPlayerInventory, InteractionHand wandHand) {
        super(ModMenuTypes.WAND_CONFIG_MENU.get(), pContainerId);
        this.wandHand = wandHand;
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

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return null;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return pPlayer.getItemInHand(this.wandHand).getItem() instanceof AndesiteWandItem;
    }
}
