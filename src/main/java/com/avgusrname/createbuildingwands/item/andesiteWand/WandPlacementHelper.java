package com.avgusrname.createbuildingwands.item.andesiteWand;

import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.avgusrname.createbuildingwands.item.WandMode;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandMaterialComponent;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandPlacementContext;
import com.avgusrname.createbuildingwands.util.BlockPlaceHelper;
import com.avgusrname.createbuildingwands.util.WandUtils;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlock;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlockEntity;
import com.copycatsplus.copycats.foundation.copycat.multistate.MaterialItemStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.List;

public class WandPlacementHelper {
    private final AndesiteWandItem andesiteWandItem;
    private final CopycatMaterialHelper copycatMaterialHelper;
    private final WandConsumptionHelper wandConsumptionHelper;

    public WandPlacementHelper(AndesiteWandItem andesiteWandItem, CopycatMaterialHelper copycatMaterialHelper, WandConsumptionHelper wandConsumptionHelper) {
        this.andesiteWandItem = andesiteWandItem;
        this.copycatMaterialHelper = copycatMaterialHelper;
        this.wandConsumptionHelper = wandConsumptionHelper;
    }

    /**
     * places one or multiple blocks
     *
     * @param player the player holding the wand
     * @param pos    the position right-clicked by the player holding the wand
     * @param ctx    more context that contains information for the block placement, exists to cut down on parameters
     * @return true if the placement succeeded, false if not
     */

    boolean placeBlock(ServerPlayer player, BlockPos pos, WandPlacementContext ctx) {

        Level level = player.level();

        if (!level.getBlockState(pos).canBeReplaced()) return false;

        BlockPlaceContext localContext = BlockPlaceContext.at(ctx.blockPlaceContext(), pos, ctx.blockPlaceContext().getClickedFace());
        BlockState stateToPlace = copycatMaterialHelper.getOrientedBlockState(ctx.blockToPlace(), localContext);

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
                finalStateToPlace = CopycatMaterialHelper.applyProperty(finalStateToPlace, prop, value);
            }

            if (!WandConsumptionHelper.isAnyActive(copycatBlock, materialComponent)) {
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

            WandConsumptionHelper.getSeenItems(wandStorage, beStorage);

            copycatMaterialHelper.syncBE(level, pos, targetBE, oldState, finalStateToPlace);
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
            copycatMaterialHelper.applyCopycatMaterial(level, pos, materialState, consumedItem);

            copycatMaterialHelper.syncBE(level, pos, level.getBlockEntity(pos), oldState, finalStateToPlace);
        }
        return true;
    }

    /**
     * places many blocks at a time, according to the user-defined shape
     *
     * @param mode    the shape the player wants to place blocks in, like line, single, plane, etc
     * @param player  player holding the wand
     * @param wand    wand held by the player
     * @param context context or something
     * @return true if the placement succeeded, false if not
     */
    boolean placeMultiple(WandMode mode, ServerPlayer player, ItemStack wand, BlockPlaceContext context) {
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
                    Component.literal("No block configured in wand").withStyle(ChatFormatting.RED), true);
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

        if (!wandConsumptionHelper.consumeItemsForPlacement(player, wand, blockToPlace, positions.size())) return false;

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
}