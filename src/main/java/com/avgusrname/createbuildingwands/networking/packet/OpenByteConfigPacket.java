package com.avgusrname.createbuildingwands.networking.packet;

import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen.ByteConfigMenu;
import com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen.ByteConfigScreen;
import com.copycatsplus.copycats.content.copycat.bytes.CopycatByteBlock;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenByteConfigPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenByteConfigPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreateBuildingWands.MODID, "open_byte_config"));
    
    public static final StreamCodec<ByteBuf, OpenByteConfigPacket> STREAM_CODEC = StreamCodec.unit(new OpenByteConfigPacket());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(OpenByteConfigPacket payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        InteractionHand hand = InteractionHand.MAIN_HAND;
        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ByteConfigMenu(id, inv, hand),
            Component.literal("Byte Configuration")
        ), buf -> buf.writeEnum(hand));
    }
}