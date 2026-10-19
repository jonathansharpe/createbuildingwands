package com.avgusrname.createbuildingwands.item.andesiteWand.screen.multiState;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.networking.packet.MultiStateTogglePacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;


public abstract class HalfLayerConfigScreen extends MultiStateConfigScreen<HalfLayerConfigMenu> {
    protected LayerSliderWidget negativeSlider;
    protected LayerSliderWidget positiveSlider;
    protected Button btnAxisX, btnAxisZ, topHalf, bottomHalf;
    protected Button btnFacingSouth, btnFacingNorth, btnFacingWest, btnFacingEast;

    public HalfLayerConfigScreen(HalfLayerConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 268;
        this.imageHeight = 214;
    }

    @Override
    protected void init() {
        super.init();
    }

    protected String formatKeyName(String key) {
        return switch(key) {
            case "negative_layers" -> "Negative";
            case "positive_layers" -> "Positive";
            default -> key;
        };
    }

    protected LayerSliderWidget getSlider(int btnY, int i, int sliderX) {
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

    protected void initExtraWidgets() {
        for (int i = 0; i < HalfLayerConfigMenu.ORDERED_KEYS.size(); i++) {
            int[] coords = this.menu.getComponentCoordinates(i);
            int btnX = this.leftPos + coords[0];
            int btnY = this.topPos + coords[1];

            int sliderX = btnX + MultiStateConfigMenu.BTN_WIDTH + 4 + MultiStateConfigMenu.SLOT_SIZE + MultiStateConfigMenu.H_SPACING;
            LayerSliderWidget slider = getSlider(btnY, i, sliderX);
            CreateBuildingWands.LOGGER.info("[WandDebug] slider: {}, bounds: X = {}, Y= {}, W= {}, H= {}", slider, sliderX, btnY, slider.getWidth(), slider.getHeight());

            if (i == 0) {
                negativeSlider = slider;
            }
            else {
                positiveSlider = slider;
            }

            this.addRenderableWidget(slider);
        }
    }

    protected abstract ResourceLocation getTexture();

}
