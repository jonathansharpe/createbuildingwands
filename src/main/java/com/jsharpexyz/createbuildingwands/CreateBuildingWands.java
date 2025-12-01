package com.jsharpexyz.createbuildingwands;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(CreateBuildingWands.MODID)
public class CreateBuildingWands {
    public static final String MODID = "createbuildingwands";

    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public CreateBuildingWands(IEventBus modEventBus) {
        ModCreativeModeTabs.register(modEventBus);

        AllItems.ITEMS.register(modEventBus);
    }
    
}