package com.avgusrname.createbuildingwands.item.custom.andesiteWand;

import com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen.ByteCornerData;
import com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen.WandMaterialComponent;
import com.avgusrname.createbuildingwands.networking.packet.ForceRedrawPacket;
import com.copycatsplus.copycats.utility.BlockEntityUtils;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.blockEntity.IMergeableBE;
import it.unimi.dsi.fastutil.bytes.Byte2CharOpenCustomHashMap;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.fixes.BlockEntityCustomNameToComponentFix;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlock;
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlockEntity;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlock;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlockEntity;
import com.copycatsplus.copycats.foundation.copycat.multistate.MultiStateCopycatBlock;
import com.copycatsplus.copycats.foundation.copycat.multistate.MultiStateCopycatBlockEntity;
import com.copycatsplus.copycats.content.copycat.bytes.CopycatByteBlock;

import javax.annotation.Nullable;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.avgusrname.createbuildingwands.item.custom.WandClientPreview;
import com.avgusrname.createbuildingwands.item.custom.WandMode;
import com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen.ByteConfigMenu;
import com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen.WandConfigMenu;
import com.avgusrname.createbuildingwands.util.WandGeometryUtil;
import net.neoforged.neoforge.network.PacketDistributor;
import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.List;

public class AndesiteWandItem extends Item {

    public static final String SELECTED_BLOCK_TAG_KEY = "WandSelectedBlock";

    public AndesiteWandItem(Properties properties) {
        super(properties);
    }

    /**
     * gets the wand mode, used to determine what shape of blocks
     * @param wand the wand
     * @return the current wand mode, yeah
     */
    public static WandMode getMode(ItemStack wand) {
        WandMode mode = wand.getOrDefault(ModDataComponents.WAND_MODE.get(), WandMode.SINGLE);
        System.out.println("[WAND DEBUG] Getter returning: " + mode.name());
        return mode;
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
                public Component getDisplayName() {
					return Component.literal("Wand Configuration");
				}

                // this makes the menu giving the important info. the level is seemingly no longer relevant since at this point we're in a menu, and not interacting with the world, but merely the players inventory and the menu itself.
                @Override
                @Nullable
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    return new WandConfigMenu(id, inv, hand);
                }
            };

            // this then opens the menu we just created
            serverPlayer.openMenu(containerProvider);
        }
    }
    
    public static void openByteConfig(Player player, InteractionHand hand) {
        player.openMenu(new SimpleMenuProvider((containerId, playerInventory, playerEntity) -> {
            return new ByteConfigMenu(containerId, playerInventory, hand);
        }, Component.literal("Copycat Byte Configuration")));
        CreateBuildingWands.LOGGER.info("[WandDebug openByteConfig] opening byte config");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
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
    public InteractionResult useOn(UseOnContext pContext) {
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
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

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
        }
        else if (overrideMaterial != null && !overrideMaterial.defaultBlockState().isAir()) {
            System.out.println("we are not placing copycats");
            blockToPlace = overrideMaterial;
        }
        else {
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

    private String getPropertyKeyFromCorner(ByteCornerData.Corner corner) {
        return switch(corner) {
            case BOTTOM_NW -> "bottom_northwest";
            case BOTTOM_NE -> "bottom_northeast";
            case BOTTOM_SW -> "bottom_southwest";
            case BOTTOM_SE -> "bottom_southeast";
            case TOP_NW    -> "top_northwest";
            case TOP_NE    -> "top_northeast";
            case TOP_SW    -> "top_southwest";
            case TOP_SE    -> "top_southeast";
        };
    }

    private BooleanProperty getPropFromCorner(ByteCornerData.Corner corner) {
        return switch(corner) {
            case BOTTOM_NW -> CopycatByteBlock.BOTTOM_NW;
            case BOTTOM_NE -> CopycatByteBlock.BOTTOM_NE;
            case BOTTOM_SW -> CopycatByteBlock.BOTTOM_SW;
            case BOTTOM_SE -> CopycatByteBlock.BOTTOM_SE;
            case TOP_NW    -> CopycatByteBlock.TOP_NW;
            case TOP_NE    -> CopycatByteBlock.TOP_NE;
            case TOP_SW    -> CopycatByteBlock.TOP_SW;
            case TOP_SE    -> CopycatByteBlock.TOP_SE;
        };
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
                .getOrDefault(ModDataComponents.WAND_MATERIALS.get(),WandMaterialComponent.createEmptyDefault());

        BlockState finalStateToPlace = stateToPlace;

        for (ByteCornerData.Corner corner : ByteCornerData.Corner.values()) {
            finalStateToPlace = finalStateToPlace.setValue(
                    getPropFromCorner(corner),
                    materialComponent.corners().get(corner.ordinal()).isActive()
            );
        }

        BlockState oldState = level.getBlockState(pos);
        if (!level.setBlock(pos, finalStateToPlace, Block.UPDATE_NEIGHBORS | Block.UPDATE_KNOWN_SHAPE)) return false;

        BlockEntity targetBE = level.getBlockEntity(pos);
        if (!(targetBE instanceof IMultiStateCopycatBlockEntity copycatMock)) return false;

        copycatMock.init();

        for (ByteCornerData.Corner corner : ByteCornerData.Corner.values()) {
            ByteCornerData cornerData = materialComponent.corners().get(corner.ordinal());
            if (!cornerData.isActive()) continue;

            String propertyKey = getPropertyKeyFromCorner(corner);
            CreateBuildingWands.LOGGER.info("Setting material for key: {} to: {}", propertyKey, cornerData.material());

            copycatMock.setMaterial(propertyKey, cornerData.material());
            copycatMock.setConsumedItem(propertyKey, cornerData.consumedItem());
            copycatMock.setEnableCT(propertyKey, cornerData.enableCT());
        }

        CreateBuildingWands.LOGGER.info("BE materials IMMEDIATELY after loop: {}", copycatMock.getMaterialItemStorage().getAllMaterials());

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

        // this should never happen but the compiler was complaining
    }

    /**
     * places many blocks at a time, according to the user-defined shape
     * @param mode
     * @param level
     * @param player
     * @param wand
     * @param context
     * @return
     */
    private boolean placeMultiple(WandMode mode, Level level, ServerPlayer player, ItemStack wand, BlockPlaceContext context) {
        // TODO implement a randomizer functionality, using the create shuffle filter mod
        // TODO fix this method so it just uses performPlacement but many times, should inherently fix the problem of the copycat texture not applying
        if (level.isClientSide) return false;

        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();

        Block storedRegularBlock = wand.get(ModDataComponents.WAND_BLOCK.get());
        Block storedCopycatBlock = wand.get(ModDataComponents.WAND_COPYCAT_BLOCK.get());

        if (storedRegularBlock == null) {
            player.displayClientMessage(Component.literal("No block configured in wand").withStyle(ChatFormatting.RED), true);
            return false;
        }

        boolean useCopycat = storedCopycatBlock != null;

        if (player.isShiftKeyDown()) {
            if (wand.has(ModDataComponents.WAND_START_POS.get())) {
                wand.remove(ModDataComponents.WAND_START_POS.get());
                player.displayClientMessage(
                    Component.literal(mode.name() + " selection cancelled.").withStyle(ChatFormatting.YELLOW),
                    true
                );
            }
            return true;
        }

        if (wand.has(ModDataComponents.WAND_START_POS.get())) {
            BlockPos startPos = wand.get(ModDataComponents.WAND_START_POS.get());
            BlockPos endPos = clickedPos;

            Block blockToPlace = useCopycat ? storedCopycatBlock : storedRegularBlock;
            ItemStack regularStack = new ItemStack(storedRegularBlock.asItem());
            if (!(regularStack.getItem() instanceof BlockItem regularBlockItem)) {
                return false;
            }

            BlockState regularState = regularBlockItem.getBlock().defaultBlockState();

            BlockState masterState = getOrientedBlockState(blockToPlace, context);

            if (useCopycat && !(storedCopycatBlock instanceof CopycatBlock) && !(storedCopycatBlock instanceof ICopycatBlock)) {
                player.displayClientMessage(
                        Component.literal("Copycat slot must contain copycat block type").withStyle(ChatFormatting.RED),
                        true);
                return false;
            }

            List<BlockPos> positions = switch(mode) {
                case LINE -> WandGeometryUtil.lineBlockPositions(startPos, endPos);
                case PLANE -> WandGeometryUtil.planeBlockPositions(startPos, endPos, face);
                case CUBE -> WandGeometryUtil.cubeBlockPositions(startPos, endPos);
                case SPHERE -> new ArrayList<>();
                default -> new ArrayList<>();
            };

            System.out.println("Start pos: " + startPos);
            System.out.println("End pos: " + endPos);
            System.out.println("Calculated positions: " + positions);
            System.out.println("Attempting to place " + positions.size() + " blocks");

            if (positions.isEmpty()) { return false; }
            int count = positions.size();

            if (!player.isCreative()) {
                if (!canConsumeMultipleItems(player.getInventory(), masterState, count)) {
                    player.displayClientMessage(
                        Component.literal("Not enough blocks to complete the copycat placement"),
                        true
                    );
                    return false;
                }
                if (useCopycat && !canConsumeMultipleItems(player.getInventory(), regularState, count)) {
                    player.displayClientMessage(
                        Component.literal("Not enough blocks to complete the placement"),
                        true
                    );
                    return false;
                }
            }

            int placedCount = 0;

            for (BlockPos pos : positions) {
                System.out.println("Checking position: " + pos);
                System.out.println("Current block at position: " + level.getBlockState(pos));
                System.out.println("Can be replaced: " + level.getBlockState(pos).canBeReplaced());

                placeBlock(level, player, pos, blockToPlace, regularStack, useCopycat, face, context);
                placedCount++;
            }

            System.out.println("Placed " + placedCount + " blocks total");

            if (!player.isCreative() && placedCount > 0) {
                consumeMultipleItems(player.getInventory(), masterState, placedCount);
                if (useCopycat) {
                    consumeMultipleItems(player.getInventory(), regularState, placedCount);
                }
            }

            wand.remove(ModDataComponents.WAND_START_POS.get());

            player.displayClientMessage(
                Component.literal("Placed " + placedCount + " blocks in a " + mode).withStyle(ChatFormatting.GREEN),
                true
            );
            return true;
        }
        // set start position
        else {
            wand.set(ModDataComponents.WAND_START_POS.get(), clickedPos);

            String blockName = useCopycat ? storedCopycatBlock.getName().getString() : storedRegularBlock.getName().getString();
            player.displayClientMessage(Component.literal("Start position set"), true);
            return true;
        }
    }

    private BlockState getOrientedBlockState(Block block, BlockPlaceContext context) {

        BlockState state = block.getStateForPlacement(context);

        return state != null ? state : block.defaultBlockState();
    }

    // TODO this method may not be needed anymore?
    private String determinePropertyFromFace(BlockState state, Direction face) {
        Block block = state.getBlock();

        if (state.hasProperty(SlabBlock.TYPE)) {
            SlabType slabType = state.getValue(SlabBlock.TYPE);
            if (slabType == SlabType.TOP) {
                return "top";
            }
            else if (slabType == SlabType.BOTTOM) {
                return "bottom";
            }
            else {
                return "block";
            }
        }
        
        return null;
    }

    /**
     * applies the copycat material for the given block information
     * @param level the minecraft world itself
     * @param pos the position at which to apply the copycat material
     * @param materialState the BlockState of the block texture to apply to the copycat
     * @param materialItemStack the itemstack of the block, so the item can be placed inside the placed copycat block to retain the texture upon relog
     */
    // TODO this does not work with the multi-state copycats, plz fix
    private void applyCopycatMaterial(Level level, BlockPos pos, BlockState materialState, ItemStack materialItemStack, String specificProperty) {
        BlockEntity be = level.getBlockEntity(pos);

        // this should never happen? if the copycat block is null this shouldn't even be triggered but safe checking i guess
        if (be == null) return;
        BlockState actualState = level.getBlockState(pos);


        // type checking for a copycats+ copycat
        CreateBuildingWands.LOGGER.info("BE Class: {}", be.getClass().getName());
        CreateBuildingWands.LOGGER.info("Interfaces: {}", java.util.Arrays.toString(be.getClass().getInterfaces()));

        if (be instanceof IMultiStateCopycatBlockEntity multiStateCopycatBE) {
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

        else if (be instanceof ICopycatBlockEntity copycatBE) {
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
        else if (be instanceof CopycatBlockEntity createCopycatBE) {
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
        else {
            System.out.println("ERROR: block entity does not match a copycat type");
        }
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().blockChanged(pos);
        }
    }

    /**
     * checks to see if the player can consume multiple items
     * TODO this can almost certainly be integrated into the consumeMultipleItems method, and it just returns a false if it doesn't work
     * @param inventory player inventory
     * @param blockState the block to remove from the inventory
     * @param count the number of blocks to remove
     * @return
     */
    private boolean canConsumeMultipleItems(Inventory inventory, BlockState blockState, int count) {
        Item requiredItem = blockState.getBlock().asItem();
        int itemsFound = 0;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == requiredItem) {
                itemsFound += stack.getCount();
                if (itemsFound >= count) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * will consume the items from the inventory
     * TODO rework this to call a consumeItem() method in a loop instead
     * TODO will also need to be reworked when the randomizer is implemented
     * @param inventory the player inventory
     * @param blockState the block state of the item to remove from the player inventory
     * @param count the number of blocks to remove
     */
    private void consumeMultipleItems(Inventory inventory, BlockState blockState, int count) {
        Item requiredItem = blockState.getBlock().asItem();
        int remainingToConsume = count;

        for (int i = 0; i < inventory.getContainerSize() && remainingToConsume > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == requiredItem) {
                int toTake = Math.min(stack.getCount(), remainingToConsume);
                stack.shrink(toTake);
                remainingToConsume -= toTake;
            }
            inventory.setChanged();
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
            public Component getDisplayName() {
                return Component.translatable("screen.createbuildingwands.wand_config");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new WandConfigMenu(id, inv, pHand);
            }
        };
    }
}
