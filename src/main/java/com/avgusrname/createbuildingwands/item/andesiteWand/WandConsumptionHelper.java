package com.avgusrname.createbuildingwands.item.andesiteWand;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandMaterialComponent;
import com.avgusrname.createbuildingwands.util.WandUtils;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlock;
import com.copycatsplus.copycats.foundation.copycat.multistate.MaterialItemStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.*;

public class WandConsumptionHelper {
    public WandConsumptionHelper() {
    }

    /**
     * attempts to consume items for placement, and if it doesn't work it sends a message in chat telling the player.
     * will return true if player is in creative and no items will be consumed
     *
     * @param player       the player attempting to place
     * @param wand         the wand trying to place
     * @param blockToPlace block attempting to be placed
     * @param count        number of blocks attempting to be placed
     * @return true if the placement can go through, false if not
     */
    boolean consumeItemsForPlacement(ServerPlayer player, ItemStack wand, Block blockToPlace, int count) {
        if (player.isCreative()) return true;

        List<ItemStack> required = getRequiredItemsForPlacement(wand, blockToPlace, count, player.level().registryAccess());
        if (!consumeItems(player.getInventory(), required)) {
            player.displayClientMessage(
                    Component.literal("Not enough blocks to complete the placement").withStyle(ChatFormatting.RED), true);
            return false;
        }
        return true;
    }

    /**
     * gets the required materials for placement
     *
     * @param wand         wands data being accessed
     * @param copycatBlock the copycat block
     * @param count        number of full blocks being placed
     * @param registries   the regstries, which gives more context
     * @return list of items that are required for placement
     */
    List<ItemStack> getRequiredItemsForPlacement(ItemStack wand, Block copycatBlock, int count, HolderLookup.Provider registries) {
        List<ItemStack> required = new ArrayList<ItemStack>();

        // if its a multi state copycat
        if (copycatBlock instanceof IMultiStateCopycatBlock multiState) {
            WandMaterialComponent component = WandUtils.getCurrentComponent(wand);

            long activeParts = getActiveParts(multiState, component);

            required.add(new ItemStack(copycatBlock.asItem(), (int) (count * activeParts)));

            MaterialItemStorage storage = component.toStorage(copycatBlock, registries);
            Set<Item> seen = new HashSet<Item>();
            for (String key : multiState.storageProperties()) {
                if (!isValueActive(component.blockStateProps().get(key))) {
                    continue;
                }
                MaterialItemStorage.MaterialItem item = storage.getMaterialItem(key);
                if (item == null || item.consumedItem().isEmpty()) {
                    continue;
                }
                if (seen.add(item.consumedItem().getItem())) {
                    required.add(new ItemStack(item.consumedItem().getItem(), count));
                }
            }
        } else {
            required.add(new ItemStack(copycatBlock.asItem(), count));
            Block regularBlock = wand.get(ModDataComponents.WAND_BLOCK_REGULAR.get());
            if (regularBlock != null) {
                boolean add = required.add(new ItemStack(regularBlock.asItem(), count));
            }
        }

        return required;
    }

    /**
     * returns the number of active parts in a single block, which will tell other methods how many multistate copycat
     * items need to be consumed
     *
     * @param multiState the multistate block attempted to be placed
     * @param component  the wand data
     * @return number of total active parts
     */
    static long getActiveParts(IMultiStateCopycatBlock multiState, WandMaterialComponent component) {
        return multiState.storageProperties().stream()
                .mapToLong(key -> {
                    String value = component.blockStateProps().get(key);
                    if (value == null) return 0;
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        return "true".equals(value) ? 1 : 0;
                    }
                })
                .sum();
    }

    /**
     * checks if a value is active
     *
     * @param value the value to check in the component
     * @return true if the value is "true", false if not
     */
    boolean isValueActive(String value) {
        if (value == null) return false;
        try {
            return Integer.parseInt(value) > 0;
        } catch (NumberFormatException e) {
            return "true".equals(value);
        }
    }

    /**
     * determines how many instances of a block there are inside a multi state copycat
     *
     * @param wandStorage existing wand storage of the copycat
     * @param beStorage   new storage of the copycat
     */
    static void getSeenItems(MaterialItemStorage wandStorage, MaterialItemStorage beStorage) {
        Set<Item> seenItems = new HashSet<Item>();
        for (String key : wandStorage.getAllProperties()) {
            MaterialItemStorage.MaterialItem item = wandStorage.getMaterialItem(key);
            if (item == null || !item.hasCustomMaterial()) {
                continue;
            }

            Item consumedItemType = item.consumedItem().getItem();
            boolean isDuplicate = !item.consumedItem().isEmpty() && !seenItems.add(consumedItemType);

            MaterialItemStorage.MaterialItem toStore = isDuplicate
                    ? new MaterialItemStorage.MaterialItem(item.material(), ItemStack.EMPTY, item.enableCT())
                    : item;

            beStorage.storeMaterialItem(key, toStore);
        }
    }

    /**
     * checks if any parts of the copycat are active
     *
     * @param copycatBlock      the block to search the storage properties of
     * @param materialComponent the data on the wand that decides what block state/block entity to place
     * @return true if any parts are active, false if none are active
     */

    static boolean isAnyActive(IMultiStateCopycatBlock copycatBlock, WandMaterialComponent materialComponent) {
        return copycatBlock.storageProperties().stream().anyMatch(key -> {
            String value = materialComponent.blockStateProps().get(key);
            if (value == null) return false;
            try {
                return Integer.parseInt(value) > 0;
            } catch (NumberFormatException e) {
                return "true".equals(value);
            }
        });
    }

    /**
     * will check if the items cant be consumed, like if the player doesn't have enough
     *
     * @param inventory players inventory
     * @param required  items required for consumption
     * @return false if items can be consumed, true if they cant
     */
    boolean consumeItems(Inventory inventory, List<ItemStack> required) {
        Map<ItemStack, List<int[]>> consumptionPlan = new LinkedHashMap<ItemStack, List<int[]>>();

        for (ItemStack requirement : required) {
            CreateBuildingWands.LOGGER.info("[WandDebug] tryConsumeItems processing: {} x{}", requirement.getItem(), requirement.getCount());
            if (requirement.isEmpty()) continue;

            List<int[]> slots = new ArrayList<int[]>();
            int found = 0;

            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack slot = inventory.getItem(i);
                if (!slot.isEmpty() && ItemStack.isSameItem(slot, requirement)) {
                    int toTake = Math.min(requirement.getCount() - found, slot.getCount());
                    slots.add(new int[]{i, toTake});
                    found += toTake;
                    if (found >= requirement.getCount()) break;
                }
            }

            if (found < requirement.getCount()) return false;
            consumptionPlan.put(requirement, slots);
        }

        for (List<int[]> slots : consumptionPlan.values()) {
            for (int[] slot : slots) {
                inventory.getItem(slot[0]).shrink(slot[1]);
            }
        }

        return true;
    }
}