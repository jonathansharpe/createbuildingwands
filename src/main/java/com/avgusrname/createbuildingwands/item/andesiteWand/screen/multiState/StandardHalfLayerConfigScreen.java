package com.avgusrname.createbuildingwands.item.andesiteWand.screen.multiState;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandMaterialComponent;
import com.avgusrname.createbuildingwands.networking.packet.MultiStateTogglePacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class StandardHalfLayerConfigScreen extends HalfLayerConfigScreen {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CreateBuildingWands.MODID, "textures/gui/half_layer_config.png");

    public StandardHalfLayerConfigScreen(HalfLayerConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected ResourceLocation getTexture() { return TEXTURE; }

    @Override
    protected void initExtraWidgets() {
        super.initExtraWidgets();
        for (int i = 0; i < StandardHalfLayerConfigMenu.ORDERED_KEYS.size(); i++) {
            int[] coords = this.menu.getComponentCoordinates(i);
            int btnX = this.leftPos + coords[0];
            int btnY = this.topPos + coords[1];

            int sliderX = btnX + MultiStateConfigMenu.BTN_WIDTH + 4 + MultiStateConfigMenu.SLOT_SIZE + MultiStateConfigMenu.H_SPACING;

            int axisXPos = sliderX + MultiStateConfigMenu.BTN_WIDTH + 4 + MultiStateConfigMenu.H_SPACING;


            if (i == 0) {
                btnAxisX = Button.builder(Component.literal("X"), btn ->
                                sendAxisChange("x"))
                        .bounds(axisXPos, btnY, 30, 20).build();
                this.addRenderableWidget(btnAxisX);
                topHalf = Button.builder(Component.literal("Top"), btn ->
                        sendHalfChange("top"))
                        .bounds(axisXPos + 30, btnY, 30, 20).build();
                this.addRenderableWidget(topHalf);
            } else {
                btnAxisZ = Button.builder(Component.literal("Z"), btn ->
                                sendAxisChange("z"))
                        .bounds(axisXPos, btnY, 30, 20).build();
                this.addRenderableWidget(btnAxisZ);
                bottomHalf = Button.builder(Component.literal("Bottom"), btn ->
                                sendHalfChange("bottom"))
                        .bounds(axisXPos + 30, btnY, 30, 20).build();
                this.addRenderableWidget(bottomHalf);
            }
        }
    }

    private void sendHalfChange(String half) {
        PacketDistributor.sendToServer(new MultiStateTogglePacket("half", half));
    }

    private void sendAxisChange(String axis) {
        PacketDistributor.sendToServer(new MultiStateTogglePacket("axis", axis));
    }

    @Override
    protected void updateExtraWidgets(WandMaterialComponent component) {

        String currentAxis = component.blockStateProps()
                .getOrDefault(StandardHalfLayerConfigMenu.AXIS_KEY, StandardHalfLayerConfigMenu.DEFAULT_AXIS);

        String currentHalf = component.blockStateProps()
                .getOrDefault(StandardHalfLayerConfigMenu.HALF_KEY, StandardHalfLayerConfigMenu.DEFAULT_HALF);

        int currentPosLayers = Integer.parseInt(component.blockStateProps()
                .getOrDefault(StandardHalfLayerConfigMenu.POSITIVE_LAYERS_KEY, String.valueOf(StandardHalfLayerConfigMenu.DEFAULT_LAYERS)));

        int currentNegLayers = Integer.parseInt(component.blockStateProps()
                .getOrDefault(StandardHalfLayerConfigMenu.NEGATIVE_LAYERS_KEY, String.valueOf(StandardHalfLayerConfigMenu.DEFAULT_LAYERS)));

        if (btnAxisX != null) btnAxisX.active = !currentAxis.equals("x");
        if (btnAxisZ != null) btnAxisZ.active = !currentAxis.equals("z");

        if (topHalf != null) topHalf.active = !currentHalf.equals("top");
        if (bottomHalf != null) bottomHalf.active = !currentHalf.equals("bottom");

        if (positiveSlider != null) positiveSlider.active = currentPosLayers > 0;
        if (negativeSlider != null) negativeSlider.active = currentNegLayers > 0;

        if (negativeSlider != null) {
            int layers = Integer.parseInt(component.blockStateProps()
                    .getOrDefault(StandardHalfLayerConfigMenu.NEGATIVE_LAYERS_KEY,
                            String.valueOf(StandardHalfLayerConfigMenu.DEFAULT_LAYERS)));
            negativeSlider.setValue(layers);
        }
        if (positiveSlider != null) {
            int layers = Integer.parseInt(component.blockStateProps()
                    .getOrDefault(StandardHalfLayerConfigMenu.POSITIVE_LAYERS_KEY,
                            String.valueOf(StandardHalfLayerConfigMenu.DEFAULT_LAYERS)));
            positiveSlider.setValue(layers);
        }
    }
}
