package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

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

import com.avgusrname.createbuildingwands.component.BlockReferenceComponent;
import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.avgusrname.createbuildingwands.item.custom.WandMode;
import com.avgusrname.createbuildingwands.item.custom.andesiteWand.AndesiteWandItem;

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
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // debugging statements
        System.out.println("--- CLICK START ---");
        System.out.println("Clicked Slot ID: " + slotId);
        System.out.println("Clicked Cursor Stack: " + player.containerMenu.getCarried());

        if (slotId == 0) {
            // gets the wand slot
            Slot wandSlot = this.slots.get(0);
            // gets what the player is carrying
            ItemStack cursorStack = player.containerMenu.getCarried();

            System.out.println("    -> Target is WAND SLOT (ID 0)");

            if (!cursorStack.isEmpty() && (cursorStack.getItem() instanceof BlockItem)) {
                if (clickType == ClickType.THROW || clickType == ClickType.CLONE || clickType == ClickType.SWAP) {
                    System.out.println("    -> Insertion attempt rejected for specified click type (" + clickType + "). Delegating.");
                    super.clicked(slotId, button, clickType, player);
                    return;
                }
                else {
                    ItemStack remainingStack = wandSlotHandler.insertItem(0, cursorStack, false);

                    // player.containerMenu.setCarried(remainingStack);
                    wandSlot.setChanged();
                    
                    System.out.println("    -> MANUAL INSERTION COMPLETE. Remaining on cursor: " + remainingStack.getCount());
                    System.out.println("--- CLICK END (MANUAL INSERT) ---");
                    return;
                }
            }
            else if (cursorStack.isEmpty() && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) && !wandSlot.getItem().isEmpty()) {
                wandSlotHandler.extractItem(0, 1, false);
                wandSlot.setChanged();

                System.out.println("    -> MANUAL EXTRACTION/CLEAR COMPLETE.");
                System.out.println("--- CLICK END (MANUAL EXTRACT) ---");
                return;
            }
            else {
                System.out.println("    -> UNHANDLED WAND SLOT CLICK. Delegating.");
                super.clicked(slotId, button, clickType, player);
                System.out.println("--- CLICK END (DELEGATE) ---");
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
        System.out.println("--- CLICK END (DEFAULT) ---");
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
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            originalStack = slotStack.copy();

            final int WAND_SLOT_START = 0;
            final int WAND_SLOT_END = 1;
            final int PLAYER_INV_START = 1;
            final int PLAYER_INV_END = PLAYER_INV_START + 36;

            Slot wandReferenceSlot = this.slots.get(WAND_SLOT_START);

            // shift clicking from wand slot to inventory
            // will clear the slot
            if (pIndex >= WAND_SLOT_START && pIndex < WAND_SLOT_END) {
                if (!wandReferenceSlot.getItem().isEmpty()) {
                    this.wandSlotHandler.extractItem(0, 1, false);
                    wandReferenceSlot.setChanged();
                }

                return ItemStack.EMPTY;
            }
            else if (pIndex >= PLAYER_INV_START && pIndex < PLAYER_INV_END) {
                if (!(slotStack.getItem() instanceof BlockItem)) {
                    return ItemStack.EMPTY;
                }
                ItemStack stackToMove = slotStack.copyWithCount(1);
                this.wandSlotHandler.insertItem(0, stackToMove, false);

                if (wandReferenceSlot.getItem().isEmpty()) {
                    return originalStack;
                }

                slot.setChanged();
                wandReferenceSlot.setChanged();
                return ItemStack.EMPTY;
            }

            if (slot.getItem().isEmpty()) {
                slot.set(ItemStack.EMPTY);
            }
            else {
                slot.setChanged();
            }

            if (slot.getItem().getCount() == originalStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(pPlayer, slot.getItem());
        }

        return originalStack;
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
