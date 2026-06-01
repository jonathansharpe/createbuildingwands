package com.avgusrname.createbuildingwands.networking.packet;

import java.util.Optional;

import org.apache.commons.lang3.concurrent.AbstractConcurrentInitializer;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.item.custom.WandMode;
import com.avgusrname.createbuildingwands.item.custom.andesiteWand.AndesiteWandItem;
import com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen.ByteConfigMenu;
import com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen.WandConfigMenu;
import com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen.ByteCornerData.ByteCopycatCorner;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;

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
            case OPEN_BYTE_CONFIG_MENU -> {
                if (player.containerMenu instanceof ByteConfigMenu byteMenu) {
                    ByteCopycatCorner corner = ByteCopycatCorner.values()[payload.value];

                    CreateBuildingWands.LOGGER.info("[WandPacket handleOnServer] value of corner is: {}", corner);
                    CreateBuildingWands.LOGGER.info("[WandPacket handleOnServer] current active server menu is byteconfigmenu");
                    byteMenu.handleServerToggle(corner);
                } else if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.openMenu(new MenuProvider() {
                        @Override
                        public Component getDisplayName() {
                            return Component.literal("Copycat Byte Configuration");
                        }

                        @Override
                        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player playerEntity) {
                            return new ByteConfigMenu(containerId, playerInventory, payload.hand());
                        }
                    }, buf -> {
                        buf.writeEnum(payload.hand());
                    });
                }
            }
        }
    }

    public enum WandCommand {
        SET_MODE,
        OPEN_BYTE_CONFIG_MENU
    }
}