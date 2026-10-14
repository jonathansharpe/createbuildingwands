package com.avgusrname.createbuildingwands.item.andesiteWand.screen.multiState;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandMaterialComponent;
import com.avgusrname.createbuildingwands.networking.packet.MultiStateTogglePacket;
import com.google.common.collect.Multiset;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public class HalfLayerConfigScreen extends MultiStateConfigScreen<HalfLayerConfigMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CreateBuildingWands.MODID, "textures/gui/half_layer_config.png");
    private LayerSliderWidget negativeSlider;
    private LayerSliderWidget positiveSlider;

    public HalfLayerConfigScreen(HalfLayerConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 268;
        this.imageHeight = 214;
    }

    @Override
    protected ResourceLocation getTexture() { return TEXTURE; }
    private Button btnAxisX, btnAxisZ, topHalf, bottomHalf;

    protected String formatKeyName(String key) {
        return switch(key) {
            case "negative_layers" -> "Negative";
            case "positive_layers" -> "Positive";
            default -> key;
        };
    }

    @Override
    protected void initExtraWidgets() {

        for (int i = 0; i < HalfLayerConfigMenu.ORDERED_KEYS.size(); i++) {
            int[] coords = this.menu.getComponentCoordinates(i);
            int btnX = this.leftPos + coords[0];
            int btnY = this.topPos + coords[1];

            int sliderX = btnX + MultiStateConfigMenu.BTN_WIDTH + 4 + MultiStateConfigMenu.SLOT_SIZE + MultiStateConfigMenu.H_SPACING;
            LayerSliderWidget slider = getSlider(btnY, i, sliderX);

            int axisXPos = sliderX + MultiStateConfigMenu.BTN_WIDTH + 4 + MultiStateConfigMenu.H_SPACING;


            if (i == 0) {
                negativeSlider = slider;
                btnAxisX = Button.builder(Component.literal("X"), btn ->
                                sendAxisChange("x"))
                        .bounds(axisXPos, btnY, 30, 20).build();
                this.addRenderableWidget(btnAxisX);
                topHalf = Button.builder(Component.literal("Top"), btn ->
                        sendHalfChange("top"))
                        .bounds(axisXPos + 30, btnY, 30, 20).build();
                this.addRenderableWidget(topHalf);
            } else {
                positiveSlider = slider;
                btnAxisZ = Button.builder(Component.literal("Z"), btn ->
                                sendAxisChange("z"))
                        .bounds(axisXPos, btnY, 30, 20).build();
                this.addRenderableWidget(btnAxisZ);
                bottomHalf = Button.builder(Component.literal("Bottom"), btn ->
                                sendHalfChange("bottom"))
                        .bounds(axisXPos + 30, btnY, 30, 20).build();
                this.addRenderableWidget(bottomHalf);
            }

            this.addRenderableWidget(slider);
        }
    }

    private @NotNull LayerSliderWidget getSlider(int btnY, int i, int sliderX) {
        int sliderY = btnY;

        String layerKey = i == 0 ? HalfLayerConfigMenu.NEGATIVE_LAYERS_KEY : HalfLayerConfigMenu.POSITIVE_LAYERS_KEY;
        int initialValue = i == 0 ? this.menu.getNegativeLayers() : this.menu.getPositiveLayers();
        String label = i == 0 ? "Negative" : "Positive";

        return new LayerSliderWidget(
                sliderX, sliderY, 80, 20,
                label, HalfLayerConfigMenu.MIN_LAYERS, HalfLayerConfigMenu.MAX_LAYERS,
                initialValue,
                value -> PacketDistributor.sendToServer(new MultiStateTogglePacket(
                        "layer_change", layerKey + ":" + value ))
        );
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
                .getOrDefault(HalfLayerConfigMenu.AXIS_KEY, HalfLayerConfigMenu.DEFAULT_AXIS);

        String currentHalf = component.blockStateProps()
                .getOrDefault(HalfLayerConfigMenu.HALF_KEY, HalfLayerConfigMenu.DEFAULT_HALF);

        int currentPosLayers = Integer.parseInt(component.blockStateProps()
                .getOrDefault(HalfLayerConfigMenu.POSITIVE_LAYERS_KEY, String.valueOf(HalfLayerConfigMenu.DEFAULT_LAYERS)));

        int currentNegLayers = Integer.parseInt(component.blockStateProps()
                .getOrDefault(HalfLayerConfigMenu.NEGATIVE_LAYERS_KEY, String.valueOf(HalfLayerConfigMenu.DEFAULT_LAYERS)));

        if (btnAxisX != null) btnAxisX.active = !currentAxis.equals("x");
        if (btnAxisZ != null) btnAxisZ.active = !currentAxis.equals("z");

        if (topHalf != null) topHalf.active = !currentHalf.equals("top");
        if (bottomHalf != null) bottomHalf.active = !currentHalf.equals("bottom");

        if (positiveSlider != null) positiveSlider.active = currentPosLayers > 0;
        if (negativeSlider != null) negativeSlider.active = currentNegLayers > 0;

        if (negativeSlider != null) {
            int layers = Integer.parseInt(component.blockStateProps()
                    .getOrDefault(HalfLayerConfigMenu.NEGATIVE_LAYERS_KEY,
                            String.valueOf(HalfLayerConfigMenu.DEFAULT_LAYERS)));
            negativeSlider.setValue(layers);
        }
        if (positiveSlider != null) {
            int layers = Integer.parseInt(component.blockStateProps()
                    .getOrDefault(HalfLayerConfigMenu.POSITIVE_LAYERS_KEY,
                            String.valueOf(HalfLayerConfigMenu.DEFAULT_LAYERS)));
            positiveSlider.setValue(layers);
        }
    }
}
