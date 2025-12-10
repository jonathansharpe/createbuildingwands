package com.avgusrname.createbuildingwands.networking.packet;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.item.custom.WandMode;
import com.avgusrname.createbuildingwands.item.custom.andesiteWand.AndesiteWandItem;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WandModePacket(WandMode newMode, InteractionHand hand) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WandModePacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreateBuildingWands.MODID, "mode_change"));

    public static final StreamCodec<FriendlyByteBuf, WandMode> WAND_MODE_CODEC = 
        StreamCodec.of(FriendlyByteBuf::writeEnum, buf -> buf.readEnum(WandMode.class));

    public static final StreamCodec<FriendlyByteBuf, InteractionHand> INTERACTION_HAND_CODEC = 
        StreamCodec.of(FriendlyByteBuf::writeEnum, buf -> buf.readEnum(InteractionHand.class));

    public static final StreamCodec<FriendlyByteBuf, WandModePacket> STREAM_CODEC = StreamCodec.composite(
        WAND_MODE_CODEC,
        WandModePacket::newMode,
        INTERACTION_HAND_CODEC,
        WandModePacket::hand,
        WandModePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(WandModePacket payload, IPayloadContext context) {
        ServerPlayer sender = (ServerPlayer) context.player();

        if (sender != null) {
            ItemStack wand = sender.getItemInHand(payload.hand());

            if (wand.getItem() instanceof AndesiteWandItem) {
                AndesiteWandItem.setMode(wand, payload.newMode());

                sender.setItemInHand(payload.hand(), wand);
                sender.getInventory().setChanged();
            }
        }
    }
}
