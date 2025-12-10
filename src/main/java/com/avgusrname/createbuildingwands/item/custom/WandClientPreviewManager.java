package com.avgusrname.createbuildingwands.item.custom;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.avgusrname.createbuildingwands.item.custom.WandMode;
import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.client.multiplayer.ClientLevel;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class WandClientPreviewManager {
    @Nullable
    private static BlockPos activeStartPos = null;

    @Nullable
    private static WandMode activeMode = null;

    private static List<BlockPos> previewPositions = Collections.emptyList();

    public static void updateActiveState(@Nullable BlockPos startPos, @Nullable WandMode mode) {
        activeStartPos = startPos;
        activeMode = mode;
        if (startPos == null) {
            clearPreviewPositions();
        }
    }

    public static void setPreviewPositions(List<BlockPos> positions) {
        previewPositions = positions;
    }

    public static void clearPreviewPositions() {
        previewPositions = Collections.emptyList();
    }

    @SubscribeEvent
    public static void onClientTick(LevelTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
    }
}
