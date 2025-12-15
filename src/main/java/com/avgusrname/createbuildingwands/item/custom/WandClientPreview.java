package com.avgusrname.createbuildingwands.item.custom;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.avgusrname.createbuildingwands.item.custom.andesiteWand.AndesiteWandItem;
import com.avgusrname.createbuildingwands.util.WandGeometryUtil;
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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class WandClientPreview {
    @Nullable
    private static BlockPos activeStartPos = null;

    @Nullable
    private static WandMode activeMode = null;

    private static List<BlockPos> previewPositions = Collections.emptyList();

    @Nullable
    private static ItemStack previewBlock = null;

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

    public static void setPreviewBlock(@Nullable ItemStack stack) {
        previewBlock = stack;
    }

    public static void clearPreviewPositions() {
        previewPositions = Collections.emptyList();
        previewBlock = null;
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

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (previewPositions.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();

        PoseStack poseStack = event.getPoseStack();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // Determine which block state to render for the preview
        BlockState stateToRender = null;
        if (previewBlock != null && !previewBlock.isEmpty()) {
            Block block = Block.byItem(previewBlock.getItem());
            if (block != null) {
                stateToRender = block.defaultBlockState();
            }
        }

        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();

        // First: render slightly transparent block models, if we have a valid block.
        // We rely on the translucent render type so alpha values on the baked quads can blend.
        if (stateToRender != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            for (BlockPos pos : previewPositions) {
                poseStack.pushPose();
                poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
                dispatcher.renderBatched(
                        stateToRender,
                        pos,
                        level,
                        poseStack,
                        buffer.getBuffer(RenderType.translucent()),
                        false,
                        level.random
                );
                poseStack.popPose();
            }
        }

        // Second: always render wireframe boxes on top for clarity
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lines());
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

        buffer.endBatch();

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
