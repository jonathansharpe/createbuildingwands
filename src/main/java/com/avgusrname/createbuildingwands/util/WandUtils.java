package com.avgusrname.createbuildingwands.util;

import com.avgusrname.createbuildingwands.component.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class WandUtils {
    public static boolean isFullBlock(Block block) {
        BlockState state = block.defaultBlockState();
        return Block.isShapeFullBlock(state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }
    public static InteractionHand deserializeHand(FriendlyByteBuf pExtraData) {
        if (pExtraData != null) {
            return pExtraData.readEnum(InteractionHand.class);
        }
        return InteractionHand.MAIN_HAND;
    }
    public static Block getCopycatBlock(ItemStack wandStack) {
        return wandStack.get(ModDataComponents.WAND_BLOCK_COPYCAT.get());
    }
}
