package com.avgusrname.createbuildingwands.item.andesiteWand.screen;

import com.copycatsplus.copycats.CCBlocks;
import com.copycatsplus.copycats.content.copycat.bytes.CopycatByteBlock;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlock;
import com.copycatsplus.copycats.foundation.copycat.multistate.MaterialItemStorage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public record WandMaterialComponent(CompoundTag materialData, Map<String, String> blockStateProps) {
    public static WandMaterialComponent createEmptyDefault() {
        return new WandMaterialComponent(new CompoundTag(), new HashMap<>());
    }

    public boolean isActive(String key) {
        return "true".equals(blockStateProps.get(key));
    }

    public WandMaterialComponent withBlockStateProp(String key, String value) {
        Map<String, String> updated = new HashMap<>(blockStateProps);
        updated.put(key, value);
        return new WandMaterialComponent(materialData, updated);
    }

    public WandMaterialComponent withPartActive(String key, boolean active) {
        return withBlockStateProp(key, String.valueOf(active));
    }

    public MaterialItemStorage toStorage(HolderLookup.Provider registries) {
        Block block = CCBlocks.COPYCAT_BYTE.get();
        MaterialItemStorage storage = MaterialItemStorage.create(
                ((IMultiStateCopycatBlock) block).storageProperties()
        );
        storage.deserialize(materialData, registries);
        return storage;
    }

    public static WandMaterialComponent fromStorage(MaterialItemStorage storage, Map<String, String> blockStateProps, HolderLookup.Provider registries) {
        return new WandMaterialComponent(storage.serialize(registries), blockStateProps);
    }

    public static final Codec<WandMaterialComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CompoundTag.CODEC.fieldOf("material_data").forGetter(WandMaterialComponent::materialData),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("block_state_props").forGetter(WandMaterialComponent::blockStateProps)
            ).apply(instance, WandMaterialComponent::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, WandMaterialComponent> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG, WandMaterialComponent::materialData,
                    ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8), WandMaterialComponent::blockStateProps,
                    WandMaterialComponent::new
            );
}