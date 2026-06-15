package com.avgusrname.createbuildingwands.item.andesiteWand.screen;

import com.copycatsplus.copycats.CCBlocks;
import com.copycatsplus.copycats.content.copycat.bytes.CopycatByteBlock;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlock;
import com.copycatsplus.copycats.foundation.copycat.multistate.MaterialItemStorage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record WandMaterialComponent(CompoundTag materialData, BlockState cornerState) {
    public static WandMaterialComponent createEmpty() {
        return new WandMaterialComponent(new CompoundTag(), CCBlocks.COPYCAT_BYTE.get().defaultBlockState());
    }

    public boolean isActive(String key) {
        return cornerState.getValue(CopycatByteBlock.byByte(CopycatByteBlock.byteMap.get(key)));
    }

    public WandMaterialComponent withCornerActive(String key, boolean active) {
        return new WandMaterialComponent(
                materialData,
                cornerState.setValue(CopycatByteBlock.byByte(CopycatByteBlock.byteMap.get(key)), active)
        );
    }

    public MaterialItemStorage toStorage(HolderLookup.Provider registries) {
        Block block = CCBlocks.COPYCAT_BYTE.get();
        MaterialItemStorage storage = MaterialItemStorage.create(
                ((IMultiStateCopycatBlock) block).storageProperties()
        );
        storage.deserialize(materialData, registries);
        return storage;
    }

    public static WandMaterialComponent fromStorage(MaterialItemStorage storage, BlockState cornerState, HolderLookup.Provider registries) {
        return new WandMaterialComponent(storage.serialize(registries), cornerState);
    }

    public static final Codec<WandMaterialComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CompoundTag.CODEC.fieldOf("material_data").forGetter(WandMaterialComponent::materialData),
                    BlockState.CODEC.fieldOf("corner_state").forGetter(WandMaterialComponent::cornerState)
            ).apply(instance, WandMaterialComponent::new)
    );
}