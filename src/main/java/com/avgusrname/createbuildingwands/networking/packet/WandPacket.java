package com.avgusrname.createbuildingwands.networking.packet;

import java.util.Optional;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.avgusrname.createbuildingwands.item.WandMode;
import com.avgusrname.createbuildingwands.item.andesiteWand.AndesiteWandItem;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.ByteConfigMenu;

import com.avgusrname.createbuildingwands.item.andesiteWand.screen.SlabConfigMenu;
import com.copycatsplus.copycats.content.copycat.bytes.CopycatByteBlock;
import com.copycatsplus.copycats.content.copycat.slab.CopycatSlabBlock;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record WandPacket(WandCommand command, int value, Optional<WandMode> wandMode, InteractionHand hand) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WandPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateBuildingWands.MODID, "mode_change"));

    public static final StreamCodec<FriendlyByteBuf, WandCommand> WAND_COMMAND_CODEC = StreamCodec
            .of(FriendlyByteBuf::writeEnum, buf -> buf.readEnum(WandCommand.class));

    public static final StreamCodec<FriendlyByteBuf, InteractionHand> INTERACTION_HAND_CODEC = StreamCodec
            .of(FriendlyByteBuf::writeEnum, buf -> buf.readEnum(InteractionHand.class));

    public static final StreamCodec<FriendlyByteBuf, WandMode> WAND_MODE_CODEC =
        StreamCodec.of(FriendlyByteBuf::writeEnum, buf -> buf.readEnum(WandMode.class));

    public static final StreamCodec<FriendlyByteBuf, WandPacket> STREAM_CODEC = StreamCodec.composite(
            WAND_COMMAND_CODEC, WandPacket::command,
            ByteBufCodecs.VAR_INT, WandPacket::value,
            WAND_MODE_CODEC.apply(ByteBufCodecs::optional), WandPacket::wandMode,
            INTERACTION_HAND_CODEC, WandPacket::hand,
            WandPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(WandPacket payload, IPayloadContext context) {
        Player player = context.player();
        ItemStack wand = player.getItemInHand(payload.hand());

        switch (payload.command()) {
            case SET_MODE -> {
                if (wand.getItem() instanceof AndesiteWandItem) {
                    payload.wandMode().ifPresent(mode -> {
                        AndesiteWandItem.setMode(wand, mode);
                        player.setItemInHand(payload.hand(), wand);
                        player.getInventory().setChanged();
                    });
                }
            }
            case OPEN_MULTISTATE_CONFIG_MENU -> {
                if (player instanceof ServerPlayer serverPlayer) {
                    CreateBuildingWands.LOGGER.info("[WandDebug] OPEN_MULTISTATE_CONFIG_MENU received, player menu: {}", player.containerMenu.getClass().getSimpleName());
                    Block copycatBlock = wand.get(ModDataComponents.WAND_BLOCK_COPYCAT.get());
                    CreateBuildingWands.LOGGER.info("[WandDebug] copycat block is: {}", copycatBlock);
                    switch (copycatBlock) {
                        case null -> {
                            return;
                        }
                        case CopycatByteBlock copycatByteBlock -> {
                            CreateBuildingWands.LOGGER.info("[WandDebug] Opening ByteConfigMenu");
                            serverPlayer.openMenu(new MenuProvider() {
                                @Override
                                public Component getDisplayName() {
                                    return Component.literal("Copycat Byte Configuration");
                                }

                                @Override
                                public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, Player p) {
                                    return new ByteConfigMenu(id, inv, payload.hand());
                                }
                            }, buf -> buf.writeEnum(payload.hand()));
                        }
                        case CopycatSlabBlock copycatSlabBlock -> {
                            CreateBuildingWands.LOGGER.info("[WandDebug] Opening SlabConfigMenu");
                            serverPlayer.openMenu(new MenuProvider() {
                                @Override
                                public Component getDisplayName() {
                                    return Component.literal("Copycat Slab Configuration");
                                }

                                @Override
                                public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                                    return new SlabConfigMenu(id, inv, payload.hand());
                                }
                            }, buf -> buf.writeEnum(payload.hand()));
                        }
                        default -> {
                        }
                    }

                }
            }
        }
    }

    public enum WandCommand {
        SET_MODE,
        OPEN_MULTISTATE_CONFIG_MENU
    }
}