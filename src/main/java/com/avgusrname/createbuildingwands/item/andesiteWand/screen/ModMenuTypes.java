package com.avgusrname.createbuildingwands.item.andesiteWand.screen;

import com.avgusrname.createbuildingwands.CreateBuildingWands;

import com.avgusrname.createbuildingwands.item.andesiteWand.screen.multiState.ByteConfigMenu;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.multiState.StandardHalfLayerConfigMenu;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.multiState.SlabConfigMenu;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.multiState.StackedHalfLayerConfigMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, CreateBuildingWands.MODID);
    
    public static final DeferredHolder<MenuType<?>, MenuType<WandConfigMenu>> WAND_CONFIG_MENU =
        registerMenuType("wand_config_menu", WandConfigMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<ByteConfigMenu>> BYTE_CONFIG_MENU =
        registerMenuType("byte_config_menu", ByteConfigMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<SlabConfigMenu>> SLAB_CONFIG_MENU =
            registerMenuType("slab_config_menu", SlabConfigMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<StandardHalfLayerConfigMenu>> STANDARD_HALF_LAYER_CONFIG_MENU =
            registerMenuType("half_layer_config_menu", StandardHalfLayerConfigMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<StackedHalfLayerConfigMenu>> STACKED_HALF_LAYER_CONFIG_MENU =
            registerMenuType("stacked_half_layer_config_menu",StackedHalfLayerConfigMenu::new);
    
    private static <T extends AbstractContainerMenu>DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
