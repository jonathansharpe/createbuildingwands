package com.jsharpexyz.createbuildingwands.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

public record BlockReferenceComponent(ItemStack blockStack) {
    public static final Codec<BlockReferenceComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStack.CODEC.fieldOf("Item").forGetter(BlockReferenceComponent::blockStack)
    ).apply(instance, BlockReferenceComponent::new));

    public BlockReferenceComponent {
        blockStack = blockStack.copyWithCount(1);
    }

    public static DataComponentType.Builder<BlockReferenceComponent> builder() {
        return DataComponentType.builder();
    }
}
