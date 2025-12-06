package com.jsharpexyz.createbuildingwands.item.custom.andesiteWand.screen;

import com.jsharpexyz.createbuildingwands.CreateBuildingWands;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import java.util.List;
import java.util.function.Consumer;

public class WandModeScrollWidget extends AbstractWidget {
    private static final ResourceLocation WIDGETS_LOCATION = ResourceLocation.fromNamespaceAndPath(CreateBuildingWands.MODID, "textures/gui/widgets.png");

    private final List<Component> modes;
    private int currentModeIndex;
    private final Consumer<Integer> onModeChanged;

    private static final int WIDGET_WIDTH = 80;
    private static final int WIDGET_HEIGHT = 20;

    public WandModeScrollWidget(int x, int y, List<Component> modes, int initialIndex, Consumer<Integer> onModeChanged) {
        super(x, y, WIDGET_WIDTH, WIDGET_HEIGHT, Component.empty());
        this.modes = modes;

        this.currentModeIndex = Mth.clamp(initialIndex, 0, modes.size() - 1);
        this.onModeChanged = onModeChanged;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.visible && this.isHoveredOrFocused()) {
            int newIndex = this.currentModeIndex;

            if (scrollY > 0) {
                newIndex = (newIndex - 1 + modes.size()) % modes.size();
            }
            else if (scrollY < 0) {
                newIndex = (newIndex + 1) % modes.size();
            }

            if (newIndex != this.currentModeIndex) {
                this.currentModeIndex = newIndex;
                this.onModeChanged.accept(this.currentModeIndex);
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        graphics.blit(WIDGETS_LOCATION, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 0, 46 + (this.isHovered() ? 20 : 0), 200, 20, 256, 256);

        Component modeText = this.modes.get(this.currentModeIndex);
        int color = this.active ? 0xFFFFFFFF : 0xFFA0A0A0; // white or gray

        graphics.drawCenteredString(
            Minecraft.getInstance().font,
            modeText,
            this.getX() + this.getWidth() / 2,
            this.getY() + (this.getHeight() - 8) / 2,
            color
        );
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        Component currentMode = this.modes.get(this.currentModeIndex);
        Component combinedMessage = Component.literal("Wand Mode: ")
            .append(currentMode)
            .append(Component.literal(". Scroll mouse wheel to cycle through modes."));

        narrationElementOutput.add(NarratedElementType.TITLE, combinedMessage);
    }
}
