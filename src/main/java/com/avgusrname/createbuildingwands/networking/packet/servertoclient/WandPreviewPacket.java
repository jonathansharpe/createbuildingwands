package com.avgusrname.createbuildingwands.networking.packet.servertoclient;

import javax.annotation.Nullable;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.item.custom.WandClientPreview;
import com.avgusrname.createbuildingwands.item.custom.WandMode;
import com.avgusrname.createbuildingwands.networking.ModPackets;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record WandPreviewPacket(List<BlockPos> positions, Optional<ItemStack> targetStack) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WandPreviewPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreateBuildingWands.MODID, "wand_preview"));

    public static final StreamCodec<ByteBuf, List<BlockPos>> POSITIONS_CODEC =
        BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list());

    public static final StreamCodec<RegistryFriendlyByteBuf, Optional<ItemStack>> TARGET_STACK_CODEC =
        ByteBufCodecs.optional(ItemStack.OPTIONAL_STREAM_CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, WandPreviewPacket> STREAM_CODEC = StreamCodec.composite(
        POSITIONS_CODEC,
        WandPreviewPacket::positions,
        TARGET_STACK_CODEC,
        WandPreviewPacket::targetStack,
        WandPreviewPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnClient(WandPreviewPacket payload, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                if (Minecraft.getInstance().player == null) {
                    return;
                }

                if (payload.positions().isEmpty() || payload.targetStack().isEmpty()) {
                    CreateBuildingWands.LOGGER.debug("Received WandPreviewPacket to clear preview");
                    WandClientPreview.clearPreviewPositions();
                } else {
                    CreateBuildingWands.LOGGER.debug("Received WandPreviewPacket to set preview with {} positions", payload.positions().size());
                    WandClientPreview.setPreviewPositions(payload.positions());
                    WandClientPreview.setPreviewBlock(payload.targetStack().orElse(ItemStack.EMPTY));
                }
            });
        }
    }
}
