package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen.ByteCornerData.ByteCopycatCorner;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ByteConfigMenu extends AbstractContainerMenu {
    private final InteractionHand wandHand;
    private final ItemStack wandItem;
    private final ItemStackHandler byteSlotHandler;
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
        this.byteSlotHandler = new ItemStackHandler(8);

        for (int i = 0; i < 8; i++) {
            int[] coords = getComponentCoordinates(i);

            int slotX = coords[0] + BTN_WIDTH + 4;
            int slotY = coords[1] + 1;

            this.addSlot(new SlotItemHandler(byteSlotHandler, i, slotX, slotY));
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

    private void saveSlotMaterialToWand(ByteCopycatCorner corner, ItemStack materialStack) {

        if (this.wandItem.isEmpty())
            return;

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                this.wandItem,
                tag -> {
                    net.minecraft.nbt.CompoundTag materialData = tag.getCompound("material_data");
                    net.minecraft.nbt.CompoundTag cornerTag = materialData.getCompound(corner.getNbtKey());

                    if (materialStack.isEmpty()) {
                        // Reset to baseline if slot is cleared out
                        net.minecraft.nbt.CompoundTag baseMat = new net.minecraft.nbt.CompoundTag();
                        baseMat.putString("Name", "create:copycat_base");
                        cornerTag.put("material", baseMat);
                        cornerTag.put("consumedItem", new net.minecraft.nbt.CompoundTag());
                    } else {
                        // Write out the chosen block ID into the material key
                        net.minecraft.nbt.CompoundTag matBlock = new net.minecraft.nbt.CompoundTag();
                        String registryName = net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .getKey(materialStack.getItem()).toString();
                        matBlock.putString("Name", registryName);
                        cornerTag.put("material", matBlock);

                        // Populate consumedItem field matching Copycats full schema layout
                        net.minecraft.nbt.CompoundTag consumed = new net.minecraft.nbt.CompoundTag();
                        consumed.putInt("count", 1);
                        consumed.putString("id", registryName);
                        cornerTag.put("consumedItem", consumed);
                    }

                    materialData.put(corner.getNbtKey(), cornerTag);
                    tag.put("material_data", materialData);
                });
        this.broadcastChanges();
    }
}