package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllBlocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public record ByteCornerData(BlockState material, boolean enableCT, ItemStack consumedItem, boolean isActive) {
    public static final Codec<ByteCornerData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockState.CODEC.fieldOf("Material").forGetter(ByteCornerData::material),
                    Codec.BOOL.fieldOf("EnableCT").forGetter(ByteCornerData::enableCT),
                    ItemStack.OPTIONAL_CODEC.fieldOf("Item").forGetter(ByteCornerData::consumedItem),
                    Codec.BOOL.fieldOf("IsActive").forGetter(ByteCornerData::isActive)
            ).apply(instance, ByteCornerData::new)
    );

    public static ByteCornerData defaultEmpty() {
        return new ByteCornerData(
                AllBlocks.COPYCAT_BASE.getDefaultState(),
                false,
                ItemStack.EMPTY,
                false
        );
    }
    public enum Corner {
        BOTTOM_NW,BOTTOM_NE,BOTTOM_SW,BOTTOM_SE,TOP_NW,TOP_NE,TOP_SW,TOP_SE
    }
}
