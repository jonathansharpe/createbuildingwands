package com.avgusrname.createbuildingwands.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;

public record BlockReferenceComponent(BlockState storedState) {
    public BlockReferenceComponent {
        if (storedState == null) {
            storedState = Blocks.AIR.defaultBlockState();
        }
    }
    public static final Codec<BlockReferenceComponent> CODEC = BlockState.CODEC.xmap(
        BlockReferenceComponent::new, 
        BlockReferenceComponent::storedState
    );

    public ItemStack storedStateAsItemStack() {
        return new ItemStack(storedState.getBlock());
    }
}
