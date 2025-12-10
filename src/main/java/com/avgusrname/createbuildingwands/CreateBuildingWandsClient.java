package com.avgusrname.createbuildingwands;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.bus.api.SubscribeEvent;

@Mod(value = "createbuildingwands", dist = Dist.CLIENT)
public class CreateBuildingWandsClient {
    public static void onClientSetup(FMLClientSetupEvent event) {
        CreateBuildingWands.LOGGER.info("Client Setup Initialized for Create: Building Wands");
    }
}
