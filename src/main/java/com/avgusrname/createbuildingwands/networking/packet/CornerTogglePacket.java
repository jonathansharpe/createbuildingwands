package com.avgusrname.createbuildingwands.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CornerTogglePacket(int cornerOrdinal) implements CustomPacketPayload {
    public static final Type<CornerTogglePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("createbuildingwands", "corner_toggle"));

    public static final StreamCodec<FriendlyByteBuf, CornerTogglePacket> CODEC = CustomPacketPayload.codec(CornerTogglePacket::write, CornerTogglePacket::new);

    private CornerTogglePacket(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeInt(this.cornerOrdinal);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
