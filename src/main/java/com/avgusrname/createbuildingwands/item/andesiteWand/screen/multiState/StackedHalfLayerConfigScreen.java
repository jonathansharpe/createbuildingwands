package com.avgusrname.createbuildingwands.item.andesiteWand.screen.multiState;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandMaterialComponent;
import com.avgusrname.createbuildingwands.networking.packet.MultiStateTogglePacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class StackedHalfLayerConfigScreen extends HalfLayerConfigScreen{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CreateBuildingWands.MODID, "textures/gui/stacked_half_layer_config.png");

    @Override
    protected void init() {
        super.init();
    }

    public StackedHalfLayerConfigScreen(HalfLayerConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation getTexture() { return TEXTURE; }

    protected void initExtraWidgets() {
        super.initExtraWidgets();
        for (int i = 0; i < StackedHalfLayerConfigMenu.ORDERED_KEYS.size(); i++) {
            int[] coords = this.menu.getComponentCoordinates(i);
            int btnX = this.leftPos + coords[0];
            int btnY = this.topPos + coords[1];

            int sliderX = btnX + MultiStateConfigMenu.BTN_WIDTH + 4 + MultiStateConfigMenu.SLOT_SIZE + MultiStateConfigMenu.H_SPACING;

            int axisXPos = sliderX + MultiStateConfigMenu.BTN_WIDTH + 4 + MultiStateConfigMenu.H_SPACING;


            if (i == 0) {
                btnFacingEast = Button.builder(Component.literal("East"), btn ->
                                sendFacingChange("east"))
                        .bounds(axisXPos, btnY, 30, 20).build();
                this.addRenderableWidget(btnFacingEast);
                btnFacingWest = Button.builder(Component.literal("West"), btn ->
                                sendFacingChange("west"))
                        .bounds(axisXPos + 30, btnY, 30, 20).build();
                this.addRenderableWidget(btnFacingWest);
            } else {
                btnFacingSouth = Button.builder(Component.literal("South"), btn ->
                                sendFacingChange("south"))
                        .bounds(axisXPos, btnY, 30, 20).build();
                this.addRenderableWidget(btnFacingSouth);
                btnFacingNorth = Button.builder(Component.literal("North"), btn ->
                                sendFacingChange("north"))
                        .bounds(axisXPos + 30, btnY, 30, 20).build();
                this.addRenderableWidget(btnFacingNorth);
            }
        }
    }


    private void sendFacingChange(String direction) {
        PacketDistributor.sendToServer(new MultiStateTogglePacket("facing", direction));
    }

    @Override
    protected void updateExtraWidgets(WandMaterialComponent component) {
        String currentFacing = component.blockStateProps()
                .getOrDefault(StackedHalfLayerConfigMenu.FACING_KEY, StackedHalfLayerConfigMenu.DEFAULT_FACING);

        int currentPosLayers = Integer.parseInt(component.blockStateProps()
                .getOrDefault(StackedHalfLayerConfigMenu.POSITIVE_LAYERS_KEY, String.valueOf(StackedHalfLayerConfigMenu.DEFAULT_LAYERS)));

        int currentNegLayers = Integer.parseInt(component.blockStateProps()
                .getOrDefault(StackedHalfLayerConfigMenu.NEGATIVE_LAYERS_KEY, String.valueOf(StackedHalfLayerConfigMenu.DEFAULT_LAYERS)));

        if (btnFacingSouth != null) btnFacingSouth.active = !currentFacing.equals("south");
        if (btnFacingNorth != null) btnFacingNorth.active = !currentFacing.equals("north");
        if (btnFacingWest != null) btnFacingWest.active = !currentFacing.equals("west");
        if (btnFacingEast != null) btnFacingEast.active = !currentFacing.equals("east");

        if (positiveSlider != null) positiveSlider.active = currentPosLayers > 0;
        if (negativeSlider != null) negativeSlider.active = currentNegLayers > 0;

        if (negativeSlider != null) {
            int layers = Integer.parseInt(component.blockStateProps()
                    .getOrDefault(StackedHalfLayerConfigMenu.NEGATIVE_LAYERS_KEY,
                            String.valueOf(StackedHalfLayerConfigMenu.DEFAULT_LAYERS)));
            negativeSlider.setValue(layers);
        }
        if (positiveSlider != null) {
            int layers = Integer.parseInt(component.blockStateProps()
                    .getOrDefault(StackedHalfLayerConfigMenu.POSITIVE_LAYERS_KEY,
                            String.valueOf(StackedHalfLayerConfigMenu.DEFAULT_LAYERS)));
            positiveSlider.setValue(layers);
        }
    }
}
