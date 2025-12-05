package com.jsharpexyz.createbuildingwands;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jsharpexyz.createbuildingwands.component.ModDataComponents;
import com.jsharpexyz.createbuildingwands.item.custom.andesiteWand.screen.ModMenuTypes;
import com.jsharpexyz.createbuildingwands.item.custom.andesiteWand.screen.WandConfigScreen;
import com.jsharpexyz.createbuildingwands.networking.packet.WandModePacket;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handlers.ClientPayloadHandler;
import net.neoforged.neoforge.network.handlers.ServerPayloadHandler;
import net.neoforged.neoforge.network.handling.ClientPayloadContext;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.client.gui.screens.MenuScreens;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(value = CreateBuildingWands.MODID, dist = Dist.CLIENT)
public class CreateBuildingWands {
    public static final String MODID = "createbuildingwands";

    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public CreateBuildingWands(IEventBus modEventBus) {
        ModCreativeModeTabs.register(modEventBus);

        AllItems.ITEMS.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModMenuTypes.register(modEventBus);
    }
    
    // supposed client side event handler
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientSetupEvents {
        @SubscribeEvent
        public static void registerMenuScreens(RegisterMenuScreensEvent event) {
            LOGGER.info("Attempting to register WandCOnfigScreen");

            event.register(ModMenuTypes.WAND_CONFIG_MENU.get(), WandConfigScreen::new);
        }

        @SubscribeEvent
        public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
            final PayloadRegistrar registrar = event.registrar("1");
            registrar.playBidirectional(
                WandModePacket.TYPE,
                WandModePacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> WandModePacket.handleOnServer(payload, context));
                }
            );
        }
    }

    
}