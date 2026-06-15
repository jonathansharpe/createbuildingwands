package com.avgusrname.createbuildingwands.item.andesiteWand.screen;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.component.ModDataComponents;

import com.copycatsplus.copycats.CCBlocks;
import com.copycatsplus.copycats.content.copycat.bytes.CopycatByteBlock;
import com.copycatsplus.copycats.foundation.copycat.multistate.MaterialItemStorage;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.datafix.fixes.ItemStackTagFix;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ByteConfigMenu extends AbstractContainerMenu {
    private final InteractionHand wandHand;
    private final ItemStack wandItem;
    private final Player player;

    public static final int BTN_WIDTH = 95;
    public static final int BTN_HEIGHT = 20;
    public static final int SLOT_SIZE = 18;
    public static final int H_SPACING = 12;
    public static final int V_SPACING = 6;

    public static final int PANEL_INNER_X = 12;
    public static final int PANEL_INNER_Y = 18;

    public static final List<String> ORDERED_KEYS = List.of(
            CopycatByteBlock.BOTTOM_NW.getName(),
            CopycatByteBlock.BOTTOM_NE.getName(),
            CopycatByteBlock.BOTTOM_SW.getName(),
            CopycatByteBlock.BOTTOM_SE.getName(),
            CopycatByteBlock.TOP_NW.getName(),
            CopycatByteBlock.TOP_NE.getName(),
            CopycatByteBlock.TOP_SW.getName(),
            CopycatByteBlock.TOP_SE.getName()
    );

    public ByteConfigMenu(int pContainerId, Inventory pPlayerInventory, InteractionHand pHand) {
        super(ModMenuTypes.BYTE_CONFIG_MENU.get(), pContainerId);
        this.wandHand = pHand;
        this.wandItem = pPlayerInventory.player.getItemInHand(pHand);
        this.player = pPlayerInventory.player;

        // registers the container slots
        for (int i = 0; i < 8; i++) {
            int[] coords = getComponentCoordinates(i);
            int slotX = coords[0] + BTN_WIDTH + 4;
            int slotY = coords[1] + 1;
            this.addSlot(new SlotItemHandler(byteSlotHandler, i, slotX, slotY));
        }

        if (!this.wandItem.isEmpty()) {
            WandMaterialComponent materialComponent = this.wandItem.getOrDefault(
                    ModDataComponents.WAND_MATERIALS.get(),
                    WandMaterialComponent.createEmpty()
            );

            if (!this.wandItem.has(ModDataComponents.WAND_MATERIALS.get())) {
                this.wandItem.set(ModDataComponents.WAND_MATERIALS.get(), materialComponent);
            }

            MaterialItemStorage storage = materialComponent.toStorage(player.level().registryAccess());

            for (int i = 0; i < ORDERED_KEYS.size(); i++) {
                MaterialItemStorage.MaterialItem materialItem = storage.getMaterialItem(ORDERED_KEYS.get(i));
                if (materialItem == null) continue;

                Item itemToStore = materialItem.consumedItem().getItem();
                // TODO may need to adjust this statement
                if (itemToStore == Items.AIR || materialItem.consumedItem().isEmpty()) continue;

                byteSlotHandler.setStackInSlot(i, materialItem.consumedItem().copy());
            }

            this.broadcastChanges();
        }

        int invTopY = 132;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(pPlayerInventory, col + row * 9 + 9, col * 18, invTopY + row * 18));
            }
        }

        int hotbarTopY = 190;
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(pPlayerInventory, col, 8 + col * 18, hotbarTopY));
        }
    }

    private final ItemStackHandler byteSlotHandler = new ItemStackHandler(8) {
        @Override
        protected void onContentsChanged(int slot) {
            ItemStack storedStack = getStackInSlot(slot);
            String propertyKey = ORDERED_KEYS.get(slot);

            ByteConfigMenu.this.saveSlotMaterialToWand(propertyKey, storedStack);

            ByteConfigMenu.this.broadcastChanges();
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            CreateBuildingWands.LOGGER.info("[WandDebug byteSlotHandler insertItem] about to insert an item");

            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                return ItemStack.EMPTY;
            } else if (!this.isItemValid(slot, stack)) {
                return stack;
            } else {
                this.validateSlotIndex(slot);
                if (!simulate) {
                    this.stacks.set(slot, stack.copyWithCount(1));
                }
                this.onContentsChanged(slot);
                return stack.copyWithCount(1);
            }
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            CreateBuildingWands.LOGGER.info("[WandDebug byteSlotHandler insertItem] about to remove an item");

            if (!this.getStackInSlot(slot).isEmpty()) {
                if (!simulate) {
                    this.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
            return ItemStack.EMPTY;
        }
    };

    public static int[] getComponentCoordinates(int index) {
        int row = index % 4;
        int col = index / 4;

        int columnWidth = BTN_WIDTH + 4 + SLOT_SIZE + H_SPACING;

        int x = PANEL_INNER_X + (col * columnWidth);
        int y = PANEL_INNER_Y + (row * (BTN_HEIGHT + V_SPACING));

        return new int[]{x, y};
    }

    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        if (slotId >= 0 && slotId < 8) {
            Slot targetslot = this.slots.get(slotId);
            ItemStack carriedStack = this.getCarried();
            String propertyKey = ORDERED_KEYS.get(slotId);

            if (!carriedStack.isEmpty() && carriedStack.getItem() instanceof BlockItem blockItem) {
                if (clickType == ClickType.CLONE || clickType == ClickType.SWAP || clickType == ClickType.THROW) {
                    super.clicked(slotId, button, clickType, player);
                    return;
                }

                ItemStack ghostCopy = carriedStack.copyWithCount(1);

                byteSlotHandler.setStackInSlot(slotId, ghostCopy);
                targetslot.setChanged();
                this.saveSlotMaterialToWand(propertyKey, ghostCopy);

                return;
            } else if (carriedStack.isEmpty() && !targetslot.getItem().isEmpty()) {
                byteSlotHandler.setStackInSlot(slotId, ItemStack.EMPTY);
                targetslot.setChanged();

                this.saveSlotMaterialToWand(propertyKey, ItemStack.EMPTY);

                return;
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    // TODO fix the below method

    /**
     * what happens when a player shift clicks an item
     * @param player
     * @param index
     * @return
     */
    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (index >= 8) {
            Slot sourceSlot = this.slots.get(index);
            if (sourceSlot.hasItem()) {
                ItemStack stackInSource = sourceSlot.getItem();

                if (stackInSource.getItem() instanceof BlockItem) {
                    for (int i = 0; i < 8; i++) {
                        if (this.byteSlotHandler.getStackInSlot(i).isEmpty()) {
                            this.byteSlotHandler.setStackInSlot(i, stackInSource.copyWithCount(1));
                            this.broadcastChanges();
                            break;
                        }
                    }
                }
            } else {
                this.byteSlotHandler.setStackInSlot(index, ItemStack.EMPTY);
                this.broadcastChanges();
            }
        }
        return ItemStack.EMPTY;
    }

    public void handleServerToggle(int slotId) {
        if (!this.wandItem.isEmpty()) {

            String propertyKey = ORDERED_KEYS.get(slotId);

            WandMaterialComponent current = this.wandItem.getOrDefault(
                    ModDataComponents.WAND_MATERIALS.get(),
                    WandMaterialComponent.createEmpty()
            );

            BooleanProperty prop = CopycatByteBlock.byByte(CopycatByteBlock.byteMap.get(propertyKey));
            boolean currentlyActive = current.cornerState().getValue(prop);

            WandMaterialComponent updated = current.withCornerActive(propertyKey, !currentlyActive);

            this.wandItem.set(ModDataComponents.WAND_MATERIALS.get(), updated);

            this.broadcastChanges();
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    public InteractionHand getWandHand() {
        return wandHand;
    }

    public ItemStack getWandItem() {
        return wandItem;
    }

    public static MenuType<ByteConfigMenu> getTypeReference() {
        return ModMenuTypes.BYTE_CONFIG_MENU.get();
    }

    @Override
    public @NotNull MenuType<?> getType() {
        return ModMenuTypes.BYTE_CONFIG_MENU.get();
    }

    public ByteConfigMenu(int pContainerId, Inventory pPlayerInventory, FriendlyByteBuf pExtraData) {
        this(pContainerId, pPlayerInventory, deserializeHand(pExtraData));
    }

    private static InteractionHand deserializeHand(FriendlyByteBuf pExtraData) {
        if (pExtraData != null) {
            return pExtraData.readEnum(InteractionHand.class);
        }
        return InteractionHand.MAIN_HAND;
    }

    /**
     * this method will apply a material to the given corner of the copycat byte
     * @param corner the corner to modify the data of
     * @param newConsumedItem the item stack representing the block to use as a material texture
     */
    private void saveSlotMaterialToWand(String propertyKey, ItemStack newConsumedItem) {
        if (this.wandItem.isEmpty()) return;

        WandMaterialComponent current = wandItem.getOrDefault(
                ModDataComponents.WAND_MATERIALS.get(),
                WandMaterialComponent.createEmpty()
        );

        HolderLookup.Provider registries = player.level().registryAccess();
        MaterialItemStorage storage = current.toStorage(registries);

        BlockState newMaterial = AllBlocks.COPYCAT_BASE.getDefaultState();
        if (!newConsumedItem.isEmpty() && newConsumedItem.getItem() instanceof BlockItem blockItem) {
            newMaterial = blockItem.getBlock().defaultBlockState();
        }

        storage.storeMaterialItem(propertyKey, new MaterialItemStorage.MaterialItem(newMaterial, newConsumedItem.isEmpty() ? ItemStack.EMPTY : newConsumedItem.copyWithCount(1)));

        BlockState newCornerState = current.cornerState();

        this.wandItem.set(ModDataComponents.WAND_MATERIALS.get(),
                WandMaterialComponent.fromStorage(storage, newCornerState, registries));

        this.broadcastChanges();
    }
}