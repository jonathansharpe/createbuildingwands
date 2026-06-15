package com.avgusrname.createbuildingwands.item.andesiteWand;

import com.avgusrname.createbuildingwands.item.andesiteWand.screen.ByteCornerData;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandMaterialComponent;
import com.avgusrname.createbuildingwands.networking.packet.ForceRedrawPacket;
import com.copycatsplus.copycats.foundation.copycat.multistate.MaterialItemStorage;
import net.minecraft.client.resources.model.Material;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.MenuProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlock;
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlockEntity;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlockEntity;
import com.copycatsplus.copycats.content.copycat.bytes.CopycatByteBlock;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.avgusrname.createbuildingwands.item.WandClientPreview;
import com.avgusrname.createbuildingwands.item.WandMode;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandConfigMenu;
import com.avgusrname.createbuildingwands.util.BlockPlaceHelper;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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
        System.out.println("[WAND DEBUG - SETTER] Attempted to set: " + mode.name() + " | Confirmed Value: " + confirmedMode.name());
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
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level pLevel, Player pPlayer, @NotNull InteractionHand pHand) {
        ItemStack itemStack = pPlayer.getItemInHand(pHand);
        if (pPlayer.isShiftKeyDown()) {
            if (pLevel.isClientSide()) {
                pPlayer.swing(pHand);
            }
            else {
                if (pPlayer instanceof ServerPlayer serverPlayer) {
                    MenuProvider provider = this.getMenuProvider(pHand);

                    serverPlayer.openMenu(
                        provider,
                        buf -> buf.writeEnum(pHand)
                    );
                }
            }
            // openConfig(pLevel, pPlayer, pHand);
            return InteractionResultHolder.consume(itemStack);
        }
        else {
            return InteractionResultHolder.pass(itemStack);
        }
    }

    @Override
    // TODO find a way to extend reach, at a configurable distance like with effortless
    public @NotNull InteractionResult useOn(UseOnContext pContext) {
        // the Level type is seemingly the entire dimension that a player is in. it has fields like max size, world border, etc. i guess we need that so we can place blocks in the world
        Level level = pContext.getLevel();

        // the coordinates of the block
        BlockPos clickedPos = pContext.getClickedPos();

        // ItemStack is an 
        ItemStack heldWand = pContext.getItemInHand();

        // the player is the player, makes sense right
        Player player = pContext.getPlayer();

        Direction clickedFace = pContext.getClickedFace();

        BlockPlaceContext placeContext = new BlockPlaceContext(pContext);

        // Client-side: drive visual preview between clicks
        if (level.isClientSide()) {
            if (player != null && !player.isCrouching()) {
                WandMode currentModeClient = heldWand.getOrDefault(ModDataComponents.WAND_MODE.get(), WandMode.SINGLE);
                Direction clickedFaceClient = pContext.getClickedFace();

                if (heldWand.has(ModDataComponents.WAND_START_POS.get())) {
                    // Second click: clear active preview state; server will handle actual placement
                    WandClientPreview.updateActiveState(null, null);
                    WandClientPreview.clearPreviewPositions();
                } else {
                    // First click: set start position, mode, and preview block for client-side preview
                    BlockPos startPosClient = clickedPos.relative(clickedFaceClient);
                    WandClientPreview.updateActiveState(startPosClient, currentModeClient);

                    // Prefer copycat block for preview if set, otherwise use regular block
                    Block copycat = heldWand.get(ModDataComponents.WAND_COPYCAT_BLOCK.get());
                    Block regular = heldWand.get(ModDataComponents.WAND_BLOCK.get());

                    CreateBuildingWands.LOGGER.info("regular block is: {}", regular);
                    
                    ItemStack selection = (copycat != null && !copycat.defaultBlockState().isAir())
                        ? new ItemStack(copycat.asItem())
                        : (regular != null ? new ItemStack(regular.asItem()) : ItemStack.EMPTY);
                    WandClientPreview.setPreviewBlock(selection);
                }
            }
            return InteractionResult.SUCCESS;
        }

        // presumably this checks to make sure there actually is a player, but shouldn't this be superfluous? how would a wand ever be right clicked if there's no player? idk i will try commenting it out when everything else works
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;

        // checks if the player is shifting, which will bring up the config menu instead. this is the same logic regardless of whether or not the player is looking at a block
        if (player.isCrouching()) {
            openConfig(level, player, pContext.getHand());
            return InteractionResult.CONSUME;
        }

        // Prefer copycat block if set, otherwise use regular block
        WandMode currentMode = heldWand.getOrDefault(ModDataComponents.WAND_MODE.get(), WandMode.SINGLE);
        Block copycatBlock = heldWand.get(ModDataComponents.WAND_COPYCAT_BLOCK.get());
        Block overrideMaterial = heldWand.get(ModDataComponents.WAND_BLOCK.get());

        CreateBuildingWands.LOGGER.info("overrideMaterial (material to apply to copycat) is: {}", overrideMaterial);

        Block blockToPlace;
        boolean isCopycatPlacement = false;
        if (copycatBlock != null && !copycatBlock.defaultBlockState().isAir()) {
            System.out.println("we are placing copycats");
            blockToPlace = copycatBlock;
            isCopycatPlacement = true;
        } else if (overrideMaterial != null && !overrideMaterial.defaultBlockState().isAir()) {
            System.out.println("we are not placing copycats");
            blockToPlace = overrideMaterial;
        } else {
            return InteractionResult.PASS;
        }

        ItemStack materialStack = (overrideMaterial != null) ? new ItemStack(overrideMaterial.asItem()) : ItemStack.EMPTY;
        CreateBuildingWands.LOGGER.info("materialStack is: {}", materialStack);

        boolean successfulPlacement = switch (currentMode) {
            case SINGLE -> {
                BlockPos targetPos = clickedPos.relative(pContext.getClickedFace());
                yield this.placeBlock(level, serverPlayer, targetPos, blockToPlace, materialStack, isCopycatPlacement, clickedFace, placeContext);
            }
            // the below cases all have the same result because placeMultiple handles the mode
            case LINE, PLANE, CUBE -> placeMultiple(currentMode, level, serverPlayer, heldWand, placeContext);
            case SPHERE -> false;
        };

        return successfulPlacement ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    private boolean placeBlock(Level level, ServerPlayer player, BlockPos pos, Block block, ItemStack material, boolean isCopycat, Direction clickedFace, BlockPlaceContext originalContext) {
        //CreateBuildingWands.LOGGER.info("material to place (at top of placeBlock) is: {}", material);
        if (!level.getBlockState(pos).canBeReplaced()) return false;
        if (player == null) return false;

        BlockPlaceContext localContext = BlockPlaceContext.at(originalContext, pos, clickedFace);
        BlockState stateToPlace = getOrientedBlockState(block, localContext);

        if (!isCopycat) {
            return level.setBlock(pos, stateToPlace, Block.UPDATE_ALL);
        }
        WandMaterialComponent materialComponent = player.getItemInHand(InteractionHand.MAIN_HAND)
                .getOrDefault(ModDataComponents.WAND_MATERIALS.get(),WandMaterialComponent.createEmpty());

        BlockState finalStateToPlace = stateToPlace;

        for (BooleanProperty prop : List.of(
                CopycatByteBlock.BOTTOM_NW, CopycatByteBlock.BOTTOM_NE,
                CopycatByteBlock.BOTTOM_SW, CopycatByteBlock.BOTTOM_SE,
                CopycatByteBlock.TOP_NW, CopycatByteBlock.TOP_NE,
                CopycatByteBlock.TOP_SW, CopycatByteBlock.TOP_SE
        )) {
            finalStateToPlace = finalStateToPlace.setValue(prop, materialComponent.cornerState().getValue(prop));
        }

        BlockState oldState = level.getBlockState(pos);
        if (!level.setBlock(pos, finalStateToPlace, Block.UPDATE_NEIGHBORS | Block.UPDATE_KNOWN_SHAPE)) return false;

        BlockEntity targetBE = level.getBlockEntity(pos);
        if (!(targetBE instanceof IMultiStateCopycatBlockEntity copycatMock)) return false;

        copycatMock.init();

        MaterialItemStorage wandStorage = materialComponent.toStorage(player.level().registryAccess());
        MaterialItemStorage beStorage = copycatMock.getMaterialItemStorage();

        for (String key : wandStorage.getAllProperties()) {
            MaterialItemStorage.MaterialItem item = wandStorage.getMaterialItem(key);
            if (item != null && item.hasCustomMaterial()) {
                beStorage.storeMaterialItem(key, item);
            }
        }

        //CreateBuildingWands.LOGGER.info("BE materials IMMEDIATELY after loop: {}", copycatMock.getMaterialItemStorage().getAllMaterials());

        targetBE.setChanged();

        if (level instanceof ServerLevel serverLevel) {
            var syncPacket = targetBE.getUpdatePacket();
            if (syncPacket != null) {
                serverLevel.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false)
                        .forEach(p -> p.connection.send(syncPacket));
            }
            serverLevel.getChunkSource().blockChanged(pos);
            serverLevel.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false)
                    .forEach(p -> PacketDistributor.sendToPlayer(p, new ForceRedrawPacket(pos)));
        }

        level.sendBlockUpdated(pos, oldState, finalStateToPlace, Block.UPDATE_ALL);
        return true;
    }

    /**
     * places many blocks at a time, according to the user-defined shape
     * @param mode the shape the player wants to place blocks in, like line, single, plane, etc
     * @param level the minecraft world
     * @param player player holding the wand
     * @param wand wand held by the player
     * @param context context or something
     * @return if the placement is completed successfully
     */
    private boolean placeMultiple(WandMode mode, Level level, ServerPlayer player, ItemStack wand, BlockPlaceContext context) {
        boolean result = false;
        boolean finished = false;
        // TODO implement a randomizer functionality, using the create shuffle filter mod
        // TODO fix this method so it just uses performPlacement but many times, should inherently fix the problem of the copycat texture not applying
        if (!level.isClientSide) {
            BlockPos clickedPos = context.getClickedPos();
            Direction face = context.getClickedFace();
            Block storedRegularBlock = wand.get(ModDataComponents.WAND_BLOCK.get());
            Block storedCopycatBlock = wand.get(ModDataComponents.WAND_COPYCAT_BLOCK.get());
            if (storedRegularBlock == null) {
                if (storedCopycatBlock != null) {
                    player.displayClientMessage(Component.literal("Utilizing copycat config for block placement").withStyle(ChatFormatting.YELLOW), true);
                } else {
                    player.displayClientMessage(Component.literal("No block configured in wand").withStyle(ChatFormatting.RED), true);
                    finished = true;
                }
            }
            if (!finished) {
                if (player.isShiftKeyDown()) {
                    if (wand.has(ModDataComponents.WAND_START_POS.get())) {
                        wand.remove(ModDataComponents.WAND_START_POS.get());
                        player.displayClientMessage(Component.literal(mode.name() + " selection cancelled.").withStyle(ChatFormatting.YELLOW),true );
                    }
                    result = true;
                } else if (!wand.has(ModDataComponents.WAND_START_POS.get())) {
                    wand.set(ModDataComponents.WAND_START_POS.get(), clickedPos);
                    player.displayClientMessage(Component.literal("Start position set"), true);
                    result = true;
                } else {
                    BlockPos startPos = wand.get(ModDataComponents.WAND_START_POS.get());
                    boolean useCopycat = storedCopycatBlock != null;
                    Block blockToPlace = useCopycat ? storedCopycatBlock : storedRegularBlock;// TODO remove this if statement, this should be handled in the menu so you can't even insert an item thats not a copycat block
                    if (useCopycat && !(storedCopycatBlock instanceof CopycatBlock) && !(storedCopycatBlock instanceof ICopycatBlock)) {
                        player.displayClientMessage(Component.literal("Copycat slot must contain a copycat block type").withStyle(ChatFormatting.RED), true);
                    } else {
                        ItemStack regularStack = ItemStack.EMPTY;
                        if (storedRegularBlock != null) {
                            regularStack = new ItemStack(storedRegularBlock.asItem());
                            // TODO this should also be handled by the slot handler
                            if (!(regularStack.getItem() instanceof BlockItem)) {
                                finished = true;
                            }
                        }
                        if (!finished) {// this should always be true but the IDE was complaining
                            assert startPos != null;// switch block will get list of block positions needed to place the blocks given the shape
                            // TODO implement more shapes
                            List<BlockPos> positions = switch (mode) {
                                case LINE -> BlockPlaceHelper.lineBlockPositions(startPos, clickedPos);
                                case PLANE -> BlockPlaceHelper.planeBlockPositions(startPos, clickedPos, face);
                                case CUBE -> BlockPlaceHelper.cubeBlockPositions(startPos, clickedPos);
                                default -> List.of();
                            };// if the positions list happens to be empty, shouldn't happen but a good failsafe
                            if (!positions.isEmpty()) {// will consume items if player is in survival, with messages if not enough items are available
                                if (!player.isCreative()) {
                                    // TODO logic here will need to be improved as placing 5 blocks of full copycat slabs should consume 10 instead of 5 like this logic indicates
                                    if (!consumeMultipleItems(player.getInventory(), storedRegularBlock, positions.size())) {
                                        player.displayClientMessage(
                                                Component.literal("Not enough blocks to complete the placement").withStyle(ChatFormatting.RED), true);
                                        finished = true;
                                    } else if (useCopycat && !consumeMultipleItems(player.getInventory(), storedRegularBlock, positions.size()) && !consumeMultipleItems(player.getInventory(), storedCopycatBlock, positions.size())) {
                                        player.displayClientMessage(
                                                Component.literal("Not enough material blocks to complete the placement").withStyle(ChatFormatting.RED), true);
                                        finished = true;
                                    }
                                }
                                if (!finished) {// get a placed counter, which will go up based on what blocks are actually placed. note that the area could be bigger than the blocks actually modified, so we can't just use positions.size()
                                    int placedCount = 0;
                                    for (BlockPos pos : positions) {
                                        if (placeBlock(level, player, pos, blockToPlace, regularStack, useCopycat, face, context)) {
                                            placedCount++;
                                        }
                                    }// remove the start position since we've placed blocks now
                                    wand.remove(ModDataComponents.WAND_START_POS.get());// message that blocks were placed
                                    player.displayClientMessage(
                                            Component.literal("Placed " + placedCount + " blocks in a " + mode.name().toLowerCase())
                                                    .withStyle(ChatFormatting.GREEN), true);
                                    result = placedCount > 0;
                                }
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private BlockState getOrientedBlockState(Block block, BlockPlaceContext context) {

        BlockState state = block.getStateForPlacement(context);

        return state != null ? state : block.defaultBlockState();
    }

    /**
     * applies the copycat material for the given block information
     * TODO this does not work with the multi-state copycats, plz fix
     * @param level the minecraft world itself
     * @param pos the position at which to apply the copycat material
     * @param materialState the BlockState of the block texture to apply to the copycat
     * @param materialItemStack the itemstack of the block, so the item can be placed inside the placed copycat block to retain the texture upon relog
     */
    private void applyCopycatMaterial(Level level, BlockPos pos, BlockState materialState, ItemStack materialItemStack, String specificProperty) {
        BlockEntity be = level.getBlockEntity(pos);

        // this should never happen? if the copycat block is null this shouldn't even be triggered but safe checking i guess
        if (be == null) return;
        BlockState actualState = level.getBlockState(pos);


        // type checking for a copycats+ copycat
        CreateBuildingWands.LOGGER.info("BE Class: {}", be.getClass().getName());
        CreateBuildingWands.LOGGER.info("Interfaces: {}", java.util.Arrays.toString(be.getClass().getInterfaces()));

        switch (be) {
            case IMultiStateCopycatBlockEntity multiStateCopycatBE -> {
                if (materialState.getBlock() == level.getBlockState(pos).getBlock()) {
                    return;
                }
                System.out.println(">>> Matched IMultiStateCopycatBlockEntity (multistate)");
                String property = specificProperty != null ? specificProperty : multiStateCopycatBE.getBlock().defaultProperty();

                System.out.println("Applying to multistate property: " + property);

                multiStateCopycatBE.setMaterial(property, materialState);
                multiStateCopycatBE.setConsumedItem(property, materialItemStack);
                multiStateCopycatBE.notifyUpdate();
                be.setChanged();
                level.sendBlockUpdated(pos, actualState, actualState, 3);

                System.out.println("Applied material to multistate copycat");
            }
            case ICopycatBlockEntity copycatBE -> {
                CreateBuildingWands.LOGGER.info("BE class is type: {}", be);
                CreateBuildingWands.LOGGER.info("materialState.getBlock() is: {}", materialState.getBlock());
                CreateBuildingWands.LOGGER.info("level.getBlockState(pos).getBlock() is: {}", level.getBlockState(pos).getBlock());
                if (materialState.getBlock() == level.getBlockState(pos).getBlock()) {
                    return;
                }
                CreateBuildingWands.LOGGER.info("materialState value is: {}", materialState);
                CreateBuildingWands.LOGGER.info("materialItemStack value is: {}", materialItemStack);

                copycatBE.setMaterial(materialState);
                copycatBE.setConsumedItem(materialItemStack);
                copycatBE.notifyUpdate();
                be.setChanged();
                level.sendBlockUpdated(pos, actualState, actualState, 3);
            }
            // type checking for a create copycat
            case CopycatBlockEntity createCopycatBE -> {
                if (materialState.getBlock() == level.getBlockState(pos).getBlock()) {
                    return;
                }
                createCopycatBE.setMaterial(materialState);
                createCopycatBE.setConsumedItem(materialItemStack);
                createCopycatBE.notifyUpdate();
                createCopycatBE.setChanged();
                level.sendBlockUpdated(pos, actualState, actualState, 3);
            }
            // this should never happen
            default -> System.out.println("ERROR: block entity does not match a copycat type");
        }
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
