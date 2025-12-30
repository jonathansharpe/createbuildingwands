package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.avgusrname.createbuildingwands.component.ByteBlockConfiguration;
import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.simibubi.create.AllBlocks;

public class ByteConfigMenu extends AbstractContainerMenu {

    private final ItemStack wandStack;
    private final InteractionHand hand;
    private final List<String> byteProperties;
    private final Map<String, ItemStackHandler> byteSlotHandlers = new HashMap<>();

    public static final int INVENTORY_START_X = 29;
    public static final int INVENTORY_START_Y = 120;
    public static final int HOTBAR_START_Y = 178;

    public ByteConfigMenu(int id, Inventory inv, InteractionHand hand) {
        super(ModMenuTypes.BYTE_CONFIG.get(), id);
        this.hand = hand;
        this.wandStack = inv.player.getItemInHand(hand);

        this.byteProperties = new ArrayList<>(ByteBlockConfiguration.getAllBytePropertyNames());
        System.out.println("ByteConfigMenu: Loaded " + byteProperties.size() + " byte properties");

        ByteBlockConfiguration existingConfig = wandStack.get(ModDataComponents.BYTE_BLOCK_CONFIG.get());
        if (existingConfig != null) {
            System.out.println("ByteConfigMenu: Found existing config with " + existingConfig.getEnabledBytes().size() + " enabled bytes");
        }

        for (String property : byteProperties) {
            ItemStackHandler handler = createByteSlotHandler(property, existingConfig);
            byteSlotHandlers.put(property, handler);
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9,
                INVENTORY_START_X + col * 18,
                INVENTORY_START_Y + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col,
            INVENTORY_START_X + col * 18,
            HOTBAR_START_Y));
        }
    }

    private ItemStackHandler createByteSlotHandler(String property, ByteBlockConfiguration config) {
        return new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                System.out.println("Slot changed for byte " + property);
                updateWandConfiguration();
                ByteConfigMenu.this.broadcastChanges();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.isEmpty() || stack.getItem() instanceof BlockItem;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!isItemValid(slot, stack)) {
                    return stack;
                }

                if (!simulate) {
                    setStackInSlot(0, stack.copyWithCount(1));
                    onContentsChanged(slot);
                }
                return stack.getCount() > 1 ? stack.copyWithCount(stack.getCount() - 1) : ItemStack.EMPTY;
            }
            {
                if (config != null && config.isByteEnabled(property)) {
                    Block existingBlock = config.getByteTextureBlock(property);
                    if (!(existingBlock == Blocks.AIR)) {
                        this.setStackInSlot(0, new ItemStack(existingBlock));
                        System.out.println("Loaded texture for " + property + ": " + existingBlock.getName());
                    }
                }
            }
        };
    }

    public boolean getByte(String property) {
        ByteBlockConfiguration config = wandStack.getOrDefault(ModDataComponents.BYTE_BLOCK_CONFIG.get(), new ByteBlockConfiguration());
        return config.isByteEnabled(property);
    }

    public void toggleByte(String property) {
        ByteBlockConfiguration config = wandStack.getOrDefault(ModDataComponents.BYTE_BLOCK_CONFIG.get(), new ByteBlockConfiguration());
        boolean isCurrentlyEnabled = config.isByteEnabled(property);
        boolean newState = !isCurrentlyEnabled;
        System.out.println("Toggling byte '" + property + "' from " + isCurrentlyEnabled + " to " + newState);

        ByteBlockConfiguration updated;

        if (newState) {
            Block currentBlockTexture = config.getByteTextureBlock(property);
            System.out.println("the value of currentBlockTexture is: " + currentBlockTexture);
            updated = config.withByteEnabled(property, true);
            System.out.println("the value of updated is now: " + updated);
        }
        else {
            updated = config.withByteEnabled(property, false);

            if (byteSlotHandlers.containsKey(property)) {
                byteSlotHandlers.get(property).setStackInSlot(0, ItemStack.EMPTY);
            }
        }

        wandStack.set(ModDataComponents.BYTE_BLOCK_CONFIG.get(), updated);
        this.broadcastChanges();
        System.out.println("wand BYTE_BLOCK_CONFIG is now: " + updated);
    }

    public ItemStack getMaterialForPart(String property) {
        ItemStackHandler handler = byteSlotHandlers.get(property);
        return handler != null ? handler.getStackInSlot(0) : ItemStack.EMPTY;
    }

    public void setMaterialForPart(String property, ItemStack stack) {
        ItemStackHandler handler = byteSlotHandlers.get(property);
        if (handler != null) {
            handler.setStackInSlot(0, stack);

            ByteBlockConfiguration config = wandStack.getOrDefault(ModDataComponents.BYTE_BLOCK_CONFIG.get(), new ByteBlockConfiguration());
            if (!stack.isEmpty() && !config.isByteEnabled(property)) {
                wandStack.set(ModDataComponents.BYTE_BLOCK_CONFIG.get(), config.withByteEnabled(property, true));
            }

            updateWandConfiguration();
        }
    }

    public void updateWandConfiguration() {
        ByteBlockConfiguration newConfig = new ByteBlockConfiguration();

        System.out.println("Updating wand byte configuration...");

        for (String property : byteProperties) {
            ItemStackHandler handler = byteSlotHandlers.get(property);
            if (handler == null) continue;

            ItemStack textureStack = handler.getStackInSlot(0);

            if (!textureStack.isEmpty()) {
                newConfig = newConfig.withByteTextureBlock(property, Block.byItem(textureStack.getItem()));
                System.out.println("   Configured byte: " + property + " = " + textureStack.getDisplayName().getString());
            }
        }

        if (newConfig.isEmpty()) {
            wandStack.remove(ModDataComponents.BYTE_BLOCK_CONFIG.get());
            System.out.println("Config is empty, removed from wand");
        }
        else {
            wandStack.set(ModDataComponents.BYTE_BLOCK_CONFIG.get(), newConfig);
            System.out.println("Saved config with " + newConfig.getEnabledBytes().size() + " enabled bytes");
        }
    }

    public ItemStack getWandStack() {
        return wandStack;
    }

    public List<String> getByteProperties() {
        return byteProperties;
    }

    public ItemStackHandler getByteSlotHandler(String property) {
        return byteSlotHandlers.get(property);
    }

    // will likely leave this un implemented because idk how you can shift-click into a specific slot without putting it in all of them
    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();

            if (!(slotStack.getItem() instanceof BlockItem)) {
                return ItemStack.EMPTY;
            }
            
            for (String property : byteProperties) {
                ItemStackHandler handler = byteSlotHandlers.get(property);
                if (handler != null && handler.getStackInSlot(0).isEmpty()) {
                    handler.setStackInSlot(0, slotStack.copyWithCount(1));
                    System.out.println("Shift-clicked into byte slot: " + property);
                    return itemStack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p) {
        return p.getItemInHand(hand).equals(wandStack);
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        updateWandConfiguration();
        ItemStack actualWand = p.getItemInHand(this.hand);
        ByteBlockConfiguration finalConfig = wandStack.get(ModDataComponents.BYTE_BLOCK_CONFIG.get());

        if (finalConfig != null) {
            actualWand.set(ModDataComponents.BYTE_BLOCK_CONFIG.get(), finalConfig);
            System.out.println("ByteConfigMenu closed, final config saved");
        }
        else {
            System.out.println("final config was not saved, there's a problem");
        }
    }
    
}
