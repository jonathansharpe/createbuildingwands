package com.avgusrname.createbuildingwands.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class WandUtils {
    public static boolean isFullBlock(Block block) {
        BlockState state = block.defaultBlockState();
        return Block.isShapeFullBlock(state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }
}
