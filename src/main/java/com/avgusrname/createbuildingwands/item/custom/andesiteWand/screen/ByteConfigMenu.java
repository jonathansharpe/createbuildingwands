package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen.ByteCornerData.ByteCopycatCorner;

import net.minecraft.Util;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

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
        // this.byteSlotHandler = new ItemStackHandler(8);

        for (int i = 0; i < 8; i++) {
            int[] coords = getComponentCoordinates(i);

            int slotX = coords[0] + BTN_WIDTH + 4;
            int slotY = coords[1] + 1;

            this.addSlot(new SlotItemHandler(byteSlotHandler, i, slotX, slotY));
        }

        if (!this.wandItem.isEmpty()) {
            if (!this.wandItem.has(DataComponents.CUSTOM_DATA) || this.wandItem.get(DataComponents.CUSTOM_DATA) == null) {
                this.wandItem.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            }
            CustomData customData = this.wandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag rootTag = customData.copyTag();

            // checking if it doesn't contain material data, so we're only setting defaults for a fresh wand
            if (!rootTag.contains("material_data", Tag.TAG_COMPOUND)) {
                for (ByteCopycatCorner corner : ByteCopycatCorner.values()) {
                    this.saveSlotMaterialToWand(corner, ItemStack.EMPTY);
                }
            }

            CustomData.update(
                DataComponents.CUSTOM_DATA,
                this.wandItem,
                tag -> {
                    if (!tag.contains("active_corners", Tag.TAG_INT_ARRAY)) {
                            tag.putIntArray("active_corners", new int[] { 0, 0, 0, 0, 0, 0, 0, 0 });
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
            ItemStack storedStack = getStackInSlot(slot);

            ByteCopycatCorner corner = ByteCopycatCorner.values()[slot];

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
        if (slotId >= 0 && slotId < 8) {

            Slot targetSlot = this.slots.get(slotId);
            ItemStack carriedStack = player.containerMenu.getCarried();
            ByteCopycatCorner corner = ByteCopycatCorner.values()[slotId];

            if (carriedStack.isEmpty()) {
                targetSlot.set(ItemStack.EMPTY);
                saveSlotMaterialToWand(corner, ItemStack.EMPTY);
            } else {
                ItemStack ghostClone = carriedStack.copyWithCount(1);
                targetSlot.set(ghostClone);
                saveSlotMaterialToWand(corner, ghostClone);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= 8) {
            Slot sourceSlot = this.slots.get(index);
            if (sourceSlot != null && sourceSlot.hasItem()) {
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

    private void logWandData() {

        if (this.wandItem.has(DataComponents.CUSTOM_DATA)) {

            CompoundTag rootTag = this.wandItem.get(DataComponents.CUSTOM_DATA).copyTag();
            CompoundTag tempMaterialData = rootTag.getCompound("material_data");

            // Grab the active array safely (fallback to empty if not initialized)
            int[] activeCorners = rootTag.getIntArray("active_corners");
            if (activeCorners.length != 8) {
                activeCorners = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
            }

            CreateBuildingWands.LOGGER.info("=================== WAND COMPONENT SNAPSHOT ===================");

            for (int i = 0; i < 8; i++) {
                ByteCopycatCorner tempCorner = ByteCopycatCorner.values()[i];
                String cornerKey = tempCorner.getNbtKey();

                // Check toggle state from the bit-array
                boolean isToggledOn = activeCorners[i] == 1;
                String toggleStatus = isToggledOn ? "[ENABLED]" : "[DISABLED]";

                // Read specific material payload for this corner
                if (tempMaterialData.contains(cornerKey)) {
                    CompoundTag cornerTag = tempMaterialData.getCompound(cornerKey);
                    CompoundTag material = cornerTag.getCompound("material");
                    String blockName = material.contains("Name") ? material.getString("Name") : "NONE";

                    // Check if there are block state properties applied (like facing, half, etc.)
                    String propertiesStr = "";
                    if (material.contains("Properties")) {
                        propertiesStr = " Prms: " + material.getCompound("Properties").toString();
                    }

                    CreateBuildingWands.LOGGER.info(String.format("-> Corner %d: %-16s %-10s | Block: %-32s%s",
                            i, cornerKey, toggleStatus, blockName, propertiesStr));
                } else {
                    // Missing entirely from material_data compound map
                    CreateBuildingWands.LOGGER.info(String.format("-> Corner %d: %-16s %-10s | Block: MISSING_TAG_DATA",
                            i, cornerKey, toggleStatus));
                }
            }
            CreateBuildingWands.LOGGER.info("===============================================================");
        }
    }

    /**
     * this method will apply a material to the given corner of the copycat byte
     * @param corner the corner to modify the data of
     * @param materialStack the item stack representing the block to use as a material texture
     */
    private void saveSlotMaterialToWand(ByteCopycatCorner corner, ItemStack materialStack) {

        /*
        so like what does this method do?

        if the wand item slot is not empty (waow)
            update the custom data!
            we're gonna use a big ol lambda function to change the tag for the wand

            if the materialStack is empty
                the material should be set to "create:copycat_base"
                connected texture set to 0b
                consumedItem to :{} (i.e. nothing)
            else if materialStack is of type BlockItem
                material set to materialStack blocks name
                connected texture set to 1b (default copycats+ behavior)
                if wandItem customData consumedItem does not contain material stack
                    consumedItem to :{blockName}
                    count to :1
                else 
                    do nothing; multistate blocks only consume up to 1 block if multiple parts contain the same texture

        this.broadcastChanges()

        log wand data where necessary
         */
        CreateBuildingWands.LOGGER.info("[WandDebug saveSlotMaterialToWand] about to update the material slots inside the byte config menu");

        // logWandData();

        if (!this.wandItem.isEmpty()) {
            CustomData.update(DataComponents.CUSTOM_DATA, this.wandItem, tag -> {
                CompoundTag rootTag = this.wandItem.get(DataComponents.CUSTOM_DATA).copyTag();
                CompoundTag materialData = rootTag.getCompound("material_data");
                CreateBuildingWands.LOGGER.info("[saveSlotMaterialToWand] here's the value of materialData BEFORE ANY MODIFICATION: {}", materialData);
                if (materialStack.isEmpty()) {
                    // creating clean tag to put into the wand data
                    CompoundTag cleanCornerTag = new CompoundTag();
                    // creating a tag to contain the default copycat material
                    CompoundTag defaultMat = new CompoundTag();
                    // setting the default material to create copycat base
                    defaultMat.putString("Name", "create:copycat_base");

                    // adding the default material to the clean tag that will be applied to the corner
                    cleanCornerTag.put("material", defaultMat);
                    // disabling connected textures, copycats+ default
                    cleanCornerTag.putByte("enableCT", (byte) 0);
                    // cleaning consumed item by creating an empty tag
                    cleanCornerTag.put("consumedItem", new CompoundTag());

                    // overwrite the old corner to now include the default settings
                    materialData.put(corner.getNbtKey(), cleanCornerTag);

                    CreateBuildingWands.LOGGER.info("[WandDebug saveSlotMaterialToWand] test drive of setting default texture, here is the entire tag: {}", materialData);
                    // the two below things should do the thing
                    tag.put("material_data", materialData);
                    CreateBuildingWands.LOGGER.info("[saveSlotMaterialToWand] here's the tag: {}", tag);
                    // logWandData();
                } else if (materialStack.getItem() instanceof BlockItem blockItem) {
                    // creating new corner tag to put into the wand data
                    CompoundTag newCornerTag = new CompoundTag();
                    // creating tag to contain the material
                    CompoundTag newMaterial = new CompoundTag();

                    // setting the material to be what the player requested; the BuiltInRegistries stuff is to make sure the block itself is gotten, and not an item or whatever; crucial for certain blocks that have different block names than item names (like redstone/redstone wire (even though that can't be used here but still))
                    newMaterial.putString("Name", BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString());

                    // adding the new material to the new tag created above
                    newCornerTag.put("material", newMaterial);
                    // enabling connected textures, copycats+ default 
                    newCornerTag.putByte("enableCT", (byte) 1);
                    // get the item name to maybe consume
                    String currentItemName = BuiltInRegistries.ITEM.getKey(materialStack.getItem()).toString();
                    // logic to determine if there's a consumed item already in the thing
                    CompoundTag existingCornerTag = materialData.getCompound(corner.getNbtKey());
                    CreateBuildingWands.LOGGER.info("[WandDebug saveSlotMaterialToWand] about to check cornerTag to see if it has consumedItem, here's the whole tag before the check: {}", existingCornerTag);
                    CreateBuildingWands.LOGGER.info("[WandDebug saveSlotMaterialToWand] does cornerTag contain consumedItem? {}", existingCornerTag.contains("consumedItem", Tag.TAG_COMPOUND));
                    // remove the below if statement when the default consumedItem is correctly applied
                    if (existingCornerTag.contains("consumedItem", Tag.TAG_COMPOUND)) {
                        CompoundTag existingConsumed = existingCornerTag.getCompound("consumedItem");

                        if (existingConsumed.getString("id").equals(currentItemName)) {
                            // do nothing
                            CreateBuildingWands.LOGGER.info("[WandDebug saveSlotMaterialToWand] material already exists inside copycat, will not consume any more items");
                        } else {
                            // consume the item with count 1
                            CompoundTag consumed = new CompoundTag();
                            consumed.putString("id", currentItemName);
                            consumed.putInt("count", 1);
                            newCornerTag.put("consumedItem", consumed);
                            CreateBuildingWands.LOGGER.info("[WandDebug saveSlotMaterialToWand] material does not yet exist inside copycat, will consume an item");
                        }
                    }
                    // modifying materialData with the completely modified tag
                    materialData.put(corner.getNbtKey(), newCornerTag);
                    CreateBuildingWands.LOGGER.info("[WandDebug saveSlotMaterialToWand] test drive of inserting the slot, here is the entire tag before modifying: {}", materialData);
                    // these two should do it
                    tag.put("material_data", materialData);
                    // logWandData();
                    // TODO items still do not persist visually in wand, even if the materials do
                    // TODO materials no longer apply when blocks are placed
                }
            });
        }

        this.broadcastChanges();
        logWandData();
    }
}