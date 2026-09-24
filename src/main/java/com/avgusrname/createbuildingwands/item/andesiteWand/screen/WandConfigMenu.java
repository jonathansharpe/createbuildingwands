package com.avgusrname.createbuildingwands.item.andesiteWand.screen;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.copycatsplus.copycats.content.copycat.slab.CopycatSlabBlock;
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlock;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.VillagerTrades;
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

    public WandConfigMenu(int pContainerId, Inventory pPlayerInventory, InteractionHand pHand) {
        super (ModMenuTypes.WAND_CONFIG_MENU.get(), pContainerId);

        this.wandHand = pHand;
        this.wandItem = pPlayerInventory.player.getItemInHand(pHand);
        this.player = pPlayerInventory.player;

        this.initialModeIndex = this.wandItem.getOrDefault(
                ModDataComponents.WAND_MODE.get(), WandMode.SINGLE).ordinal();

        loadBlockIntoSlot(regularSlotHandler, ModDataComponents.WAND_BLOCK_REGULAR.get());
        this.addSlot(new WandBlockSlot(regularSlotHandler, 0, REGULAR_WAND_SLOT_X, REGULAR_WAND_SLOT_Y));

        loadBlockIntoSlot(copycatSlotHandler, ModDataComponents.WAND_BLOCK_COPYCAT.get());
        this.addSlot(new WandBlockSlot(copycatSlotHandler, 0, COPYCAT_WAND_SLOT_X, COPYCAT_WAND_SLOT_Y));

        layoutPlayerInventory(pPlayerInventory);

    }

    private void loadBlockIntoSlot(WandSlotHandler handler, DataComponentType<Block> component) {
        Block block = this.wandItem.get(component);
        if (block != null) {
            handler.setStackInSlot(0, new ItemStack(block.asItem()));
        }
    }

    private final WandSlotHandler regularSlotHandler = new WandSlotHandler(
            stack -> stack.getItem() instanceof BlockItem blockItem
                    && !(blockItem.getBlock() instanceof ICopycatBlock)
                    && !(blockItem.getBlock() instanceof CopycatBlock)
                    && (!this.getWandItem().has(ModDataComponents.WAND_BLOCK_COPYCAT.get())
                    || WandUtils.isFullBlock(blockItem.getBlock())),
            this::onRegularSlotChanged
    );

    private void onRegularSlotChanged() {
        ItemStack storedStack = regularSlotHandler.getStackInSlot(0);
        if (storedStack.isEmpty()) {
            wandItem.remove(ModDataComponents.WAND_BLOCK_REGULAR.get());
        } else if (storedStack.getItem() instanceof BlockItem blockItem) {
            wandItem.set(ModDataComponents.WAND_BLOCK_REGULAR.get(), blockItem.getBlock());
        }
        broadcastChanges();
    }

    private final WandSlotHandler copycatSlotHandler = new WandSlotHandler(
            stack -> stack.getItem() instanceof BlockItem blockItem
            && (blockItem.getBlock() instanceof ICopycatBlock
            || blockItem.getBlock() instanceof CopycatBlock),
            this::onCopycatSlotChanged
    );

    private void onCopycatSlotChanged() {
        ItemStack storedStack = copycatSlotHandler.getStackInSlot(0);
        Block newBlock = storedStack.isEmpty() ? null
                : storedStack.getItem() instanceof BlockItem bi ? bi.getBlock() : null;
        Block currentBlock = wandItem.get(ModDataComponents.WAND_BLOCK_COPYCAT.get());

        if (newBlock != currentBlock) {
            if (newBlock == null) {
                wandItem.remove(ModDataComponents.WAND_BLOCK_COPYCAT.get());
            } else {
                wandItem.set(ModDataComponents.WAND_BLOCK_COPYCAT.get(), newBlock);
            }
            wandItem.set(ModDataComponents.WAND_MATERIALS.get(), WandMaterialComponent.createEmptyDefault());
        }
        broadcastChanges();
    }

    private void handleWandSlotClick(int slotId, int button, ClickType clickType, Player player) {
        Slot slot = this.slots.get(slotId);
        WandSlotHandler handler = slotId == 0 ? regularSlotHandler : copycatSlotHandler;
        ItemStack carriedStack = this.getCarried();

        if (clickType == ClickType.THROW || clickType == ClickType.CLONE || clickType == ClickType.SWAP) {
            super.clicked(slotId, button, clickType, player);
            return;
        }
        if (!carriedStack.isEmpty() && carriedStack.getItem() instanceof BlockItem) {
            handler.insertItem(0, carriedStack, false);
            slot.setChanged();
        } else if (carriedStack.isEmpty() && !slot.getItem().isEmpty()) {
            handler.extractItem(0, 1, false);
            slot.setChanged();
        }
    }

    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        if (slotId == 0 || slotId == 1) {
            handleWandSlotClick(slotId, button, clickType, player);
            return;
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

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stackToMove = slot.getItem().copyWithCount(1);

        if (index == 0 || index == 1) {
            WandSlotHandler handler = index == 0 ? regularSlotHandler : copycatSlotHandler;
            handler.extractItem(0, 1, false);
            slot.setChanged();
            return ItemStack.EMPTY;
        }

        if (!(stackToMove.getItem() instanceof BlockItem)) return ItemStack.EMPTY;

        WandSlotHandler targetHandler = copycatSlotHandler.isItemValid(0, stackToMove)
                ? copycatSlotHandler : regularSlotHandler;
        int targetSlot = copycatSlotHandler.isItemValid(0, stackToMove) ? 1 : 0;

        if (this.slots.get(targetSlot).getItem().isEmpty()) {
            targetHandler.insertItem(0, stackToMove, false);
            this.broadcastChanges();
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
