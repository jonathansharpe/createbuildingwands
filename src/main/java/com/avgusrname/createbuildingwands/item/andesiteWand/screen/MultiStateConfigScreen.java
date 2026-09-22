package com.avgusrname.createbuildingwands.item.andesiteWand.screen;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.avgusrname.createbuildingwands.networking.packet.MultiStateTogglePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public abstract class MultiStateConfigScreen<T extends MultiStateConfigMenu> extends AbstractContainerScreen<T> {
    protected final Button[] partButtons;

    protected MultiStateConfigScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.partButtons = new Button[menu.slotCount];
    }

    @Override
    protected void init() {
        super.init();
        CreateBuildingWands.LOGGER.info("[WandDebug] MultiStateConfigScreen init called, slotCount: {}", menu.slotCount);
        this.renderables.clear();

        for (int i = 0; i < menu.slotCount; i++) {
            CreateBuildingWands.LOGGER.info("[WandDebug] Creating button for slot {}", i);
            int[] coords = this.menu.getComponentCoordinates(i);
            int absoluteBtnX = this.leftPos + coords[0];
            int absoluteBtnY = this.topPos + coords[1];

            final int index = i;
            String key = menu.getOrderedKeys().get(i);

            this.partButtons[i] = Button.builder(Component.literal(formatKeyName(key)), button -> {
                onPartButtonClicked(index);
            }).bounds(absoluteBtnX, absoluteBtnY, MultiStateConfigMenu.BTN_WIDTH, MultiStateConfigMenu.BTN_HEIGHT).build();

            this.addRenderableWidget(this.partButtons[i]);
        }

        initExtraWidgets();
        updateButtonMessages();
    }

    private int renderCount = 0;
    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (renderCount < 5) {
            CreateBuildingWands.LOGGER.info("[WandDebug] MultiStateConfigScreen render called");
            renderCount++;
        }
        updateButtonMessages();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
       // int x = (this.width - this.imageWidth) / 2;
       // int y = (this.height - this.imageHeight) / 2;
       // graphics.blit(getTexture(), x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    private void updateButtonMessages() {
        if (this.minecraft == null || this.minecraft.player == null) return;

        ItemStack syncedWand = this.minecraft.player.getItemInHand(this.menu.getWandHand());
        WandMaterialComponent component = syncedWand.getOrDefault(
                ModDataComponents.WAND_MATERIALS.get(),
                WandMaterialComponent.createEmptyDefault()
        );

        for (int i = 0; i < partButtons.length; i++) {
            Button btn = partButtons[i];
            if (btn == null) continue;

            String key = menu.getOrderedKeys().get(i);
            boolean isActive = component.isActive(key);
            btn.setMessage(Component.literal(formatKeyName(key) + (isActive ? ": ON" : ": OFF")));
        }

        updateExtraWidgets(component);
    }

    protected void onPartButtonClicked(int index) {
        PacketDistributor.sendToServer(new MultiStateTogglePacket("toggle", String.valueOf(index)));
    }

    protected void initExtraWidgets() {}
    protected void updateExtraWidgets(WandMaterialComponent component) {}

    protected abstract ResourceLocation getTexture();
    protected abstract String formatKeyName(String key);
}
