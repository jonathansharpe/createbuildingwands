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
        this.imageHeight = 180;
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

            int xOffset = 20 + (col * 70);
            int yOffset = 30 + (row * 25);

            if (row >= 2) yOffset += 10;

            this.addRenderableWidget(new ByteToggleButton(
                this.menu, x + xOffset, y + yOffset, 60, 20, prop
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
            int slotX = x + 15 + (col * 100) + 65;
            int slotY = y + 30 + (row * 30);
            if (row >= 2) slotY += 15;

            guiGraphics.fill(slotX, slotY, slotX + 20, slotY + 20, 0xFF202020);
            guiGraphics.renderOutline(slotX, slotY, 20, 20, 0xFF8B8B8B);

            ItemStack stack = menu.getMaterialForPart(prop);
            if (!stack.isEmpty()) {
                guiGraphics.renderFakeItem(stack, slotX + 2, slotY + 2);
            }

            if (mouseX >= slotX && mouseX < slotX + 20 && mouseY >= slotY && mouseY < slotY + 20) {
                guiGraphics.renderTooltip(font, Component.literal("Set material for: " + formatName(prop)), mouseX, mouseY);
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
            int slotX = x + 15 + (col * 100) + 65;
            int slotY = y + 30 + (row * 30);
            if (row >= 2) slotY += 15;

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
        guiGraphics.drawString(font, "Top Layer", x + 15, y + 15, 0xFFAAAAAA);
        guiGraphics.drawString(font, "Bottom Layer", x + 15, y + 100, 0xFFAAAAAA);
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
            return Component.literal((active ? "▣  " : "□ ") + formatName(property).split(" ")[1]);
        }
    }
}
