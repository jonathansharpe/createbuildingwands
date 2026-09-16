package com.avgusrname.createbuildingwands.item.andesiteWand.screen;

import com.copycatsplus.copycats.foundation.copycat.ICopycatBlock;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.Block;

import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.avgusrname.createbuildingwands.item.WandMode;

import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public class WandConfigMenu extends AbstractContainerMenu{

    private final InteractionHand wandHand;
    private final ItemStack wandItem;

    private final int initialModeIndex;

    public static final int WAND_SLOT_X = 134;
    public static final int WAND_SLOT_Y = 16;
    public static final int COPYCAT_SLOT_X = 153;
    public static final int COPYCAT_SLOT_Y = 16;
    public static final int INVENTORY_START_X = 8;
    public static final int INVENTORY_START_Y = 60;
    public static final int HOTBAR_START_Y = 118;

    /**
     * creates the container that holds the info for the wand config menu thats displayed to the player
     * @param pContainerId container id to track the container among different wand instances
     * @param pPlayerInventory the inventory that contains the wand thats opened
     * @param pHand hand thats holding the wand
     */
    // this makes the menu n stuff
    public WandConfigMenu(int pContainerId, Inventory pPlayerInventory, InteractionHand pHand) {
        // calls the parent constructor to make a menu given this information
        super(ModMenuTypes.WAND_CONFIG_MENU.get(), pContainerId);

        // sets some important stuff passed through the parameters
        this.wandHand = pHand;
        this.wandItem = pPlayerInventory.player.getItemInHand(pHand);

        // this will set the wand mode in the menu to be what it is from the data components, or set a default value of SINGLE if there is none (like when the wand is used for the first time)
        WandMode currentMode = this.wandItem.getOrDefault(ModDataComponents.WAND_MODE.get(), WandMode.SINGLE);
        // gets a numerical value for the mode selected. for example, if SINGLE is selected, and its the first mode, it'd return 0
        this.initialModeIndex = currentMode.ordinal();

        // the data for the block stored in the wand is fetched here and tells the menu what it is
        Block regularBlock = this.wandItem.get(ModDataComponents.WAND_BLOCK.get());

        // the block inside the wand, set to empty by default i guess
        ItemStack storedStack = ItemStack.EMPTY;

        // if there IS a stored block, it'll apply that to the slot: i.e. the block with stack of 1; could also be empty 
        if (regularBlock != null) {
            storedStack = new ItemStack(regularBlock.asItem());
        }

        // if the stack is not empty or invalid or whatever, set the stack to be the block there
        if (!storedStack.isEmpty()) {
            this.wandSlotHandler.setStackInSlot(0, storedStack.copyWithCount(1));
        }

        // add the slot to the menu
        this.addSlot(new WandBlockSlot(wandSlotHandler, 0, WAND_SLOT_X, WAND_SLOT_Y));

        Block copycatBlock = this.wandItem.get(ModDataComponents.WAND_COPYCAT_BLOCK.get());
        ItemStack copycatStoredStack = ItemStack.EMPTY;

        if (copycatBlock != null) {
            copycatStoredStack = new ItemStack(copycatBlock.asItem());
        }

        if (!copycatStoredStack.isEmpty()) {
            this.copycatSlotHandler.setStackInSlot(0, copycatStoredStack.copyWithCount(1));
        }

        this.addSlot(new WandBlockSlot(copycatSlotHandler, 0, COPYCAT_SLOT_X, COPYCAT_SLOT_Y));

        // this just draws the inventory on the screen below the wand menu. will need to revise this eventually
        layoutPlayerInventory(pPlayerInventory);
    }

    private final ItemStackHandler wandSlotHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            ItemStack storedStack = getStackInSlot(slot);

            if (storedStack.isEmpty()) {
                wandItem.remove(ModDataComponents.WAND_BLOCK.get());
            }
            else {
                if (storedStack.getItem() instanceof BlockItem storedBlock) {
                    wandItem.set(ModDataComponents.WAND_BLOCK.get(), storedBlock.getBlock());
                }
            }

            WandConfigMenu.this.broadcastChanges();
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {

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
                // if the existing stack is not empty
                if (!simulate) {
                    // i think this should just copy the stack from the cursor with quantity of limit (which is 1)
                    this.stacks.set(slot, stack.copyWithCount(1));
                }
                // will change the contents
                this.onContentsChanged(slot);
                // idk if this is correct but we'll see
                return stack.copyWithCount(1);
            }
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {

            System.out.println("Wand Slot extractItem() called: Clearing reference slot.");

            if (!this.getStackInSlot(slot).isEmpty()) {
                if (!simulate) {
                    this.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
            return ItemStack.EMPTY;
        }
    };

    private boolean isUpdating = false;
    private final ItemStackHandler copycatSlotHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            if (isUpdating) return;
            isUpdating = true;

            try {
                ItemStack storedStack = getStackInSlot(0);
                if (storedStack.isEmpty()) {
                    wandItem.remove(ModDataComponents.WAND_COPYCAT_BLOCK.get());
                } else if (storedStack.getItem() instanceof BlockItem copycatItem) {
                    wandItem.set(ModDataComponents.WAND_COPYCAT_BLOCK.get(), copycatItem.getBlock());
                }

                WandConfigMenu.this.broadcastChanges();
            } finally {
                isUpdating = true;
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
            Block block = blockItem.getBlock();
            return block instanceof ICopycatBlock || block instanceof CopycatBlock;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) return stack;
            if (!this.isItemValid(slot, stack)) return ItemStack.EMPTY;

            if (!simulate) {
                this.stacks.set(slot, stack.copyWithCount(1));
                this.onContentsChanged(slot);
            }
            return stack;
        }

        // TODO this function is not working at the moment; right or left clicking the copycat slot clears it but reverts back when the wand menu is re-opened
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            System.out.println("Copycat Slot extractItem() called: Clearing reference slot.");

            if (!this.getStackInSlot(slot).isEmpty()) {
                if (!simulate) {
                    this.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
            return ItemStack.EMPTY;
        }

    };


    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        // debugging statements

        if (slotId == 0) {
            // gets the wand slot
            Slot wandSlot = this.slots.get(slotId);
            // gets what the player is carrying
            ItemStack carriedStack = player.containerMenu.getCarried();

            System.out.println("    -> Target is WAND SLOT (ID 0)");

            if (!carriedStack.isEmpty() && carriedStack.getItem() instanceof BlockItem) {
                if (clickType == ClickType.THROW || clickType == ClickType.CLONE || clickType == ClickType.SWAP) {
                    System.out.println("    -> Insertion attempt rejected for specified click type (" + clickType + "). Delegating.");
                    super.clicked(slotId, button, clickType, player);
                    return;
                }
                ItemStack configStack = wandSlotHandler.insertItem(slotId, carriedStack, false);
                wandSlot.setChanged();

                System.out.println("    -> slot now contains: " + configStack.getHoverName().getString());
                return;
            }
            // due to the above if statement we already know that carriedStack is empty, so we can just extract the item from the slot
            else if (!wandSlot.getItem().isEmpty()) {
                // TODO THIS DOESN'T WORK FOR SOME REASON
                wandSlotHandler.extractItem(slotId, 1, false);
                wandSlot.setChanged();

                return;
            }
            super.clicked(slotId, button, clickType, player);
            return;
        }
        else if (slotId == 1) {
            // gets the copycat slot
            Slot copycatSlot = this.slots.get(slotId);
            ItemStack cursorStack = player.containerMenu.getCarried();

            System.out.println("    -> Target is COPYCAT SLOT (ID 1)");

            if (!cursorStack.isEmpty() && (cursorStack.getItem() instanceof BlockItem)) {
                if (clickType == ClickType.THROW || clickType == ClickType.CLONE || clickType == ClickType.SWAP) {
                    super.clicked(slotId, button, clickType, player);
                }
                else {
                    copycatSlotHandler.insertItem(0, cursorStack, false);
                    copycatSlot.setChanged();
                }
                return;
            }
            else if (cursorStack.isEmpty() && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) && !copycatSlot.getItem().isEmpty()) {
                copycatSlotHandler.extractItem(0, 1, false);
                copycatSlot.setChanged();
                return;
            }
            else {
                super.clicked(slotId, button, clickType, player);
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);

    }

    // getters and setters
    public int getInitialModeIndex() {
        return initialModeIndex;
    }

    public InteractionHand getWandHand() {
        return wandHand;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return pPlayer.getItemInHand(this.wandHand) == this.wandItem;
    }

    /**
     * basically gets the data from the hand that is used elsewhere in packets and stuff
     * @param pExtraData the data from the packet that is then converted to the hand
     * @return the hand that is passed for use later in another constructor
     */
    private static InteractionHand deserializeHand(FriendlyByteBuf pExtraData) {
        if (pExtraData != null) {
            return pExtraData.readEnum(InteractionHand.class);
        }
        return InteractionHand.MAIN_HAND;
    }

    public WandConfigMenu(int pContainerId, Inventory pPlayerInventory, FriendlyByteBuf pExtraData) {
        this(pContainerId, pPlayerInventory, deserializeHand(pExtraData));
    }

    /**
     * shift clicking an item into the wand menu
     * @param pPlayer the player
     * @param pIndex the slot where the block is getting shifted into
     * @return the stack to put in the slot
     */
    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player pPlayer, int pIndex) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            originalStack = slotStack.copy();

            final int WAND_SLOT_START = 0;
            final int WAND_SLOT_END = 1;
            final int COPYCAT_SLOT_START = 1;
            final int COPYCAT_SLOT_END = 2;
            final int PLAYER_INV_START = 2;
            final int PLAYER_INV_END = PLAYER_INV_START + 36;

            // shift clicking from wand slot (regular block) to inventory
            if (pIndex >= WAND_SLOT_START && pIndex < WAND_SLOT_END) {
                Slot wandReferenceSlot = this.slots.get(WAND_SLOT_START);
                if (!wandReferenceSlot.getItem().isEmpty()) {
                    this.wandSlotHandler.extractItem(0, 1, false);
                    wandReferenceSlot.setChanged();
                }
                return ItemStack.EMPTY;
            }
            // shift clicking from copycat slot to inventory
            else if (pIndex >= COPYCAT_SLOT_START && pIndex < COPYCAT_SLOT_END) {
                Slot copycatReferenceSlot = this.slots.get(COPYCAT_SLOT_START);
                if (!copycatReferenceSlot.getItem().isEmpty()) {
                    this.copycatSlotHandler.extractItem(0, 1, false);
                    copycatReferenceSlot.setChanged();
                }
                return ItemStack.EMPTY;
            }
            else if (pIndex >= PLAYER_INV_START && pIndex < PLAYER_INV_END) {
                if (!(slotStack.getItem() instanceof BlockItem)) {
                    return ItemStack.EMPTY;
                }
                ItemStack stackToMove = slotStack.copyWithCount(1);
                
                // Try to insert into copycat slot first if it's empty, otherwise regular slot
                Slot copycatSlot = this.slots.get(COPYCAT_SLOT_START);
                Slot wandSlot = this.slots.get(WAND_SLOT_START);
                
                if (copycatSlot.getItem().isEmpty()) {
                    if (this.copycatSlotHandler.isItemValid(0, stackToMove)) {
                        this.copycatSlotHandler.insertItem(0, stackToMove, false);
                        copycatSlot.setChanged();
                        return originalStack;
                    }
                    if (wandSlot.getItem().isEmpty()) {
                        this.wandSlotHandler.insertItem(0, stackToMove, false);
                        wandSlot.setChanged();
                        return originalStack;
                    }
                }
                else if (wandSlot.getItem().isEmpty()) {
                    this.wandSlotHandler.insertItem(0, stackToMove, false);
                    wandSlot.setChanged();
                    return originalStack;
                }
                else {
                    // Both slots full, don't move
                    return ItemStack.EMPTY;
                }
            }

            if (slot.getItem().getCount() == originalStack.getCount()) {
                return ItemStack.EMPTY;
            }
        }

        return originalStack;
    }

    /**
     * draws the players inventory
     * TODO theres gotta be a more automated way to do this right? see if it can be written once and imported everywhere else
     * @param pPlayerInventory the players inventory to draw
     */
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
