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
import java.util.Set;
import java.util.HashSet;

import com.avgusrname.createbuildingwands.component.ByteBlockConfiguration;
import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.simibubi.create.AllBlocks;

public class ByteConfigMenu extends AbstractContainerMenu {

    private final ItemStack wandStack;
    private final InteractionHand hand;
    private final Player player;
    private final List<String> byteProperties;
    private final Map<String, ItemStackHandler> byteSlotHandlers = new HashMap<>();

    public static final int INVENTORY_START_X = 29;
    public static final int INVENTORY_START_Y = 120;
    public static final int HOTBAR_START_Y = 178;

    public ByteConfigMenu(int id, Inventory inv, InteractionHand hand) {
        super(ModMenuTypes.BYTE_CONFIG.get(), id);
        this.hand = hand;
        this.player = inv.player;
        this.wandStack = inv.player.getItemInHand(hand);

        this.byteProperties = new ArrayList<>();
        ByteBlockConfiguration existingConfig = wandStack.get(ModDataComponents.BYTE_BLOCK_CONFIG.get());
        System.out.println("ByteConfigMenu: Loaded " + byteProperties.size() + " byte properties");
        Set<String> allPossible = ByteBlockConfiguration.getAllBytePropertyNames();

        if (existingConfig != null) {
            Set<String> enabledOnWand = existingConfig.getEnabledBytes();
            for (String property : allPossible) {
                if (enabledOnWand.contains(property)) {
                    this.byteProperties.add(property);
                }
            }
            System.out.println("ByteConfigMenu: Loaded " + byteProperties.size() + " active properties from wand");
        }
        else {
            System.out.println("ByteConfigMenu: No existing config, starting with empty property list");
        }

        for (String property : allPossible) {
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
        /*
        logic flow:
        - get wand item
        - check to see if given byte is enabled
            - if it is, remove it from the map because it is now disabled
            - if it's not, enable it; give it the copycat base texture as a default
                - if there's a block in the slot, apply that block as the texture
        - update the wand configuration
         */

        ByteBlockConfiguration config = wandStack.getOrDefault(ModDataComponents.BYTE_BLOCK_CONFIG.get(), new ByteBlockConfiguration());

        ItemStackHandler handler = byteSlotHandlers.get(property);
        Block defaultTexture = AllBlocks.COPYCAT_BASE.get();

        if (config.isByteEnabled(property)) {
            byteProperties.remove(property);
            System.out.println("Removed byte: " + property);
        }
        else {
            config = config.withByteTextureBlock(property, defaultTexture);
            if (!byteProperties.contains(property)) {
                byteProperties.add(property);
            }
            System.out.println("Added byte: " + property + " with texture: " + defaultTexture);
        }

        wandStack.set(ModDataComponents.BYTE_BLOCK_CONFIG.get(), config);
        updateWandConfiguration();
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

        if (this.player.level().isClientSide) return;

        ByteBlockConfiguration currentConfig = wandStack.getOrDefault(
            ModDataComponents.BYTE_BLOCK_CONFIG.get(),
            new ByteBlockConfiguration()
        );

        Set<String> keysToSync = new HashSet<>(byteProperties);
        keysToSync.addAll(currentConfig.getEnabledBytes());
        System.out.println("DEBUG: Starting Sync. Unique keys to check: " + keysToSync.size());

        if (keysToSync.isEmpty()) {
            System.out.println("DEBUG: No keys found, removing component.");
            wandStack.remove(ModDataComponents.BYTE_BLOCK_CONFIG.get());
            return;
        }

        for (String property : keysToSync) {
            ItemStackHandler handler = byteSlotHandlers.get(property);
            if (handler == null) continue;
            ItemStack textureStack = handler.getStackInSlot(0);

            if (!textureStack.isEmpty()) {
                Block block = Block.byItem(textureStack.getItem());
                currentConfig = currentConfig.withByteTextureBlock(property, block);
                System.out.println("DEBUG: Found item in slot [" + property + "] -> " + block);
            }
        }

        if (!currentConfig.isEmpty()) {
            wandStack.set(ModDataComponents.BYTE_BLOCK_CONFIG.get(), currentConfig);
            System.out.println("DEBUG: Successfully persisted " + currentConfig.getEnabledBytes().size() + " keys");
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
        System.out.println("ByteConfigMenu: Menu removal triggered. Performing final sync");
        updateWandConfiguration();
        super.removed(p);
    }
    
}
