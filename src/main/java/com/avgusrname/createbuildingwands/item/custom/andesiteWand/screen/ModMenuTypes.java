package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

import com.avgusrname.createbuildingwands.CreateBuildingWands;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
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

    public static final DeferredHolder<MenuType<?>, MenuType<ByteConfigMenu>> BYTE_CONFIG = 
        MENUS.register("byte_config", () -> IMenuTypeExtension.create((windowId, inv, data) -> {
            InteractionHand hand = data.readEnum(InteractionHand.class);
            return new ByteConfigMenu(windowId, inv, hand);
        }));
    
    private static <T extends AbstractContainerMenu>DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
