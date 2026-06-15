package com.avgusrname.createbuildingwands;


import com.avgusrname.createbuildingwands.item.ModItems;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.*;
import com.avgusrname.createbuildingwands.networking.packet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.avgusrname.createbuildingwands.item.WandClientPreview;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.world.entity.player.Player;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(value = CreateBuildingWands.MODID, dist = Dist.CLIENT)
public class CreateBuildingWands {
    public static final String MODID = "createbuildingwands";

    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public CreateBuildingWands(IEventBus modEventBus) {
        ModCreativeModeTabs.register(modEventBus);

        ModItems.ITEMS.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        LOGGER.info("Hello from Create Building Wands!");
    }
    
    // supposed client side event handler
    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientSetupEvents {

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                LOGGER.info("Client setup complete. Ensuring WandClientPreview is loaded...");
                WandClientPreview.clearPreviewPositions();
            });
        }

        @SubscribeEvent
        public static void registerMenuScreens(RegisterMenuScreensEvent event) {
            LOGGER.info("Registering WandConfigScreen...");
            event.register(
                ModMenuTypes.WAND_CONFIG_MENU.get(), 
                WandConfigScreen::new
            );
            event.register(
                ModMenuTypes.BYTE_CONFIG_MENU.get(),
                ByteConfigScreen::new
            );
        }

        @SubscribeEvent
        public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
            final PayloadRegistrar registrar = event.registrar("1");
            registrar.playBidirectional(
                WandPacket.TYPE,
                WandPacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> WandPacket.handleOnServer(payload, context));
                }
            );

            registrar.playBidirectional(
                WandPreviewPacket.TYPE,
                WandPreviewPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> WandPreviewPacket.handleOnClient(payload, context))
            );

            registrar.playToClient(
                    ForceRedrawPacket.TYPE,
                    ForceRedrawPacket.CODEC,
                    ForceRedrawPacketHandler::handle
            );

            registrar.playBidirectional(
                CornerTogglePacket.TYPE, 
                CornerTogglePacket.CODEC, 
                (payload, context) -> context.enqueueWork(() -> {
                    Player player = context.player();

                    if (player.containerMenu instanceof ByteConfigMenu menu) {
                        menu.handleServerToggle(payload.cornerOrdinal());
                    } else {
                        CreateBuildingWands.LOGGER.warn("Packet dropped! Player container menu is NOT instance of ByteConfigMenu. Active Container: {}", player.containerMenu.getClass().getSimpleName());
                    }
                })
            );

            LOGGER.info("Networking Payloads Registered directly in main mod class.");
        }
    }

    
}