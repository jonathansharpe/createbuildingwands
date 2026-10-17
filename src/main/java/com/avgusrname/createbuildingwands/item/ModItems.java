package com.avgusrname.createbuildingwands.item;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.avgusrname.createbuildingwands.item.andesiteWand.AndesiteWandItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateBuildingWands.MODID);

    public static final DeferredItem<Item> ANDESITE_WAND = ITEMS.register("andesite_wand", 
        () -> new AndesiteWandItem(new Item.Properties().stacksTo(1))
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}