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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record WandMaterialComponent(CompoundTag materialData, Set<String> activeParts) {
    public static WandMaterialComponent createEmptyDefault() {
        return new WandMaterialComponent(new CompoundTag(), new HashSet<>());
    }

    public boolean isActive(String key) {
        return activeParts.contains(key);
    }

    public WandMaterialComponent withCornerActive(String key, boolean active) {
        Set<String> updated = new HashSet<>(activeParts);
        if (active) updated.add(key);
        else updated.remove(key);
        return new WandMaterialComponent(materialData, updated);
    }

    public MaterialItemStorage toStorage(HolderLookup.Provider registries) {
        Block block = CCBlocks.COPYCAT_BYTE.get();
        MaterialItemStorage storage = MaterialItemStorage.create(
                ((IMultiStateCopycatBlock) block).storageProperties()
        );
        storage.deserialize(materialData, registries);
        return storage;
    }

    public static WandMaterialComponent fromStorage(MaterialItemStorage storage, Set<String> activeParts, HolderLookup.Provider registries) {
        return new WandMaterialComponent(storage.serialize(registries), activeParts);
    }

    public static final Codec<WandMaterialComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CompoundTag.CODEC.fieldOf("material_data").forGetter(WandMaterialComponent::materialData),
                    Codec.STRING.listOf()
                            .xmap(list -> (Set<String>) new HashSet<>(list), list -> new ArrayList<>(list))
                            .fieldOf("active_parts")
                            .forGetter(WandMaterialComponent::activeParts)
            ).apply(instance, WandMaterialComponent::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, WandMaterialComponent> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG, WandMaterialComponent::materialData,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(HashSet::new, List::copyOf), WandMaterialComponent::activeParts,
                    WandMaterialComponent::new
            );
}