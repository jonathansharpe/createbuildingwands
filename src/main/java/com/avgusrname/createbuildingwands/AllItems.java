package com.avgusrname.createbuildingwands;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.avgusrname.createbuildingwands.item.andesiteWand.AndesiteWandItem;

public class AllItems {
    public static final DeferredRegister.Items ITEMS = 
        DeferredRegister.createItems(CreateBuildingWands.MODID);

    public static final DeferredItem<Item> ANDESITE_WAND = ITEMS.register("andesite_wand", 
        () -> new AndesiteWandItem(new Item.Properties().stacksTo(1))
    );

}