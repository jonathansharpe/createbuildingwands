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
import net.minecraft.references.Items;

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
                return ItemStack.EMPTY;
            }
            else if (!this.isItemValid(slot, stack)) {
                return stack;
            }
            // this else triggers once we've established that the item that the player is attempting to insert is not empty, is a BlockItem, and the item itself is valid
            else {
                // makes sure the slot is actually valid
                this.validateSlotIndex(slot);
                // sets the limit of the slot, is set to 1 in the WandBlockSlot class
                int limit = this.getStackLimit(slot, stack);
                // if the existing stack is not empty
                if (limit <= 0) {
                    return stack;
                }
                else {
                    boolean reachedLimit = stack.getCount() > limit;
                    if (!simulate) {
                        this.stacks.set(slot, reachedLimit ? stack.copyWithCount(limit) : stack);
                    }
                    this.onContentsChanged(slot);
                    return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
                }
            }
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

    // this makes the menu n stuff
    public WandConfigMenu(int pContainerId, Inventory pPlayerInventory, InteractionHand pHand) {
        // calls the parent constructor to make a menu given this information
        super(ModMenuTypes.WAND_CONFIG_MENU.get(), pContainerId);

        // sets some important stuff passed through the parameters
        this.wandHand = pHand;
        this.wandItem = pPlayerInventory.player.getItemInHand(pHand);
        this.wandLevel = pPlayerInventory.player.level();

        this.actualPlayerInventory = pPlayerInventory;
        this.playerInventoryWrapper = new InvWrapper(pPlayerInventory);

        // this will set the wand mode in the menu to be what it is from the data components, or set a default value of SINGLE if there is none (like when the wand is used for the first time)
        WandMode currentMode = this.wandItem.getOrDefault(ModDataComponents.WAND_MODE.get(), WandMode.SINGLE);
        // gets a numerical value for the mode selected. for example, if SINGLE is selected, and its the first mode, it'd return 0
        this.initialModeIndex = currentMode.ordinal();

        // the data for the block stored in the wand is fetched here and tells the menu what it is
        BlockReferenceComponent component = this.wandItem.get(ModDataComponents.WAND_BLOCK.get());

        // the block inside the wand, set to empty by default i guess
        ItemStack storedStack = ItemStack.EMPTY;

        // if there IS a stored block, it'll apply that to the slot: i.e. the block with stack of 1; could also be empty 
        if (component != null) {
            storedStack = component.blockStack();
        }

        // if the stack is not empty or invalid or whatever, set the stack to be the block there
        if (!storedStack.isEmpty()) {
            this.wandSlotHandler.setStackInSlot(0, storedStack.copyWithCount(1));
        }

        // add the slot to the menu
        this.addSlot(new WandBlockSlot(wandSlotHandler, 0, WAND_SLOT_X, WAND_SLOT_Y));

        // this just draws the inventory on the screen below the wand menu. will need to revise this eventually
        layoutPlayerInventory(pPlayerInventory);
    }

    // TODO: you need to overwrite the clicked() method for more debugging, as the insertItem() method is not being called

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
