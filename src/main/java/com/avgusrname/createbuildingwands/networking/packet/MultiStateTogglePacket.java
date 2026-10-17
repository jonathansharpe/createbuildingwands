package com.avgusrname.createbuildingwands.networking.packet;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MultiStateTogglePacket(String key, String value) implements CustomPacketPayload {
    public static final Type<MultiStateTogglePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateBuildingWands.MODID, "wand_config")
    );

    public static final StreamCodec<FriendlyByteBuf, MultiStateTogglePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, MultiStateTogglePacket::key,
                    ByteBufCodecs.STRING_UTF8, MultiStateTogglePacket::value,
                    MultiStateTogglePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
