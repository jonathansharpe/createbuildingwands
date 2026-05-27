package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.item.custom.WandMode;
import com.avgusrname.createbuildingwands.item.custom.andesiteWand.AndesiteWandItem;
import com.avgusrname.createbuildingwands.networking.packet.WandModePacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity.Server;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WandConfigScreen extends AbstractContainerScreen<WandConfigMenu> {
    private static final ResourceLocation GUI_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(CreateBuildingWands.MODID, "textures/gui/wandconfig/wand_gui.png");

    private static final int MODE_WIDGET_WIDTH = 80;
    
    private WandModeScrollWidget modeWidget;

    private static final List<Component> WAND_MODE_NAMES = Arrays.stream(WandMode.values())
        .map(WandMode::getDisplayName)
        .collect(Collectors.toList());
    
    // constructor for the wand config screen
    // sets the image width and height, may need to change to not hardcoded
    public WandConfigScreen(WandConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        this.imageWidth = 176;
        this.imageHeight = 153;
        this.titleLabelY = 5;
        this.inventoryLabelY = WandConfigMenu.INVENTORY_START_Y - 10;
    }

    // initializes the screen and adds the mode scroll widget
    @Override
    protected void init() {
        super.init();

        int modeWidgetX = this.leftPos + 8;
        int modeWidgetY = this.topPos + 35;
        int xOffset = this.leftPos + 100;
        int yOffset = this.topPos + 100;

        int initialModeIndex = this.menu.getInitialModeIndex();

        this.modeWidget = new WandModeScrollWidget(
            modeWidgetX,
            modeWidgetY,
            WAND_MODE_NAMES,
            initialModeIndex,
            this::onModeScroll
        );
        this.addRenderableWidget(modeWidget);

        // adds widget for copycat multistate config
        this.addRenderableWidget(Button.builder(Component.literal("Configure Byte"), button -> {
            Minecraft.getInstance().setScreen(new ByteConfigScreen(
                new ByteConfigMenu(0, Minecraft.getInstance().player.getInventory()), 
                Minecraft.getInstance().player.getInventory(),
                Component.literal("Prototyping Screen")
            ));
        }).bounds(xOffset, yOffset, 100, 20).build());
    }

    // renders all of the labels for the menu
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        super.renderLabels(guiGraphics, pMouseX, pMouseY);

        int labelX = 8 + (MODE_WIDGET_WIDTH / 2);
        int labelY = 20;

        guiGraphics.drawCenteredString(
            this.font,
            Component.literal("MODE SELECTOR"),
            labelX,
            labelY,
            0xFFFFFF
        );
    }

    // when the mode scroll widget is scrolled, this function is called
    // handles the logic for selecting the wand mode
    // involves sending a packet to the server to set the wand mode
    private void onModeScroll(int newIndex) {
        WandMode selectedMode = WandMode.values()[newIndex];
        InteractionHand hand = this.menu.getWandHand();

        Minecraft.getInstance().player.displayClientMessage(
            Component.literal("Wand Mode Set to: " + selectedMode.getDisplayName().getString()),
            true
        );
        
        PacketDistributor.sendToServer(new WandModePacket(selectedMode, hand));
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight);

        guiGraphics.blit(GUI_TEXTURE, this.leftPos + 7, this.topPos + WandConfigMenu.INVENTORY_START_Y, 7, WandConfigMenu.INVENTORY_START_Y, 162, 94);

        guiGraphics.blit(GUI_TEXTURE,
            this.leftPos + WandConfigMenu.WAND_SLOT_X - 1,
            this.topPos + WandConfigMenu.WAND_SLOT_Y - 1,
            0,
            this.imageHeight,
            18,
            18
        );
    }

}
