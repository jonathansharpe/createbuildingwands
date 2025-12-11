package com.avgusrname.createbuildingwands.networking.packet;

import javax.annotation.Nullable;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.item.custom.WandClientPreview;
import com.avgusrname.createbuildingwands.item.custom.WandMode;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.world.item.ItemStack;

public record SetPreviewStatePacket(@Nullable BlockPos startPos, @Nullable WandMode mode, ItemStack targetStack) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreateBuildingWands.MODID, "set_preview_state");

    public static final Type<SetPreviewStatePacket> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SetPreviewStatePacket> STREAM_CODEC = StreamCodec.of(
        SetPreviewStatePacket::encode, SetPreviewStatePacket::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buf, SetPreviewStatePacket packet) {
        buf.writeBoolean(packet.startPos != null);
        if (packet.startPos != null) {
            buf.writeBlockPos(packet.startPos);
        }

        buf.writeBoolean(packet.mode != null);
        if (packet.mode != null) {
            buf.writeEnum(packet.mode);
        }

        ItemStack.STREAM_CODEC.encode(buf, packet.targetStack);
    }

    private static SetPreviewStatePacket decode(FriendlyByteBuf buf) {
        BlockPos startPos = null;
        if (buf.readBoolean()) {
            startPos = buf.readBlockPos();
        }

        WandMode mode = null;
        if (buf.readBoolean()) {
            mode = buf.readEnum(WandMode.class);
        }

        ItemStack targetStack = buf.readIte

        return new SetPreviewStatePacket(startPos, mode);
    }

    public void handle(IPayloadContext context){
        context.enqueueWork(() -> {
            WandClientPreview.updateActiveState(this.startPos, this.mode);
        });
    }
}
