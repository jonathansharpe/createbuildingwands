package com.avgusrname.createbuildingwands.item.custom;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.avgusrname.createbuildingwands.item.custom.WandMode;
import com.avgusrname.createbuildingwands.util.WandGeometryUtil;
import com.google.common.eventbus.Subscribe;
import com.jcraft.jorbis.Block;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class WandClientPreview {
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
    public static void onClientTick(PlayerTickEvent.Post event) {
        // System.out.println("does this thing work? (onClientTick)");
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;

        if (level == null || event.getEntity() == null || activeStartPos == null || activeMode == null) {
            return;
        }

        HitResult hit = event.getEntity().pick(50.0D, 0.0F, false);
        
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos clickedPos = blockHit.getBlockPos();
            Direction face = blockHit.getDirection();

            BlockPos currentEndPos = clickedPos.relative(face);

            List<BlockPos> calculatedPositions;

            switch (activeMode) {
                case PLANE:
                    calculatedPositions = WandGeometryUtil.planeBlockPositions(activeStartPos, currentEndPos, face);
                    break;
                case CUBE:
                    calculatedPositions = WandGeometryUtil.cubeBlockPositions(activeStartPos, currentEndPos);
                    break;
                case LINE:
                    calculatedPositions = WandGeometryUtil.lineBlockPositions(activeStartPos, currentEndPos);
                    break;
                default:
                    calculatedPositions = Collections.emptyList();
                    break;
            }

            setPreviewPositions(calculatedPositions);
        }
        else {
            clearPreviewPositions();
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {

        System.out.println("attempting to render the things");
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        if (previewPositions.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();

        PoseStack poseStack = event.getPoseStack();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lines());

        poseStack.pushPose();

        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        for (BlockPos pos : previewPositions) {
            AABB box = new AABB(pos);

            LevelRenderer.renderLineBox(
                    poseStack,
                    vertexConsumer,
                    box,
                    0.0F, 0.9F, 0.0F, 0.8F // RGBA
            );
        }


        poseStack.popPose();

        buffer.endBatch(RenderType.lines());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

}
