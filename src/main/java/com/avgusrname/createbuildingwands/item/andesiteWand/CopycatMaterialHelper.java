package com.avgusrname.createbuildingwands.item.andesiteWand;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.networking.packet.ForceRedrawPacket;
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlockEntity;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlockEntity;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.network.PacketDistributor;

public class CopycatMaterialHelper {
    public CopycatMaterialHelper() {
    }

    /**
     * applies properties using a map function thingy
     *
     * @param state existing state of the state
     * @param prop  properties to apply
     * @param value values of the properties to apply
     * @param <T>   type varies i guess
     * @return the properties after modification
     */
    static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> prop, String value) {
        return prop.getValue(value)
                .map(v -> state.setValue(prop, v))
                .orElse(state);
    }

    /**
     * modifies the block entity at the specified position to match the updated state
     *
     * @param level    the minecraft world (needed context)
     * @param pos      the block position to modify
     * @param be       the block entity to modify
     * @param oldState the previous block state
     * @param newState the new block state to update the old one to
     */
    void syncBE(Level level, BlockPos pos, BlockEntity be, BlockState oldState, BlockState newState) {
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
     * gets the block state necessary for placement.
     *
     * @param block   the block attempting to be placed
     * @param context the context surrounding the block
     * @return the block state of the block
     */
    BlockState getOrientedBlockState(Block block, BlockPlaceContext context) {

        BlockState state = block.getStateForPlacement(context);

        return state != null ? state : block.defaultBlockState();
    }

    /**
     * applies the copycat material properties after block placement
     *
     * @param level         the minecraft world
     * @param pos           position of block to modify
     * @param materialState state to apply to the block
     * @param consumedItem  items to consume
     */
    void applyCopycatMaterial(Level level, BlockPos pos, BlockState materialState, ItemStack consumedItem) {
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
}