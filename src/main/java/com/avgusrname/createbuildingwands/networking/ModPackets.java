package com.avgusrname.createbuildingwands.networking;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class ModPackets {
    public static <T extends CustomPacketPayload> void sendToClient(ServerPlayer player, T payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}