package com.jsharpexyz.createbuildingwands.item.custom.andesiteWand.screen;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import com.jsharpexyz.createbuildingwands.component.BlockReferenceComponent;
import com.jsharpexyz.createbuildingwands.component.ModDataComponents;
import com.jsharpexyz.createbuildingwands.item.custom.WandMode;
import com.jsharpexyz.createbuildingwands.item.custom.andesiteWand.AndesiteWandItem;

import net.minecraft.network.FriendlyByteBuf;

public class WandConfigMenu extends AbstractContainerMenu{

    private final InteractionHand wandHand;
    private final ItemStack wandItem;
    private final Level wandLevel;

    private final Inventory actualPlayerInventory;
    private final IItemHandler playerInventoryWrapper;

    private final int initialModeIndex;

    public static final int WAND_SLOT_X = 153;
    public static final int WAND_SLOT_Y = 16;
    public static final int INVENTORY_START_X = 8;
    public static final int INVENTORY_START_Y = 60;
    public static final int HOTBAR_START_Y = 118;

    private final ItemStackHandler wandSlotHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            ItemStack storedBlock = getStackInSlot(0);

            if (storedBlock.isEmpty()) {
                wandItem.remove(ModDataComponents.WAND_BLOCK.get());
            }
            else {
                BlockReferenceComponent component = new BlockReferenceComponent(storedBlock);
                wandItem.set(ModDataComponents.WAND_BLOCK.get(), component);
            }

            WandConfigMenu.this.broadcastChanges();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {

            System.out.println("Wand Slot insertItem() called; Performing overwrite.");

            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                return stack;
            }

            ItemStack stackToStore = stack.copyWithCount(1);

            if (!simulate) {
                this.setStackInSlot(slot, ItemStack.EMPTY);
                this.setStackInSlot(slot, stackToStore);
            }
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {

            System.out.println("Wand Slot extractItem() called: Clearing reference slot.");

            if (!this.getStackInSlot(slot).isEmpty()) {
                if (!simulate) {
                    this.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
            return ItemStack.EMPTY;
        }
    };

    public WandConfigMenu(int pContainerId, Inventory pPlayerInventory, InteractionHand pHand) {
        super(ModMenuTypes.WAND_CONFIG_MENU.get(), pContainerId);

        this.wandHand = pHand;
        this.wandItem = pPlayerInventory.player.getItemInHand(pHand);
        this.wandLevel = pPlayerInventory.player.level();

        this.actualPlayerInventory = pPlayerInventory;
        this.playerInventoryWrapper = new InvWrapper(pPlayerInventory);

        WandMode currentMode = this.wandItem.getOrDefault(ModDataComponents.WAND_MODE.get(), WandMode.SINGLE);
        this.initialModeIndex = currentMode.ordinal();

        BlockReferenceComponent component = this.wandItem.get(ModDataComponents.WAND_BLOCK.get());

        ItemStack storedStack = ItemStack.EMPTY;

        if (component != null) {
            storedStack = component.blockStack();
        }

        if (!storedStack.isEmpty()) {
            this.wandSlotHandler.setStackInSlot(0, storedStack.copyWithCount(1));
        }

        this.addSlot(new WandBlockSlot(wandSlotHandler, 0, WAND_SLOT_X, WAND_SLOT_Y));

        layoutPlayerInventory(pPlayerInventory);
    }

    public int getInitialModeIndex() { return initialModeIndex; }

    public InteractionHand getWandHand() { return wandHand; }

    public ItemStack getWandItem() { return wandItem; }

    @Override
    public boolean stillValid(Player pPlayer) {
        return pPlayer.getItemInHand(this.wandHand) == this.wandItem;
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
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }

    private void layoutPlayerInventory(Inventory pPlayerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(pPlayerInventory, col + row * 9 + 9, INVENTORY_START_X + col * 18, INVENTORY_START_Y + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(pPlayerInventory, col, 8 + col * 18, HOTBAR_START_Y));
        }
    }
}
