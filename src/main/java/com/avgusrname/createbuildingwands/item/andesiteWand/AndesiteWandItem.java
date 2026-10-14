package com.avgusrname.createbuildingwands.item.andesiteWand;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.avgusrname.createbuildingwands.item.WandClientPreview;
import com.avgusrname.createbuildingwands.item.WandMode;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandConfigMenu;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandMaterialComponent;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandPlacementContext;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.multiState.HalfLayerConfigMenu;
import com.avgusrname.createbuildingwands.networking.packet.ForceRedrawPacket;
import com.avgusrname.createbuildingwands.networking.packet.WandPreviewPacket;
import com.avgusrname.createbuildingwands.util.BlockPlaceHelper;
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlockEntity;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlock;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlockEntity;
import com.copycatsplus.copycats.foundation.copycat.multistate.MaterialItemStorage;
import com.electronwill.nightconfig.core.io.ParsingException;
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
		// so this again checks to make sure this is happening on the server side and also that the player is an instance of a ServerPlayer. the ServerPlayer class extends the Player class, but has server attributes, like ServerGamePacketListenerImpl which seems to have to do with the server
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // creating a new instance of a MenuProvider, which is an interface type. it inherits other interfaces, which seemingly wouldn't be possible with other classes. this new instance overrides the necessary classes.
            MenuProvider containerProvider = new MenuProvider() {
                // this just makes the display name which will be displayed when the menu is drawn
                @Override
                public @NotNull Component getDisplayName() {
					return Component.literal("Wand Configuration");
				}

                // this makes the menu giving the important info. the level is seemingly no longer relevant since at this point we're in a menu, and not interacting with the world, but merely the players inventory and the menu itself.
                @Override
                public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
                    return new WandConfigMenu(id, inv, hand);
                }
            };

            // this then opens the menu we just created
            serverPlayer.openMenu(containerProvider);
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack wand = player.getItemInHand(hand);

        if (!player.isCrouching()) return InteractionResultHolder.pass(wand);

        if (level.isClientSide()) {
            player.swing(hand);
            return InteractionResultHolder.consume(wand);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.fail(wand);

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

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack heldWand = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();

        if (player == null) return InteractionResult.FAIL;

        if (level.isClientSide()) {
            handleClientSide(heldWand, player, clickedPos, clickedFace);
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;

        if (player.isCrouching()) {
            if (heldWand.has(ModDataComponents.WAND_START_POS.get())) {
                heldWand.remove(ModDataComponents.WAND_START_POS.get());
                player.displayClientMessage(
                        Component.literal("Start position cleared").withStyle(ChatFormatting.WHITE), true);
                PacketDistributor.sendToPlayer(serverPlayer, new WandPreviewPacket(List.of(), Optional.empty()));
                return InteractionResult.CONSUME;
            }
            openConfig(level, player, context.getHand());
            return InteractionResult.CONSUME;
        }

        Block copycatBlock = heldWand.get(ModDataComponents.WAND_BLOCK_COPYCAT.get());
        Block regularBlock = heldWand.get(ModDataComponents.WAND_BLOCK_REGULAR.get());
        boolean isCopycat = copycatBlock != null && !copycatBlock.defaultBlockState().isAir();
        Block blockToPlace = isCopycat ? copycatBlock : regularBlock;

        if (blockToPlace == null || blockToPlace.defaultBlockState().isAir()) return InteractionResult.PASS;

        ItemStack materialStack = regularBlock != null ? new ItemStack(regularBlock.asItem()) : ItemStack.EMPTY;
        WandMode currentMode = heldWand.getOrDefault(ModDataComponents.WAND_MODE.get(), WandMode.SINGLE);
        BlockPlaceContext placeContext = new BlockPlaceContext(context);

        boolean placed = switch (currentMode) {
            case SINGLE -> {
                if (!player.isCreative()) {
                    List<ItemStack> required = getRequiredMaterialStacks(heldWand, blockToPlace, 1, player.level().registryAccess());
                    required.forEach(stack -> CreateBuildingWands.LOGGER.info("[WandDebug] required item: {} x {}", stack.getItem(), stack.getCount()));
                    if (!tryConsumeItems(player.getInventory(), required)) {
                        player.displayClientMessage(
                                Component.literal("Not enough blocks to complete the placement").withStyle(ChatFormatting.RED), true);
                        yield false;
                    }
                }
                yield placeBlock(serverPlayer, clickedPos.relative(clickedFace),
                        new WandPlacementContext(blockToPlace, materialStack, isCopycat, placeContext));
            }
            case LINE, PLANE, CUBE -> placeMultiple(currentMode, serverPlayer, heldWand, placeContext);
            case SPHERE -> false;
        };

        return placed ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

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

    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> prop, String value) {
        return prop.getValue(value)
                .map(v -> state.setValue(prop, v))
                .orElse(state);
    }

    private List<ItemStack> getRequiredMaterialStacks(ItemStack wand, Block copycatBlock, int count, HolderLookup.Provider registries) {
        List<ItemStack> required = new ArrayList<>();

        if (!(copycatBlock instanceof IMultiStateCopycatBlock multiState)) {
            required.add(new ItemStack(copycatBlock.asItem(), count));
            Block regularBlock = wand.get(ModDataComponents.WAND_BLOCK_REGULAR.get());
            if (regularBlock != null) required.add(new ItemStack(regularBlock.asItem(), count));
            return required;
        }

        WandMaterialComponent component = wand.getOrDefault(
                ModDataComponents.WAND_MATERIALS.get(),
                WandMaterialComponent.createEmptyDefault()
        );

        var currentStorage = multiState.storageProperties();
        var currentBlockStateProps = component.blockStateProps();

        long activeParts = multiState.storageProperties().stream()
                .filter(key -> "true".equals(component.blockStateProps().get(key)))
                .count();

        // TODO fix the following so the active parts are correctly counted
        long enabledPosLayers = multiState.storageProperties().stream()
                .filter(key -> "positive_layers".equals(component.blockStateProps().get(key)))
                .count();

        long enabledNegLayers = multiState.storageProperties().stream()
                .filter(key -> "negative_layers".equals(component.blockStateProps().get(key)))
                .count();

        // find all layers enabled
        // get the storage properties
        //long totalLayers = multiState.storageProperties().stream(key -> "negative_layers".equals(component.blockStateProps().get(key)));

        required.add(new ItemStack(copycatBlock.asItem(), (int) (count * activeParts)));

        MaterialItemStorage storage = component.toStorage(copycatBlock, registries);
        Set<Item> seen = new HashSet<>();
        for (String key : multiState.storageProperties()) {
            if (!"true".equals(component.blockStateProps().get(key))) continue;
            MaterialItemStorage.MaterialItem item = storage.getMaterialItem(key);
            if (item == null || item.consumedItem().isEmpty()) continue;
            if (seen.add(item.consumedItem().getItem())) {
                required.add(new ItemStack(item.consumedItem().getItem(), count));
            }
        }

        return required;
    }

    private boolean placeBlock(ServerPlayer player, BlockPos pos, WandPlacementContext ctx) {

        Level level = player.level();
        Direction clickedFace = ctx.blockPlaceContext().getClickedFace();

        if (!level.getBlockState(pos).canBeReplaced()) return false;

        BlockPlaceContext localContext = BlockPlaceContext.at(ctx.blockPlaceContext(), pos, clickedFace);
        BlockState stateToPlace = getOrientedBlockState(ctx.blockToPlace(), localContext);

        if (!ctx.isCopycat()) {
            return level.setBlock(pos, stateToPlace, Block.UPDATE_ALL);
        }
        WandMaterialComponent materialComponent = player.getItemInHand(InteractionHand.MAIN_HAND)
                .getOrDefault(ModDataComponents.WAND_MATERIALS.get(),WandMaterialComponent.createEmptyDefault());

        BlockState finalStateToPlace = stateToPlace;

        if (ctx.blockToPlace() instanceof IMultiStateCopycatBlock copycatBlock) {

            for (Property<?> prop : ctx.blockToPlace().defaultBlockState().getProperties()) {
                if (prop instanceof BooleanProperty boolProp && copycatBlock.storageProperties().contains(boolProp.getName())) {
                    finalStateToPlace = finalStateToPlace.setValue(boolProp, false);
                }
            }

            for (Property<?> prop : ctx.blockToPlace().defaultBlockState().getProperties()) {
                String value = materialComponent.blockStateProps().get(prop.getName());
                if (value == null) continue;
                finalStateToPlace = applyProperty(finalStateToPlace, prop, value);
            }

            boolean anyActive =
                    (copycatBlock.storageProperties().stream().anyMatch(key -> "true".equals(materialComponent.blockStateProps().get(key)))) ||
                            (copycatBlock.storageProperties().stream().anyMatch(key -> 0 < Integer.parseInt(materialComponent.blockStateProps().getOrDefault(key, String.valueOf(HalfLayerConfigMenu.DEFAULT_LAYERS)))));

            if (!anyActive) {
                player.displayClientMessage(
                        Component.literal("No parts are enabled, cannot place block").withStyle(ChatFormatting.RED),
                        true
                );
                return false;
            }

            BlockState oldState = level.getBlockState(pos);
            if (!level.setBlock(pos, finalStateToPlace, Block.UPDATE_NEIGHBORS | Block.UPDATE_KNOWN_SHAPE)) return false;

            BlockEntity targetBE = level.getBlockEntity(pos);
            CreateBuildingWands.LOGGER.info("[WandDebug] targetBe after placement: {}", targetBE);
            CreateBuildingWands.LOGGER.info("[WandDebug] is IMultiStateCopycatBlockEntity: {}", targetBE instanceof IMultiStateCopycatBlockEntity);
            if (!(targetBE instanceof IMultiStateCopycatBlockEntity copycatMock)) return false;

            copycatMock.init();
            CreateBuildingWands.LOGGER.info("[WandDebug] BE storage properties after init: {}", copycatMock.getMaterialItemStorage().getAllProperties());
            MaterialItemStorage wandStorage = materialComponent.toStorage((Block) copycatBlock, player.level().registryAccess());
            MaterialItemStorage beStorage = copycatMock.getMaterialItemStorage();
            CreateBuildingWands.LOGGER.info("[WandDebug] wandStorage.getMaterialMap(): {}", wandStorage.getMaterialMap());

            Set<Item> seenItems = new HashSet<>();
            for (String key : wandStorage.getAllProperties()) {
                MaterialItemStorage.MaterialItem item = wandStorage.getMaterialItem(key);
                if (item == null || !item.hasCustomMaterial()) continue;

                Item consumedItemType = item.consumedItem().getItem();
                boolean isDuplicate = !item.consumedItem().isEmpty() && !seenItems.add(consumedItemType);

                MaterialItemStorage.MaterialItem toStore = isDuplicate
                        ? new MaterialItemStorage.MaterialItem(item.material(), ItemStack.EMPTY, item.enableCT())
                        : item;

                beStorage.storeMaterialItem(key, toStore);
            }
            CreateBuildingWands.LOGGER.info("[WandDebug] beStorage after copy: {}", copycatMock.getMaterialItemStorage().getMaterialMap());

            syncBE(level, pos, targetBE, oldState, finalStateToPlace);
        } else {
            // non-multistate copycat block logic
            Block regularBlock = player.getItemInHand(InteractionHand.MAIN_HAND)
                    .get(ModDataComponents.WAND_BLOCK_REGULAR.get());


            BlockState oldState = level.getBlockState(pos);
            if (!level.setBlock(pos, finalStateToPlace, Block.UPDATE_NEIGHBORS | Block.UPDATE_KNOWN_SHAPE)) return false;

            BlockState materialState = regularBlock.defaultBlockState();
            ItemStack consumedItem = new ItemStack(regularBlock.asItem());
            applyCopycatMaterial(level, pos, materialState, consumedItem);

            syncBE(level, pos, level.getBlockEntity(pos), oldState, finalStateToPlace);
        }
        return true;
    }

    private void syncBE(Level level, BlockPos pos, BlockEntity be, BlockState oldState, BlockState newState) {
        if (be == null) return;
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
        if (level.isClientSide) return false;

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

        if (!player.isCreative()) {
            List<ItemStack> required = getRequiredMaterialStacks(wand, blockToPlace, positions.size(), player.level().registryAccess());
            if (!tryConsumeItems(player.getInventory(), required)) {
                player.displayClientMessage(
                        Component.literal("Not enough blocks to complete the placement").withStyle(ChatFormatting.RED), true);
                return false;
            }
        }

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

    private boolean tryConsumeItems(Inventory inventory, List<ItemStack> required) {
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

    private BlockState getOrientedBlockState(Block block, BlockPlaceContext context) {

        BlockState state = block.getStateForPlacement(context);

        return state != null ? state : block.defaultBlockState();
    }

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
     * checks to see if the player can consume multiple items
     * TODO this can almost certainly be integrated into the consumeMultipleItems method, and it just returns a false if it doesn't work
     * @param inventory player inventory
     * @param block the block to remove from the inventory
     * @param count the number of blocks to remove
     * @return if the items can be consumed or not
     */
    private boolean consumeMultipleItems(Inventory inventory, Block block, int count) {
        ItemStack requiredItem = new ItemStack(block.asItem());
        if (requiredItem.isEmpty()) return false;

        // list so if accessing multiple slots is necessary it can be done
        List<Integer> matchingSlots = new ArrayList<>();
        int itemsFound = 0;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItem(slot, requiredItem)) {
                matchingSlots.add(i);
                itemsFound += slot.getCount();
                if (itemsFound >= count) break;
            }
        }

        if (itemsFound < count) return false;

        int remaining = count;
        for (int slot : matchingSlots) {
            ItemStack stack = inventory.getItem(slot);
            int toRemove = Math.min(remaining, stack.getCount());
            stack.shrink(toRemove);
            remaining -= toRemove;
            if (remaining <= 0) break;
        }

        return true;
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
