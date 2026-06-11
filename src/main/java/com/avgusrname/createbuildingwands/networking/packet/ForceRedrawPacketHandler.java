package com.avgusrname.createbuildingwands.networking.packet;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.copycatsplus.copycats.utility.BlockEntityUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ForceRedrawPacketHandler {
    public static void handle(ForceRedrawPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            retryRedraw(packet.pos(), 10);
        });
    }

    public static void retryRedraw(BlockPos pos, int attemptsLeft) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            BlockEntityUtils.redraw(be);
            return;
        }

        if (attemptsLeft <= 0) {
            CreateBuildingWands.LOGGER.info("[ForceRedraw] BE never loaded at pos: {}, giving up", pos);
            return;
        }

        mc.tell(() -> retryRedraw(pos, attemptsLeft - 1));
    }
}
