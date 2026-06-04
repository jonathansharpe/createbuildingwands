package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen.ByteCornerData.ByteCopycatCorner;

import com.simibubi.create.AllBlocks;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.checkerframework.checker.units.qual.C;
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlockEntity;

public class ByteConfigMenu extends AbstractContainerMenu {
    private final InteractionHand wandHand;
    private final ItemStack wandItem;
    // private final ItemStackHandler byteSlotHandler;
    private final Player player;

    public static final int BTN_WIDTH = 95;
    public static final int BTN_HEIGHT = 20;
    public static final int SLOT_SIZE = 18;
    public static final int H_SPACING = 12;
    public static final int V_SPACING = 6;

    public static final int PANEL_INNER_X = 12;
    public static final int PANEL_INNER_Y = 18;

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
            HolderLookup.Provider registries = this.player.level().registryAccess();

            if (!this.wandItem.has(DataComponents.CUSTOM_DATA) || this.wandItem.get(DataComponents.CUSTOM_DATA) == null) {
                this.wandItem.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            }

            CustomData customData = this.wandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag initialRoot = customData.copyTag();

            if (!initialRoot.contains("material_data", Tag.TAG_COMPOUND)) {
                for (ByteCopycatCorner corner : ByteCopycatCorner.values()) {
                    this.saveSlotMaterialToWand(corner, ItemStack.EMPTY);
                }
            }

            CustomData updatedCustomData = this.wandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag rootTag = updatedCustomData.copyTag();
            CompoundTag materialData = rootTag.getCompound("material_data");

            for (ByteCopycatCorner corner : ByteCopycatCorner.values()) {
                int slotIndex = corner.ordinal();
                CompoundTag cornerTag = materialData.getCompound(corner.getNbtKey());
                ItemStack setStack = ItemStack.EMPTY;

                if (cornerTag.contains("Item", Tag.TAG_COMPOUND)) {
                    setStack = ItemStack.parseOptional(registries, cornerTag.getCompound("Item"));
                }

                byteSlotHandler.setStackInSlot(slotIndex, setStack);
            }
             CustomData.update(
                     DataComponents.CUSTOM_DATA,
                     this.wandItem,
                     tag -> {
                         if (!tag.contains("active_corners", Tag.TAG_INT_ARRAY)) {
                             tag.putIntArray("active_corners", new int[] {0,0,0,0,0,0,0,0});
                         }
                     }
             );
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
            // get the item of whatever is stored in the slot
            ItemStack storedStack = getStackInSlot(slot);

            // get the corner based on the slot given
            ByteCopycatCorner corner = ByteCopycatCorner.values()[slot];

            // save the new material to that corner
            ByteConfigMenu.this.saveSlotMaterialToWand(corner, storedStack);

            CreateBuildingWands.LOGGER.info("[WandDebug ByteConfigMenu onContentsChanged] contents have been changed, slot is {}", slot);

            ByteConfigMenu.this.broadcastChanges();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
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
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
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
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // checking to make sure slot is in range
        if (slotId >= 0 && slotId < 8) {

            // get the slot
            Slot targetSlot = this.slots.get(slotId);
            // get the stack that the player is carrying
            ItemStack carriedStack = player.containerMenu.getCarried();
            // get the corner for the respective slot
            ByteCopycatCorner corner = ByteCopycatCorner.values()[slotId];

            // if the player is holding something, AND its a blockitem
            if (!carriedStack.isEmpty() && carriedStack.getItem() instanceof BlockItem) {
                // if its any of these click attempts, dont do it
                if (clickType == ClickType.THROW || clickType == ClickType.CLONE || clickType == ClickType.SWAP) {
                    CreateBuildingWands.LOGGER.info("[WandDebug ByteConfigMenu clicked] incorrect click type, rejecting");
                    super.clicked(slotId, button, clickType, player);
                    return;
                }
                // use insertItem to insert the item
                ItemStack configStack = byteSlotHandler.insertItem(slotId, carriedStack, false);
                targetSlot.setChanged();

                CreateBuildingWands.LOGGER.info("[WandDebug ByteConfigMenu clicked] slot now contains: {}", configStack.getHoverName().getString());
                return;
            } else if (!targetSlot.getItem().isEmpty()) {
                byteSlotHandler.extractItem(slotId, 1, false);
                targetSlot.setChanged();
                return;
            }
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
    public ItemStack quickMoveStack(Player player, int index) {
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

    public void handleServerToggle(ByteCopycatCorner corner) {
        CreateBuildingWands.LOGGER.info("[WandDebug handleServerToggle] toggling the corner {} on the server", corner);
        if (!this.wandItem.isEmpty()) {
            CustomData.update(
                    DataComponents.CUSTOM_DATA,
                    this.wandItem,
                    tag -> {
                        int[] activeCorners = tag.getIntArray("active_corners");
                        if (activeCorners.length != 8) {
                            activeCorners = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
                        }

                        int index = corner.ordinal();
                        activeCorners[index] = (activeCorners[index] == 1) ? 0 : 1;

                        tag.putIntArray("active_corners", activeCorners);
                        CompoundTag materialData = tag.getCompound("material_data");
                        CompoundTag cornerTag = materialData.getCompound(corner.getNbtKey());
                        cornerTag.putByte("enableCT", (byte) 0);

                        CreateBuildingWands.LOGGER.info("[WandDebug handleServerToggle] tag value is: {}", tag);
                    });
        }

        CreateBuildingWands.LOGGER.info("[WandDebug handleServerToggle] the custom data inside the wand is {}", this.wandItem.get(DataComponents.CUSTOM_DATA));
        this.broadcastChanges();
        CreateBuildingWands.LOGGER.info("[WandDebug handleServerToggle] this.broadcastChanges() was just called, the wand should have updated");
    }

    @Override
    public boolean stillValid(Player player) {
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
    public MenuType<?> getType() {
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
     * @param materialStack the item stack representing the block to use as a material texture
     */
    private void saveSlotMaterialToWand(ByteCopycatCorner corner, ItemStack materialStack) {
        if (!this.wandItem.isEmpty() && this.player != null) {

            HolderLookup.Provider registries = this.player.level().registryAccess();
            CustomData.update(DataComponents.CUSTOM_DATA, this.wandItem, tag -> {
                if (!tag.contains("material_data", Tag.TAG_COMPOUND)) {
                    tag.put("material_data", new CompoundTag());
                }
                CompoundTag materialData = tag.getCompound("material_data");

                BlockState targetMaterialState;
                ItemStack targetConsumedStack;
                boolean ctEnabled;

                if (materialStack.isEmpty()) {
                    targetMaterialState = AllBlocks.COPYCAT_BASE.getDefaultState();
                    targetConsumedStack = ItemStack.EMPTY;
                    ctEnabled = false;
                } else if (materialStack.getItem() instanceof BlockItem blockItem) {
                    targetMaterialState = blockItem.getBlock().defaultBlockState();
                    targetConsumedStack = new ItemStack(materialStack.getItem(), 1);
                    ctEnabled = true;
                } else {
                    return;
                }
                CompoundTag cornerTag = new CompoundTag();

                ICopycatBlockEntity.write(cornerTag, targetConsumedStack, targetMaterialState, registries, ctEnabled);

                materialData.put(corner.getNbtKey(), cornerTag);

                tag.put("material_data", materialData);
            });
        }
        this.broadcastChanges();
    }
}