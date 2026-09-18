package com.avgusrname.createbuildingwands.item.andesiteWand.screen;

import com.avgusrname.createbuildingwands.util.WandUtils;
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlock;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntPredicate;

public class MultiStateMaterialSlotHandler extends ItemStackHandler {
    private final AbstractContainerMenu menu;
    private final ItemStack wandItem;
    private final OnMaterialChanged onChanged;
    private final IntPredicate isPartActive;

    @FunctionalInterface
    public interface OnMaterialChanged {
        void onChange(int slot, ItemStack stack);
    }

    public MultiStateMaterialSlotHandler(int size, AbstractContainerMenu menu, ItemStack wandItem, OnMaterialChanged onChanged, IntPredicate isPartActive) {
        super(size);
        this.menu = menu;
        this.wandItem = wandItem;
        this.onChanged = onChanged;
        this.isPartActive = isPartActive;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack){
        if (!isPartActive.test(slot)) return false;
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        Block block = blockItem.getBlock();
        if (block instanceof ICopycatBlock || block instanceof CopycatBlock) return false;
        return WandUtils.isFullBlock(block);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) return stack;
        if (!isItemValid(slot, stack)) return ItemStack.EMPTY;

        if (!simulate) {
            this.stacks.set(slot, stack.copyWithCount(1));
            this.onContentsChanged(slot);
        }
        return stack;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!getStackInSlot(slot).isEmpty() && !simulate) {
            setStackInSlot(slot, ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void onContentsChanged(int slot) {
        onChanged.onChange(slot, getStackInSlot(slot));
        menu.broadcastChanges();
    }
}
