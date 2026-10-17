package com.avgusrname.createbuildingwands.networking.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ForceRedrawPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<ForceRedrawPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("createbuildingwands", "force_redraw")
    );

    public static final StreamCodec<FriendlyByteBuf, ForceRedrawPacket> CODEC =
            CustomPacketPayload.codec(ForceRedrawPacket::write, ForceRedrawPacket::new);

    private ForceRedrawPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
