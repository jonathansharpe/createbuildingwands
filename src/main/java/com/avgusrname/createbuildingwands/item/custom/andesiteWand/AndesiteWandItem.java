package com.avgusrname.createbuildingwands.item.custom.andesiteWand;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.MenuProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlock;
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlockEntity;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlock;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlockEntity;

import javax.annotation.Nullable;

import com.avgusrname.createbuildingwands.component.BlockReferenceComponent;
import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.avgusrname.createbuildingwands.item.custom.WandClientPreview;
import com.avgusrname.createbuildingwands.item.custom.WandMode;
import com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen.WandConfigMenu;
import com.avgusrname.createbuildingwands.util.WandGeometryUtil;

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
                    BlockReferenceComponent copycatComponentClient = heldWand.get(ModDataComponents.WAND_COPYCAT_BLOCK.get());
                    BlockReferenceComponent blockComponentClient = heldWand.get(ModDataComponents.WAND_BLOCK.get());
                    
                    ItemStack selection = ItemStack.EMPTY;
                    if (copycatComponentClient != null && !copycatComponentClient.blockStack().isEmpty()) {
                        selection = copycatComponentClient.blockStack();
                    } else if (blockComponentClient != null && !blockComponentClient.blockStack().isEmpty()) {
                        selection = blockComponentClient.blockStack();
                    }
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
            // checks to make sure the player is not client side, i think this is to ensure the server actually knows about it?
            if (!level.isClientSide()) {
                // calls the openConfig method, passing through the important context
                openConfig(level, player, pContext.getHand());
            }
            return InteractionResult.CONSUME;
        }

        BlockState targetState = Blocks.STONE.defaultBlockState();
        
        // Prefer copycat block if set, otherwise use regular block
        BlockReferenceComponent copycatComponent = heldWand.get(ModDataComponents.WAND_COPYCAT_BLOCK.get());
        BlockReferenceComponent blockComponent = heldWand.get(ModDataComponents.WAND_BLOCK.get());

        // Check copycat block first
        if (copycatComponent != null) {
            ItemStack storedStack = copycatComponent.blockStack();
            if (!storedStack.isEmpty()) {
                Block blockToPlace = Block.byItem(storedStack.getItem());
                if (blockToPlace != Blocks.AIR) {
                    targetState = blockToPlace.defaultBlockState();
                }
            }
        }
        // Fall back to regular block if copycat not set
        else if (blockComponent != null) {
            ItemStack storedStack = blockComponent.blockStack();
            if (!storedStack.isEmpty()) {
                Block blockToPlace = Block.byItem(storedStack.getItem());
                if (blockToPlace != Blocks.AIR) {
                    targetState = blockToPlace.defaultBlockState();
                }
            }
        }

        // Only fail if neither block is set
        if (targetState.is(Blocks.STONE) && 
            (copycatComponent == null || copycatComponent.blockStack().isEmpty()) &&
            (blockComponent == null || blockComponent.blockStack().isEmpty())) {
            return InteractionResult.PASS;
        }

        WandMode currentMode = heldWand.getOrDefault(ModDataComponents.WAND_MODE.get(), WandMode.SINGLE);

        Direction clickedFace = pContext.getClickedFace();

        boolean successfulPlacement = false;
        switch (currentMode) {
            case SINGLE:
                successfulPlacement = placeBlock(level, serverPlayer, heldWand, placeContext);
                break;
            case LINE:
                successfulPlacement = placeMultiple(currentMode, level, serverPlayer, heldWand, placeContext);
                break;
            case PLANE:
                successfulPlacement = placeMultiple(currentMode, level, serverPlayer, heldWand, placeContext);
                break;
            case CUBE:
                successfulPlacement = placeMultiple(currentMode, level, serverPlayer, heldWand, placeContext);
                break;
            case SPHERE:
                successfulPlacement = false;
                break;

        }

        return successfulPlacement ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    /**
     * will place a single block given the information. called once in the single mode, called multiple times when placing any multiple of blocks
     * @param level the minecraft world
     * @param player the player who holds the wand and blocks to consume
     * @param wand the wand that holds the block information, like which blocks to place (copycat or regular)
     * @param clickedPos helps get the face of the block i guess? idk but it does stuff
     * @param face like the other methods, supposed to actually determine the direction of the block but doesn't really do that
     * @return returns true if the block was placed, false if not; need to use better for debugging
     */
    private boolean placeBlock(Level level, ServerPlayer player, ItemStack wand, BlockPlaceContext context) {
        BlockPos placementPos = context.getClickedPos();
        Direction face = context.getClickedFace();

        BlockReferenceComponent regularComponent = wand.get(ModDataComponents.WAND_BLOCK.get());
        BlockReferenceComponent copycatComponent = wand.get(ModDataComponents.WAND_COPYCAT_BLOCK.get());

        // TODO this should actually be allowed, in the case that the player wants to place empty copycat blocks. however an edge case will need to be considered if both slots contain copycat blocks, as you cannot fill one copycat block with another
        boolean useCopycat = copycatComponent != null && regularComponent != null;

        if (regularComponent == null) {
            player.displayClientMessage(
                Component.literal("No block configured in wand").withStyle(ChatFormatting.RED), 
                true
            );
            return false;
        }
        ItemStack regularStack = regularComponent.blockStack();

        if (!(regularStack.getItem() instanceof BlockItem regularBlockItem)) {
            return false;
        }

        BlockState regularState = regularBlockItem.getBlock().defaultBlockState();
        if (useCopycat) {
            ItemStack copycatStack = copycatComponent.blockStack();
            if (!(copycatStack.getItem() instanceof BlockItem copycatBlockItem)) {
                return false;
            }

            Block copycatBlock = copycatBlockItem.getBlock();

            if (!(copycatBlock instanceof CopycatBlock) && !(copycatBlock instanceof ICopycatBlock)) {
                // TODO redo this logic so even attempting to place a non-copycat block into the
                // slot doesn't work, that would lie in WandConfigScreen i believe
                player.displayClientMessage(Component.literal("copycat slot must contain copycat block type"),
                        true);
                return false;
            }

            BlockState copycatState = getOrientedBlockState(copycatBlock, context);

            if (level.setBlock(placementPos, copycatState, 3)) {
                if (!level.isClientSide) {
                    String property = determinePropertyFromFace(copycatState, face);
                    applyCopycatMaterial(level, placementPos, regularState, regularStack, property);
                }
                return true;
            }
            return false;
        }
        else {
            BlockState orientedRegularState = getOrientedBlockState(regularBlockItem.getBlock(), context);
            return level.setBlock(placementPos, orientedRegularState, 3);
        }
    }

    /**
     * places many blocks. the function will either set the start position, or use the existing start position to place blocks to the end position
     * @param mode the mode that defines what helper function to call, that gets a list of BlockPos at which to place the blocks
     * @param level the minecraft world itself where the blocks are placed
     * @param player the player who holds the wand and items to consume
     * @param wand the wand item, which will contain the startpos if it exists
     * @param clickedPos the clicked block position, will either be used to set the startpos or as the endpos to complete the block placement
     * @param face the direction at which the player is looking, but it's not really working i don't think? fix this
     * @return will return a bool if the placement succeeded or failed. will need to utilize this more to better identify problems
     */
    private boolean placeMultiple(WandMode mode, Level level, ServerPlayer player, ItemStack wand, BlockPlaceContext context) {
        // TODO evaluate the usefulness of the `face` parameter, it's supposed to determine the direction at which the blocks are placed (like stairs, logs, etc) but it's not working at the time of writing
        // TODO implement a randomizer functionality, using the create list filter

        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos originalClickedPos = clickedPos;

        System.out.println("=== placeMultiple called on " + (level.isClientSide ? "CLIENT" : "SERVER") + " ===");

        if (level.isClientSide) {
            System.out.println("running on client side, aborting");
            return false;
        }

        BlockReferenceComponent regularComponent = wand.get(ModDataComponents.WAND_BLOCK.get());
        BlockReferenceComponent copycatComponent = wand.get(ModDataComponents.WAND_COPYCAT_BLOCK.get());

        if (regularComponent == null) {
            player.displayClientMessage(
                Component.literal("No block configured in wand").withStyle(ChatFormatting.RED),
                true
            );
            return false;
        }

        boolean useCopycat = copycatComponent != null;

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
            BlockPos endPos = clickedPos.relative(face);

            ItemStack regularStack = regularComponent.blockStack();
            if (!(regularStack.getItem() instanceof BlockItem regularBlockItem)) {
                return false;
            }

            BlockState regularState = regularBlockItem.getBlock().defaultBlockState();
            BlockState copycatState = regularState;
            Block copycatBlock = null;
            Block regularBlock = regularBlockItem.getBlock();

            if (useCopycat) {
                ItemStack copycatStack = copycatComponent.blockStack();
                if (!(copycatStack.getItem() instanceof BlockItem copycatBlockItem)) {
                    return false;
                }
                copycatBlock = copycatBlockItem.getBlock();
                if (!(copycatBlock instanceof CopycatBlock) && !(copycatBlock instanceof ICopycatBlock)) {
                    player.displayClientMessage(
                        Component.literal("Copycat slot must contain copycat block type").withStyle(ChatFormatting.RED),
                        true
                    );
                    return false;
                }
                copycatState = copycatBlock.defaultBlockState();
            }

            Component blockName = useCopycat ?
                copycatComponent.blockStack().getDisplayName() :
                regularStack.getDisplayName();

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

            if (positions.isEmpty()) {
                return false;
            }

            int count = positions.size();

            if (!player.isCreative()) {
                BlockState stateToCheck = useCopycat ? copycatState : regularState;
                if (!canConsumeMultipleItems(player.getInventory(), stateToCheck, count)) {
                    player.displayClientMessage(
                        Component.literal("Need ")
                            .append(Component.literal(String.valueOf(count)).withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(" x "))
                            .append(blockName.copy().withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(" to complete the "))
                            .append(Component.literal(String.valueOf(mode)).withStyle(ChatFormatting.YELLOW))
                            .withStyle(ChatFormatting.RED),
                        true
                    );
                    return false;
                }
                if (useCopycat && !canConsumeMultipleItems(player.getInventory(), regularState, count)) {
                    player.displayClientMessage(
                        Component.literal("Need ")
                            .append(Component.literal(String.valueOf(count)).withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(" x "))
                            .append(blockName.copy().withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(" for copycat materials."))
                            .withStyle(ChatFormatting.RED),
                        true
                    );
                    return false;
                }
            }

            int placedCount = 0;
            List<BlockPos> copycatPositions = new ArrayList<>();

            for (BlockPos pos : positions) {
                System.out.println("Checking position: " + pos);
                System.out.println("Current block at position: " + level.getBlockState(pos));
                System.out.println("Can be replaced: " + level.getBlockState(pos).canBeReplaced());

                if (!level.getBlockState(pos).canBeReplaced()) {
                    System.out.println("Skipping non-replaceable block at " + pos);
                    continue;
                }
                BlockState stateToPlace = useCopycat ?
                    getOrientedBlockState(copycatBlock, context) :
                    getOrientedBlockState(regularBlockItem.getBlock(), context);
                System.out.println("Placing " + stateToPlace + " at " + pos);
                if (level.setBlock(pos, stateToPlace, 3)) {
                    System.out.println("Successfully placed block at " + pos);
                    placedCount++;
                    if (useCopycat) {
                        copycatPositions.add(pos);
                    }
                }
                else {
                    System.out.println("Failed to place block at " + pos);
                }
            }

            System.out.println("Placed " + placedCount + " blocks total");
            System.out.println("Copycat positions to fill: " + copycatPositions.size());

            if (useCopycat && !copycatPositions.isEmpty()) {
                System.out.println("Applying copycat materials on server side: " + !level.isClientSide);
                if (!level.isClientSide) {
                    for (BlockPos pos : copycatPositions) {
                        System.out.println("Applying material to " + pos);

                        BlockState placedCopycatState = level.getBlockState(pos);
                        String property = determinePropertyFromFace(placedCopycatState, face);

                        applyCopycatMaterial(level, pos, regularState, regularStack, property);
                    }
                }
            }

            if (!player.isCreative() && placedCount > 0) {
                BlockState stateToConsume = useCopycat ? copycatState : regularState;
                consumeMultipleItems(player.getInventory(), stateToConsume, placedCount);
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
            BlockPos startPos = clickedPos.relative(face);
            wand.set(ModDataComponents.WAND_START_POS.get(), startPos);

            ItemStack regularStack = regularComponent.blockStack();
            Component blockName = useCopycat ? 
                copycatComponent.blockStack().getDisplayName() :
                regularStack.getDisplayName();
            
            player.displayClientMessage(
                Component.literal("Start position set. Click another location to place a " + mode + " of ")
                    .append(blockName.copy().withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("."))
                    .withStyle(ChatFormatting.AQUA),
                true
            );
            return true;
        }
    }

    private BlockState getOrientedBlockState(Block block, BlockPlaceContext context) {

        BlockState state = block.getStateForPlacement(context);

        return state != null ? state : block.defaultBlockState();
    }

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

        // type checking for a copycats+ copycat

        if (be instanceof IMultiStateCopycatBlockEntity multiStateCopycatBE) {
            System.out.println(">>> Matched IMultiStateCopycatBlockEntity (multistate)");
            String property = specificProperty != null ? specificProperty : multiStateCopycatBE.getBlock().defaultProperty();

            System.out.println("Applying to multistate property: " + property);

            multiStateCopycatBE.setMaterial(property, materialState);
            multiStateCopycatBE.setConsumedItem(property, materialItemStack);
            multiStateCopycatBE.notifyUpdate();
            be.setChanged();

            System.out.println("Applied material to multistate copycat");
        }

        else if (be instanceof ICopycatBlockEntity copycatBE) {
            copycatBE.setMaterial(materialState);
            copycatBE.setConsumedItem(materialItemStack);
            copycatBE.notifyUpdate();
            be.setChanged();
        }
        // type checking for a create copycat
        else if (be instanceof CopycatBlockEntity createCopycatBE) {
            createCopycatBE.setMaterial(materialState);
            createCopycatBE.setConsumedItem(materialItemStack);
            createCopycatBE.notifyUpdate();
            createCopycatBE.setChanged();
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
