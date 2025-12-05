package com.jsharpexyz.createbuildingwands.item.custom.andesiteWand.screen;

import com.jsharpexyz.createbuildingwands.CreateBuildingWands;
import com.jsharpexyz.createbuildingwands.item.custom.WandMode;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity.Server;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.network.chat.Component;
import com.jsharpexyz.createbuildingwands.item.custom.andesiteWand.AndesiteWandItem;
import com.jsharpexyz.createbuildingwands.networking.packet.WandModePacket;

import net.minecraft.server.level.ServerPlayer;


public class WandConfigScreen extends AbstractContainerScreen<WandConfigMenu> {
    private static final ResourceLocation GUI_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(CreateBuildingWands.MODID, "textures/gui/wandconfig/wand_gui.png");
    
    private static final WandMode[] MODES = WandMode.values();
    private static final int MODE_SPACING = 12;

    private WandMode activeMode;

    public WandConfigScreen(WandConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        this.imageWidth = 200;
        this.imageHeight = 100;
        this.titleLabelX = 5;
        this.inventoryLabelY = Integer.MAX_VALUE;

        this.activeMode = AndesiteWandItem.getMode(playerInventory.player.getItemInHand(menu.getWandHand()));
        System.out.println("[WAND DEBUG] Mode read on client init: " + this.activeMode.name());
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        int modeStartX = this.leftPos + 8;
        int modeStartY = this.topPos + 25;

        pGuiGraphics.drawString(this.font, Component.literal("Building Mode:"), modeStartX, modeStartY, 0x404040, false);

        for (int i = 0; i < MODES.length; i++) {
            WandMode mode = MODES[i];
            int yPos = modeStartY + 15 + (i * MODE_SPACING);

            int color = (mode == activeMode) ? mode.getColor().getTextColor() : 0x808080;

            pGuiGraphics.drawString(this.font, mode.getDisplayName(), modeStartX + 5, yPos, color, false);

            if (isMouseOverMode(modeStartX + 5, yPos, mode.getDisplayName().getString(), pMouseX, pMouseY)) {
                pGuiGraphics.hLine(modeStartX + 5, modeStartX + 5 + font.width(mode.getDisplayName()), yPos + MODE_SPACING - 3, 0xFFFFFFFF);
            }
        }

        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        // following two identify the starting position of the mouse
        int modeStartX = this.leftPos + 8;
        int modeStartY = this.topPos + 25;

        // loop cycles through modes when mouse is clicked to select the correct one
        for (int i = 0; i < MODES.length; i++) {
            // gets the current mode index
            WandMode mode = MODES[i];
            // bumps the mouse down a certain number of pixels to make sure it's detecting the correct list item
            int yPos = modeStartY + 15 + (i * MODE_SPACING);

            // checks if the mouse collides with the selection area
            if (isMouseOverMode(modeStartX + 5, yPos, mode.getDisplayName().getString(), (int)pMouseX, (int)pMouseY)) {
                // the next line only changes the mode VISUALLY; if it's removed, the previous option will remain selected until the player exits and re-opens the menu, where the correct option will be shown
                this.activeMode = mode;

                InteractionHand hand = this.menu.getWandHand();
                
                PacketDistributor.sendToServer(new WandModePacket(mode, hand));

                System.out.println("[WAND DEBUG] Mode selected in mouseClicked(): " + mode.name());
                // following line just says "yes the mouse has been clicked"
                return true;
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    private boolean isMouseOverMode(int x, int y, String text, int mouseX, int mouseY) {
        int textWidth = this.font.width(text);
        int textHeight = this.font.lineHeight;

        return mouseX >= x && mouseX < x + textWidth &&
            mouseY >= y - 1 && mouseY < y + textHeight;
    }

}
