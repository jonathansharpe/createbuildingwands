package com.avgusrname.createbuildingwands.item.andesiteWand;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.avgusrname.createbuildingwands.item.WandClientPreview;
import com.avgusrname.createbuildingwands.item.WandMode;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandConfigMenu;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandMaterialComponent;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandPlacementContext;
import com.avgusrname.createbuildingwands.networking.packet.ForceRedrawPacket;
import com.avgusrname.createbuildingwands.networking.packet.WandPreviewPacket;
import com.avgusrname.createbuildingwands.util.BlockPlaceHelper;
import com.avgusrname.createbuildingwands.util.WandUtils;
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlockEntity;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlock;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlockEntity;
import com.copycatsplus.copycats.foundation.copycat.multistate.MaterialItemStorage;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

// SPAGHETTI
public class AndesiteWandItem extends Item {

    public AndesiteWandItem(Properties properties) {
        super(properties);
    }

    /**
     * sets the wand mode as defined for the user
     * @param wand the wand
     * @param mode the mode at which to set
     */
    public static void setMode(ItemStack wand, WandMode mode) {
        wand.set(ModDataComponents.WAND_MODE.get(), mode);
        WandMode confirmedMode = wand.getOrDefault(ModDataComponents.WAND_MODE.get(), WandMode.SINGLE);
    }

    /**
     * will open the config menu, called when shift-right click or maybe in other instances. it at least separates this logic for easier reading
     * @param level the minecraft world
     * @param player the player holding the wand
     * @param hand the hand which is using the wand
     */
    private static void openConfig(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        MenuProvider containerProvider = new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.literal("Wand Configuration");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
                return new WandConfigMenu(id, inv, hand);
            }
        };

        serverPlayer.openMenu(containerProvider);
    }

    /**
     * function called when a right click while not facing a block is performed
     * @param level the minecraft world
     * @param player the player who performed the right click
     * @param hand the hand that performed the right click
     * @return the item stack to return. in the wands case, it'll just be the wand again.
     */
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack wand = player.getItemInHand(hand);

        if (!player.isCrouching()) {
            return InteractionResultHolder.pass(wand);
        }

        if (level.isClientSide()) {
            player.swing(hand);
            return InteractionResultHolder.consume(wand);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(wand);
        }

        if (wand.has(ModDataComponents.WAND_START_POS.get())) {
            wand.remove(ModDataComponents.WAND_START_POS.get());
            player.displayClientMessage(
                    Component.literal("Start position cleared").withStyle(ChatFormatting.WHITE), true);
            PacketDistributor.sendToPlayer(serverPlayer, new WandPreviewPacket(List.of(), Optional.empty()));
            return InteractionResultHolder.consume(wand);
        }

        serverPlayer.openMenu(getMenuProvider(hand), buf -> buf.writeEnum(hand));
        return InteractionResultHolder.consume(wand);
    }

    /**
     * called when a right click while facing a block is performed
     * @param context the world context
     * @return pass, fail, or consume depending on what happens
     */
    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack heldWand = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();

        if (level.isClientSide()) {
            assert player != null;
            handleClientSide(heldWand, player, clickedPos, clickedFace);
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        if (player.isCrouching()) {
            if (heldWand.has(ModDataComponents.WAND_START_POS.get())) {
                return removeStartPosition(serverPlayer, heldWand, player);
            }
            openConfig(level, player, context.getHand());
            return InteractionResult.CONSUME;
        }

        Block copycatBlock = heldWand.get(ModDataComponents.WAND_BLOCK_COPYCAT.get());
        Block regularBlock = heldWand.get(ModDataComponents.WAND_BLOCK_REGULAR.get());
        boolean isCopycat = copycatBlock != null && !copycatBlock.defaultBlockState().isAir();
        Block blockToPlace = isCopycat ? copycatBlock : regularBlock;

        if ((blockToPlace == null) || blockToPlace.defaultBlockState().isAir()) {
            return InteractionResult.PASS;
        }

        ItemStack materialStack = regularBlock != null ? new ItemStack(regularBlock.asItem()) : ItemStack.EMPTY;
        WandMode currentMode = heldWand.getOrDefault(ModDataComponents.WAND_MODE.get(), WandMode.SINGLE);
        BlockPlaceContext placeContext = new BlockPlaceContext(context);

        boolean placed = switch (currentMode) {
            // TODO this does not work with doors (or any other multi-block blocks) plz fix
            case SINGLE -> {
                if (!consumeItemsForPlacement(serverPlayer, heldWand, blockToPlace, 1)) yield false;
                yield placeBlock(serverPlayer, clickedPos.relative(clickedFace),
                        new WandPlacementContext(blockToPlace, materialStack, isCopycat, placeContext));
            }
            case LINE, PLANE, CUBE -> placeMultiple(currentMode, serverPlayer, heldWand, placeContext);
            case SPHERE -> false;
        };

        return placed ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    /**
     * attempts to consume items for placement, and if it doesn't work it sends a message in chat telling the player. will return true if player is in creative and no items will be consumed
     * @param player the player attempting to place
     * @param wand the wand trying to place
     * @param blockToPlace block attempting to be placed
     * @param count number of blocks attempting to be placed
     * @return true if the placement can go through, false if not
     */
    private boolean consumeItemsForPlacement(ServerPlayer player, ItemStack wand, Block blockToPlace, int count) {
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
     * removes the start position from the wand
     * @param serverPlayer the player holding the wand on the server side
     * @param heldWand the wand item the player is holding
     * @param player the player holding the wand on the client side
     * @return the InteractionResult, in this case CONSUME since the behavior is always consistent
     */
    private static @NotNull InteractionResult removeStartPosition(ServerPlayer serverPlayer, ItemStack heldWand, Player player) {
        heldWand.remove(ModDataComponents.WAND_START_POS.get());
        player.displayClientMessage(
                Component.literal("Start position cleared").withStyle(ChatFormatting.WHITE), true);
        PacketDistributor.sendToPlayer(serverPlayer, new WandPreviewPacket(List.of(), Optional.empty()));
        return InteractionResult.CONSUME;
    }

    /**
     * does anything client side stuff, notably displaying the block preview
     * @param heldWand the wand whose data is being accessed
     * @param player the player on the client side
     * @param clickedPos the coordinates clicked
     * @param clickedFace the face clicked
     */
    private void handleClientSide(ItemStack heldWand, Player player, BlockPos clickedPos, Direction clickedFace) {
        if (player.isCrouching()) return;

        WandMode currentMode = heldWand.getOrDefault(ModDataComponents.WAND_MODE.get(), WandMode.SINGLE);

        if (heldWand.has(ModDataComponents.WAND_START_POS.get())) {
            WandClientPreview.updateActiveState(null, null);
            WandClientPreview.clearPreviewPositions();
        } else {
            WandClientPreview.updateActiveState(clickedPos.relative(clickedFace), currentMode);

            Block copycat = heldWand.get(ModDataComponents.WAND_BLOCK_COPYCAT.get());
            Block regular = heldWand.get(ModDataComponents.WAND_BLOCK_REGULAR.get());

            ItemStack previewBlock = copycat != null && !copycat.defaultBlockState().isAir()
                    ? new ItemStack(copycat.asItem())
                    : regular != null ? new ItemStack(regular.asItem()) : ItemStack.EMPTY;

            WandClientPreview.setPreviewBlock(previewBlock);
        }
    }

    /**
     * applies properties using a map function thingy
     * @param state existing state of the state
     * @param prop properties to apply
     * @param value values of the properties to apply
     * @return the properties after modification
     * @param <T> type varies i guess
     */
    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> prop, String value) {
        return prop.getValue(value)
                .map(v -> state.setValue(prop, v))
                .orElse(state);
    }

    /**
     * gets the required materials for placement
     * @param wand wands data being accessed
     * @param copycatBlock the copycat block
     * @param count number of full blocks being placed
     * @param registries the regstries, which gives more context
     * @return list of items that are required for placement
     */
    private List<ItemStack> getRequiredItemsForPlacement(ItemStack wand, Block copycatBlock, int count, HolderLookup.Provider registries) {
        List<ItemStack> required = new ArrayList<>();

        // if its a multi state copycat
        if (copycatBlock instanceof IMultiStateCopycatBlock multiState) {
            WandMaterialComponent component = WandUtils.getCurrentComponent(wand);

            long activeParts = getActiveParts(multiState, component);

            required.add(new ItemStack(copycatBlock.asItem(), (int) (count * activeParts)));

            MaterialItemStorage storage = component.toStorage(copycatBlock, registries);
            Set<Item> seen = new HashSet<>();
            for (String key : multiState.storageProperties()) {
                if (!isValueActive(component.blockStateProps().get(key))) continue;
                MaterialItemStorage.MaterialItem item = storage.getMaterialItem(key);
                if (item == null || item.consumedItem().isEmpty()) continue;
                if (seen.add(item.consumedItem().getItem())) {
                    required.add(new ItemStack(item.consumedItem().getItem(), count));
                }
            }
        } else {
            required.add(new ItemStack(copycatBlock.asItem(), count));
            Block regularBlock = wand.get(ModDataComponents.WAND_BLOCK_REGULAR.get());
            if (regularBlock != null) required.add(new ItemStack(regularBlock.asItem(), count));
        }

        return required;
    }

    /**
     * returns the number of active parts in a single block, which will tell other methods how many multistate copycat items need to be consumed
     * @param multiState the multistate block attempted to be placed
     * @param component the wand data
     * @return number of total active parts
     */
    private static long getActiveParts(IMultiStateCopycatBlock multiState, WandMaterialComponent component) {
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
     * @param value the value to check in the component
     * @return true if the value is "true", false if not
     */
    private boolean isValueActive(String value) {
        if (value == null) return false;
        try {
            return Integer.parseInt(value) > 0;
        } catch (NumberFormatException e) {
            return "true".equals(value);
        }
    }

    /**
     * places one or multiple blocks
     * @param player the player holding the wand
     * @param pos the position right clicked by the player holding the wand
     * @param ctx more context that contains information for the block placement, exists to cut down on parameters
     * @return true if the placement succeeded, false if not
     */

    private boolean placeBlock(ServerPlayer player, BlockPos pos, WandPlacementContext ctx) {

        Level level = player.level();

        if (!level.getBlockState(pos).canBeReplaced()) return false;

        BlockPlaceContext localContext = BlockPlaceContext.at(ctx.blockPlaceContext(), pos, ctx.blockPlaceContext().getClickedFace());
        BlockState stateToPlace = getOrientedBlockState(ctx.blockToPlace(), localContext);

        if (!ctx.isCopycat()) {
            return level.setBlock(pos, stateToPlace, Block.UPDATE_ALL);
        }

        WandMaterialComponent materialComponent = WandUtils.getCurrentComponent(player.getItemInHand(InteractionHand.MAIN_HAND));

        BlockState finalStateToPlace = stateToPlace;

        if (ctx.blockToPlace() instanceof IMultiStateCopycatBlock copycatBlock) {

            for (Property<?> prop : ctx.blockToPlace().defaultBlockState().getProperties()) {
                if (prop instanceof BooleanProperty boolProp && copycatBlock.storageProperties().contains(boolProp.getName())) {
                    finalStateToPlace = finalStateToPlace.setValue(boolProp, false);
                }
                String value = materialComponent.blockStateProps().get(prop.getName());
                if (value == null) continue;
                finalStateToPlace = applyProperty(finalStateToPlace, prop, value);
            }

            if (!isAnyActive(copycatBlock, materialComponent)) {
                player.displayClientMessage(
                        Component.literal("No parts are enabled, cannot place block").withStyle(ChatFormatting.RED),
                        true
                );
                return false;
            }

            BlockState oldState = level.getBlockState(pos);
            if (!level.setBlock(pos, finalStateToPlace, Block.UPDATE_NEIGHBORS | Block.UPDATE_KNOWN_SHAPE)) {
                return false;
            }

            BlockEntity targetBE = level.getBlockEntity(pos);
            if (!(targetBE instanceof IMultiStateCopycatBlockEntity copycatMock)) {
                return false;
            }

            copycatMock.init();
            MaterialItemStorage wandStorage = materialComponent.toStorage((Block) copycatBlock, player.level().registryAccess());
            MaterialItemStorage beStorage = copycatMock.getMaterialItemStorage();

            getSeenItems(wandStorage, beStorage);

            syncBE(level, pos, targetBE, oldState, finalStateToPlace);
        } else {
            // non-multistate copycat block logic
            Block regularBlock = player.getItemInHand(InteractionHand.MAIN_HAND)
                    .get(ModDataComponents.WAND_BLOCK_REGULAR.get());

            BlockState oldState = level.getBlockState(pos);
            if (!level.setBlock(pos, finalStateToPlace, Block.UPDATE_NEIGHBORS | Block.UPDATE_KNOWN_SHAPE)) {
                return false;
            }

            assert regularBlock != null;
            BlockState materialState = regularBlock.defaultBlockState();
            ItemStack consumedItem = new ItemStack(regularBlock.asItem());
            applyCopycatMaterial(level, pos, materialState, consumedItem);

            syncBE(level, pos, level.getBlockEntity(pos), oldState, finalStateToPlace);
        }
        return true;
    }

    /**
     * determines how many instances of a block there are inside a multi state copycat
     * @param wandStorage existing wand storage of the copycat
     * @param beStorage new storage of the copycat
     */
    private static void getSeenItems(MaterialItemStorage wandStorage, MaterialItemStorage beStorage) {
        Set<Item> seenItems = new HashSet<>();
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
     * @param copycatBlock the block to search the storage properties of
     * @param materialComponent the data on the wand that decides what block state/block entity to place
     * @return true if any parts are active, false if none are active
     */

    private static boolean isAnyActive(IMultiStateCopycatBlock copycatBlock, WandMaterialComponent materialComponent) {
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
     * modifies the block entity at the specified position to match the updated state
     * @param level the minecraft world (needed context)
     * @param pos the block position to modify
     * @param be the block entity to modify
     * @param oldState the previous block state
     * @param newState the new block state to update the old one to
     */
    private void syncBE(Level level, BlockPos pos, BlockEntity be, BlockState oldState, BlockState newState) {
        if (be == null) {
            return;
        }
        be.setChanged();

        if (level instanceof ServerLevel serverLevel) {
            var syncPacket = be.getUpdatePacket();
            if (syncPacket != null) {
                serverLevel.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false)
                        .forEach(p -> p.connection.send(syncPacket));
            }
            serverLevel.getChunkSource().blockChanged(pos);
            serverLevel.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false)
                    .forEach(p -> PacketDistributor.sendToPlayer(p, new ForceRedrawPacket(pos)));
        }

        level.sendBlockUpdated(pos, oldState, newState, Block.UPDATE_ALL);

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            BlockState updatedNeighbor = neighborState.updateShape(
                    direction.getOpposite(), newState, level, neighborPos, pos
            );
            if (updatedNeighbor != neighborState) {
                level.setBlock(neighborPos, updatedNeighbor, Block.UPDATE_ALL);
            }
        }
    }

    /**
     * places many blocks at a time, according to the user-defined shape
     * @param mode the shape the player wants to place blocks in, like line, single, plane, etc
     * @param player player holding the wand
     * @param wand wand held by the player
     * @param context context or something
     * @return true if the placement succeeded, false if not
     */
    private boolean placeMultiple(WandMode mode, ServerPlayer player, ItemStack wand, BlockPlaceContext context) {
        Level level = player.level();
        if (level.isClientSide) {
            return false;
        }

        Block regularBlock = wand.get(ModDataComponents.WAND_BLOCK_REGULAR.get());
        Block copycatBlock = wand.get(ModDataComponents.WAND_BLOCK_COPYCAT.get());
        boolean useCopycat = copycatBlock != null;
        Block blockToPlace = useCopycat ? copycatBlock : regularBlock;

        if (blockToPlace == null) {
            player.displayClientMessage(
                    Component.literal("No block configured in wand").withStyle(ChatFormatting.RED), true );
            return false;
        }

        if (player.isShiftKeyDown()) {
            if (wand.has(ModDataComponents.WAND_START_POS.get())) {
                wand.remove(ModDataComponents.WAND_START_POS.get());
                player.displayClientMessage(
                        Component.literal(mode.name() + " selection cancelled.").withStyle(ChatFormatting.RED), true);
            }
            return true;
        }

        if (!wand.has(ModDataComponents.WAND_START_POS.get())) {
            wand.set(ModDataComponents.WAND_START_POS.get(), context.getClickedPos());
            player.displayClientMessage(Component.literal("Start position set"), true);
            return true;
        }

        BlockPos startPos = wand.get(ModDataComponents.WAND_START_POS.get());
        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();

        List<BlockPos> positions = switch (mode) {
            case LINE -> BlockPlaceHelper.lineBlockPositions(startPos, clickedPos);
            case PLANE -> BlockPlaceHelper.planeBlockPositions(startPos, clickedPos, face);
            case CUBE -> BlockPlaceHelper.cubeBlockPositions(startPos, clickedPos);
            default -> List.of();
        };

        if (positions.isEmpty()) return false;

        ItemStack regularStack = regularBlock != null ? new ItemStack(regularBlock.asItem()) : ItemStack.EMPTY;
        WandPlacementContext ctx = new WandPlacementContext(blockToPlace, regularStack, useCopycat, context);

        if (!consumeItemsForPlacement(player, wand, blockToPlace, positions.size())) return false;

        int placedCount = 0;
        for (BlockPos pos : positions) {
            if (placeBlock(player, pos, ctx)) {
                placedCount++;
            }
        }

        wand.remove(ModDataComponents.WAND_START_POS.get());
        player.displayClientMessage(
                Component.literal("Placed " + placedCount + " blocks in a " + mode.name().toLowerCase())
                        .withStyle(ChatFormatting.GREEN), true);

        return placedCount > 0;
    }

    /**
     * will check if the items cant be consumed, like if the player doesn't have enough
     * @param inventory players inventory
     * @param required items required for consumption
     * @return false if items can be consumed, true if they cant
     */
    private boolean consumeItems(Inventory inventory, List<ItemStack> required) {
        Map<ItemStack, List<int[]>> consumptionPlan = new LinkedHashMap<>();

        for (ItemStack requirement : required) {
            CreateBuildingWands.LOGGER.info("[WandDebug] tryConsumeItems processing: {} x{}", requirement.getItem(), requirement.getCount());
            if (requirement.isEmpty()) continue;

            List<int[]> slots = new ArrayList<>();
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

    /**
     * gets the block state necessary for placement.
     * @param block the block attempting to be placed
     * @param context the context surrounding the block
     * @return the block state of the block
     */
    private BlockState getOrientedBlockState(Block block, BlockPlaceContext context) {

        BlockState state = block.getStateForPlacement(context);

        return state != null ? state : block.defaultBlockState();
    }

    /**
     * applies the copycat material properties after block placement
     * @param level the minecraft world
     * @param pos position of block to modify
     * @param materialState state to apply to the block
     * @param consumedItem items to consume
     */
    private void applyCopycatMaterial(Level level, BlockPos pos, BlockState materialState, ItemStack consumedItem) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;

        BlockState currentState = level.getBlockState(pos);

        if (materialState.is(currentState.getBlock())) return;

        switch (be) {
            case IMultiStateCopycatBlockEntity multiStateBE -> {
                String property = multiStateBE.getBlock().defaultProperty();
                multiStateBE.setMaterial(property, materialState);
                multiStateBE.setConsumedItem(property, consumedItem);
                multiStateBE.notifyUpdate();
            }
            case ICopycatBlockEntity copycatBE -> {
                copycatBE.setMaterial(materialState);
                copycatBE.setConsumedItem(consumedItem);
                copycatBE.notifyUpdate();
            }
            case CopycatBlockEntity createCopycatBE -> {
                createCopycatBE.setMaterial(materialState);
                createCopycatBE.setConsumedItem(consumedItem);
                createCopycatBE.notifyUpdate();
            }
            default -> CreateBuildingWands.LOGGER.warn("applyCopycatMaterial: unrecognized BE type {}", be.getClass().getName());
        }

        be.setChanged();
        level.sendBlockUpdated(pos, currentState, currentState, Block.UPDATE_ALL);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().blockChanged(pos);
        }
    }

    /**
     * gets the menu or something when right clicked
     * @param pHand the hand of the wand
     * @return the menu or something
     */
    private MenuProvider getMenuProvider(InteractionHand pHand) {
        return new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.translatable("screen.createbuildingwands.wand_config");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
                return new WandConfigMenu(id, inv, pHand);
            }
        };
    }
}
