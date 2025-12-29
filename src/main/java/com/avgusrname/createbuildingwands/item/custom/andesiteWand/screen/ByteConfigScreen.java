package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ByteConfigScreen extends AbstractContainerScreen<ByteConfigMenu> {

    private static final List<String> PROPERTIES = List.of(
        "top_northwest", "top_northeast",
        "top_southwest", "top_southeast",
        "bottom_northwest", "bottom_northeast",
        "bottom_southwest", "bottom_southeast"
    );

    public ByteConfigScreen(ByteConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 220;
        this.imageHeight = 206;
        this.inventoryLabelY = Integer.MAX_VALUE;
        this.titleLabelY = 5;
    }

    @Override
    protected void init() {
        super.init();
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        for (int i = 0; i < PROPERTIES.size(); i++) { 
            String prop = PROPERTIES.get(i);
            int col = i % 2;
            int row = i / 2;

            int xOffset = 15 + (col * 100);
            int yOffset = 20 + (row * 24);

            if (row >= 2) yOffset += 8;

            this.addRenderableWidget(new ByteToggleButton(
                this.menu, x + xOffset, y + yOffset, 85, 20, prop
            ));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        for (int i = 0; i < PROPERTIES.size(); i++) {
            String prop = PROPERTIES.get(i);
            int col = i % 2;
            int row = i / 2;
            int slotX = x + 15 + (col * 100) + 88;
            int slotY = y + 20 + (row * 24);
            if (row >= 2) slotY += 8;

            guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF202020);

            int borderColor = 0xFF5A5A5A;

            guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 1, borderColor);
            guiGraphics.fill(slotX, slotY, slotX + 1, slotY + 18, borderColor);

            guiGraphics.fill(slotX, slotY + 17, slotX + 18, slotY + 18, borderColor);
            guiGraphics.fill(slotX + 17, slotY, slotX + 18, slotY + 18, borderColor);

            ItemStack stack = menu.getMaterialForPart(prop);
            if (!stack.isEmpty()) {
                guiGraphics.renderFakeItem(stack, slotX + 2, slotY + 2);
            }

            if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                guiGraphics.renderTooltip(font, Component.literal("Material: " + (stack.isEmpty() ? "None" : stack.getHoverName().getString())), mouseX, mouseY);
            }
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        for (int i = 0; i < PROPERTIES.size(); i++) {
            String prop = PROPERTIES.get(i);
            int col = i % 2;
            int row = i / 2;
            int slotX = x + 15 + (col * 100) + 88;
            int slotY = y + 20 + (row * 24);
            if (row >= 2) slotY += 8;

            if (mouseX >= slotX && mouseX < slotX + 20 && mouseY >= slotY && mouseY < slotY + 20) {
                ItemStack held = getMenu().getCarried();

                menu.setMaterialForPart(prop, button == 1 ? ItemStack.EMPTY : held.copyWithCount(1));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF353535);

        guiGraphics.fill(x, y, x + imageWidth, y + 2, 0xFF5A5A5A);
        guiGraphics.fill(x, y + imageHeight - 2, x + imageWidth, y + imageHeight, 0xFF5A5A5A);
        guiGraphics.fill(x, y, x + 2, y + imageHeight, 0xFF5A5A5A);
        guiGraphics.fill(x + imageWidth - 2, y, x + imageWidth, y + imageHeight, 0xFF5A5A5A);

        drawInventorySlots(guiGraphics, x + ByteConfigMenu.INVENTORY_START_X, y + ByteConfigMenu.INVENTORY_START_Y);
    }

    private void drawInventorySlots(GuiGraphics guiGraphics, int startX, int startY){
        int slotSize = 18;
        int slotColor = 0xFF202020;
        int borderColor = 0xFF5A5A5A;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = startX + col * slotSize;
                int sy = startY + row * slotSize;
                drawSlot(guiGraphics, sx, sy, slotColor, borderColor);
            }
        }
    }

    private void drawSlot(GuiGraphics g, int x, int y, int color, int border) {
        g.fill(x, y, x + 18, y + 18, border);
        g.fill(x + 1, y + 1, x + 17, y + 17, color);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }

    private String formatName(String raw) {
        return raw.replace("_", " ").toUpperCase();
    }

    private class ByteToggleButton extends Button {
        private final String property;
        private final ByteConfigMenu menu;

        public ByteToggleButton(ByteConfigMenu menu, int x, int y, int width, int height, String property) {
            super(x, y, width, height, Component.empty(), b -> menu.toggleByte(property), DEFAULT_NARRATION);
            this.menu = menu;
            this.property = property;
        }

        @Override
        public Component getMessage() {
            boolean active = menu.getByte(property);
            String[] parts = formatName(property).split(" ");
            String label = parts.length > 1 ? parts[1] : parts[0];
            return Component.literal((active ? "▣ " : "□ ") + label);
        }
    }
}
