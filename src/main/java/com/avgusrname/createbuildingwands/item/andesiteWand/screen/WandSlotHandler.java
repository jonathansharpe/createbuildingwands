package com.avgusrname.createbuildingwands.item.andesiteWand.screen;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class WandSlotHandler extends ItemStackHandler {
    private final Predicate<ItemStack> validator;
    private final Runnable onChanged;
    private boolean isUpdating = false;

    public WandSlotHandler(Predicate<ItemStack> validator, Runnable onChanged) {
        super(1);
        this.validator = validator;
        this.onChanged = onChanged;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return !stack.isEmpty() && validator.test(stack);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return stack;
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
        if (isUpdating) return;
        isUpdating = true;
        try {
            onChanged.run();
        } finally {
            isUpdating = false;
        }
    }
}
