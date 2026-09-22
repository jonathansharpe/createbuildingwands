package com.avgusrname.createbuildingwands.item.andesiteWand.screen;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlock;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
import com.avgusrname.createbuildingwands.util.WandUtils;

import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public class WandConfigMenu extends AbstractContainerMenu{

    private final InteractionHand wandHand;
    private final ItemStack wandItem;
    private final Player player;

    private final int initialModeIndex;

    public static final int REGULAR_WAND_SLOT_X = 134;
    public static final int REGULAR_WAND_SLOT_Y = 16;
    public static final int COPYCAT_WAND_SLOT_X = 153;
    public static final int COPYCAT_WAND_SLOT_Y = 16;
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
        this.player = pPlayerInventory.player;

        // this will set the wand mode in the menu to be what it is from the data components, or set a default value of SINGLE if there is none (like when the wand is used for the first time)
        WandMode currentMode = this.wandItem.getOrDefault(ModDataComponents.WAND_MODE.get(), WandMode.SINGLE);
        // gets a numerical value for the mode selected. for example, if SINGLE is selected, and its the first mode, it'd return 0
        this.initialModeIndex = currentMode.ordinal();

        // the data for the block stored in the wand is fetched here and tells the menu what it is
        Block regularBlock = this.wandItem.get(ModDataComponents.WAND_BLOCK_REGULAR.get());

        // the block inside the wand, set to empty by default i guess
        ItemStack storedStack = ItemStack.EMPTY;

        // if there IS a stored block, it'll apply that to the slot: i.e. the block with stack of 1; could also be empty 
        if (regularBlock != null) {
            storedStack = new ItemStack(regularBlock.asItem());
        }

        // if the stack is not empty or invalid or whatever, set the stack to be the block there
        if (!storedStack.isEmpty()) {
            this.regularSlotHandler.setStackInSlot(0, storedStack.copyWithCount(1));
        }

        // add the slot to the menu
        this.addSlot(new WandBlockSlot(regularSlotHandler, 0, REGULAR_WAND_SLOT_X, REGULAR_WAND_SLOT_Y));

        Block copycatBlock = this.wandItem.get(ModDataComponents.WAND_BLOCK_COPYCAT.get());
        ItemStack copycatStoredStack = ItemStack.EMPTY;

        if (copycatBlock != null) {
            copycatStoredStack = new ItemStack(copycatBlock.asItem());
        }

        if (!copycatStoredStack.isEmpty()) {
            this.copycatSlotHandler.setStackInSlot(0, copycatStoredStack.copyWithCount(1));
        }

        this.addSlot(new WandBlockSlot(copycatSlotHandler, 0, COPYCAT_WAND_SLOT_X, COPYCAT_WAND_SLOT_Y));

        // this just draws the inventory on the screen below the wand menu. will need to revise this eventually
        layoutPlayerInventory(pPlayerInventory);
    }

    private final ItemStackHandler regularSlotHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            ItemStack storedStack = getStackInSlot(slot);

            if (storedStack.isEmpty()) {
                wandItem.remove(ModDataComponents.WAND_BLOCK_REGULAR.get());
            }
            else {
                if (storedStack.getItem() instanceof BlockItem storedBlock) {
                    wandItem.set(ModDataComponents.WAND_BLOCK_REGULAR.get(), storedBlock.getBlock());
                }
            }

            WandConfigMenu.this.broadcastChanges();
        }

        /**
         * determines if the item about to be inserted is actually a valid item for the slot. THIS IS EXTREMELY IMPORTANT FOR FILTERING OUT VALID/INVALID ITEMS. this one rejects any copycat blocks, or any non-full blocks if a copycat block already exists in the copycat slot, and also if its just not a block
         * @param slot the slot that the item will check insertion for
         * @param stack the stack to attempt insertion for
         * @return true if item can be inserted, false if not
         */
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
            Block block = blockItem.getBlock();
            if (block instanceof ICopycatBlock || block instanceof CopycatBlock) return false;

            if (wandItem.has(ModDataComponents.WAND_BLOCK_COPYCAT.get()) && !WandUtils.isFullBlock(block)) {
                player.displayClientMessage(Component.literal("Copycat blocks require a full block material").withStyle(ChatFormatting.RED), true);
                return false;
            }
            return true;
        }

        /**
         * inserts an item into a slot. the override is necessary so the item is not consumed when inserted, merely that the data is copied so the wand knows what blocks to place. the items will be consumed upon placing.
         * @param slot the slot insertion is attempted in
         * @param stack the stack to attempt insertion with
         * @param simulate if we're simulating insertion or not (useful for testing i'm guestting)
         * @return the stack that is inserted; empty if it fails, the stack in the parameter if it succeeds
         */
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

        /**
         * clears the slot of any items in it. again no actual items are moved between the player and wand, it just clears the stored data
         * @param slot the slot to extract the item from
         * @param amount the amount to extract, even though i dont think its actually used it needs to exist to override a super class method
         * @param simulate simulating extraction or not
         * @return will always be empty because nothing is given back to the player since nothing was taken in the first place
         */
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!this.getStackInSlot(slot).isEmpty()) {
                if (!simulate) {
                    this.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
            return ItemStack.EMPTY;
        }
    };

    private final ItemStackHandler copycatSlotHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            ItemStack storedStack = getStackInSlot(slot);

            if (storedStack.isEmpty()) {
                wandItem.remove(ModDataComponents.WAND_BLOCK_COPYCAT.get());
            } else {
                if (storedStack.getItem() instanceof BlockItem storedBlock) {
                    wandItem.set(ModDataComponents.WAND_BLOCK_COPYCAT.get(), storedBlock.getBlock());
                }
            }

            WandConfigMenu.this.broadcastChanges();
        }

        /**
         *  checks if the given item is a valid copycat item, for use before insertion. currently rejects any non-copycat block
         * @param slot the slot to attempt insertion into
         * @param stack the stack to check
         * @return true if the item is valid, false if it isn't
         */
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
            Block block = blockItem.getBlock();
            if (!(block instanceof ICopycatBlock || block instanceof CopycatBlock)) return false;

            ItemStack regularWandStack = regularSlotHandler.getStackInSlot(0);
            if (!regularWandStack.isEmpty() && regularWandStack.getItem() instanceof BlockItem regularBlockItem && !WandUtils.isFullBlock(regularBlockItem.getBlock())) {
                player.displayClientMessage(Component.literal("Cannot use copycat block with non-full block.").withStyle(ChatFormatting.RED), true);
                return false;
            }
            return true;
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

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            CreateBuildingWands.LOGGER.info("Copycat Slot extractItem() called: Clearing reference slot.");

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
                ItemStack configStack = regularSlotHandler.insertItem(slotId, carriedStack, false);
                wandSlot.setChanged();

                System.out.println("    -> slot now contains: " + configStack.getHoverName().getString());
                return;
            }
            // due to the above if statement we already know that carriedStack is empty, so we can just extract the item from the slot
            else if (!wandSlot.getItem().isEmpty()) {
                regularSlotHandler.extractItem(slotId, 1, false);
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

    public ItemStack getWandItem() {
        return wandItem;
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
        Slot slot = this.slots.get(pIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack slotStack = slot.getItem();
        ItemStack originalStack = slotStack.copy();

        final int REGULAR_WAND_SLOT = 0;
        final int COPYCAT_WAND_SLOT = 1;
        final int PLAYER_INV_START = 2;
        final int PLAYER_INV_END = PLAYER_INV_START + 36;

        if (pIndex == REGULAR_WAND_SLOT) {
            this.regularSlotHandler.extractItem(0, 1, false);
            this.slots.get(REGULAR_WAND_SLOT).setChanged();
            return ItemStack.EMPTY;
        }
        if (pIndex == COPYCAT_WAND_SLOT) {
            this.copycatSlotHandler.extractItem(0, 1, false);
            this.slots.get(COPYCAT_WAND_SLOT).setChanged();
            return ItemStack.EMPTY;
        }

        if (pIndex >= PLAYER_INV_START && pIndex < PLAYER_INV_END) {
            if (!(slotStack.getItem() instanceof BlockItem)) return ItemStack.EMPTY;

            ItemStack stackToMove = slotStack.copyWithCount(1);

            if (this.copycatSlotHandler.isItemValid(0, stackToMove)) {
                if (this.slots.get(COPYCAT_WAND_SLOT).getItem().isEmpty()) {
                    this.copycatSlotHandler.insertItem(0, stackToMove, false);
                    this.broadcastChanges();
                }
            } else {
                if (this.slots.get(REGULAR_WAND_SLOT).getItem().isEmpty()) {
                    this.regularSlotHandler.insertItem(0, stackToMove, false);
                    this.broadcastChanges();
                }
            }
        }
        return ItemStack.EMPTY;
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
