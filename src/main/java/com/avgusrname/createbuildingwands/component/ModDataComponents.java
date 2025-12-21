package com.avgusrname.createbuildingwands.component;

import java.util.function.UnaryOperator;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.item.custom.WandMode;
import com.avgusrname.createbuildingwands.networking.packet.MultiStateConfiguration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = 
        DeferredRegister.createDataComponents(CreateBuildingWands.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WandMode>> WAND_MODE = register("wand_mode",
        builder -> builder.persistent(WandMode.CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> WAND_START_POS = register("wand_start_pos", builder -> builder.persistent(BlockPos.CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockReferenceComponent>> WAND_BLOCK = register("wand_block",
        builder -> builder.persistent(BlockReferenceComponent.CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockReferenceComponent>> WAND_COPYCAT_BLOCK = register("wand_copycat_block",
        builder -> builder.persistent(BlockReferenceComponent.CODEC));
    
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MultiStateConfiguration>> MULTI_STATE_CONFIG = register("multi_state_config",
        builder -> builder
            .persistent(MultiStateConfiguration.CODEC)
            .networkSynchronized(MultiStateConfiguration.STREAM_CODEC)
        );

    private static <T>DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return DATA_COMPONENT_TYPES.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}