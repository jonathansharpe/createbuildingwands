package com.avgusrname.createbuildingwands.item.andesiteWand.screen;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.networking.packet.MultiStateTogglePacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class SlabConfigScreen extends MultiStateConfigScreen<SlabConfigMenu>{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CreateBuildingWands.MODID, "textures/gui/slab_config.png");

    private Button btnAxisX, btnAxisY, btnAxisZ;

    public SlabConfigScreen(SlabConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 200;
        this.imageHeight = 180;
    }

    @Override
    protected void initExtraWidgets() {
        int axisY = this.topPos + 70;
        int axisX = this.leftPos + MultiStateConfigMenu.PANEL_INNER_X;

        btnAxisX = Button.builder(Component.literal("X"), btn ->
                        sendAxisChange("x"))
                .bounds(axisX, axisY, 30, 20).build();

        btnAxisY = Button.builder(Component.literal("Y"), btn ->
                sendAxisChange("y"))
                .bounds(axisX + 34, axisY, 30, 20).build();

        btnAxisZ = Button.builder(Component.literal("Z"), btn ->
                sendAxisChange("z"))
                .bounds(axisX + 60, axisY, 30, 20).build();

        this.addRenderableWidget(btnAxisX);
        this.addRenderableWidget(btnAxisY);
        this.addRenderableWidget(btnAxisZ);
    }

    @Override
    protected void updateExtraWidgets(WandMaterialComponent component) {
        String currentAxis = component.blockStateProps()
                .getOrDefault(SlabConfigMenu.AXIS_KEY, SlabConfigMenu.DEFAULT_AXIS);

        if (btnAxisX != null) btnAxisX.active = !currentAxis.equals("x");
        if (btnAxisY != null) btnAxisY.active = !currentAxis.equals("y");
        if (btnAxisZ != null) btnAxisZ.active = !currentAxis.equals("z");
    }

    private void sendAxisChange(String axis) {
        PacketDistributor.sendToServer(new MultiStateTogglePacket("axis", axis));
    }

    @Override
    protected ResourceLocation getTexture() { return TEXTURE; }

    @Override
    protected String formatKeyName(String key) {
        return switch (key) {
            case "top" -> "Top";
            case "bottom" -> "Bottom";
            default -> key;
        };
    }
}
