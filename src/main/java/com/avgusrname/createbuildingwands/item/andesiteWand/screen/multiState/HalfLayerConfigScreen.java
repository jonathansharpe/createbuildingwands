package com.avgusrname.createbuildingwands.item.andesiteWand.screen.multiState;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandMaterialComponent;
import com.avgusrname.createbuildingwands.networking.packet.MultiStateTogglePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

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

    protected String formatKeyName(String key) {
        return switch(key) {
            case "negative_layers" -> "Negative";
            case "positive_layers" -> "Positive";
            default -> key;
        };
    }

    @Override
    protected void initExtraWidgets() {
        int sliderY = this.topPos + 70;
        int sliderX = this.leftPos + MultiStateConfigMenu.PANEL_INNER_X;

        negativeSlider = new LayerSliderWidget(
                sliderX, sliderY, 100, 20,
                "Negative", HalfLayerConfigMenu.MIN_LAYERS, HalfLayerConfigMenu.MAX_LAYERS,
                this.menu.getNegativeLayers(),
                value -> PacketDistributor.sendToServer(new MultiStateTogglePacket(
                        "layer_change", HalfLayerConfigMenu.NEGATIVE_LAYERS_KEY + ":" + value))
        );

        positiveSlider = new LayerSliderWidget(
                sliderX, sliderY + 26, 100, 20,
                "Positive", HalfLayerConfigMenu.MIN_LAYERS, HalfLayerConfigMenu.MAX_LAYERS,
                this.menu.getPositiveLayers(),
                value -> PacketDistributor.sendToServer(new MultiStateTogglePacket(
                        "layer_change", HalfLayerConfigMenu.POSITIVE_LAYERS_KEY + ":" + value))
        );

        this.addRenderableWidget(negativeSlider);
        this.addRenderableWidget(positiveSlider);
    }

    @Override
    protected void updateExtraWidgets(WandMaterialComponent component) {
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
