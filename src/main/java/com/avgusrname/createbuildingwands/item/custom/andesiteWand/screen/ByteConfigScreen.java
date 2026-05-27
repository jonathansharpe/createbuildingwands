package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

import java.util.List;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.simibubi.create.CreateBuildInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ByteConfigScreen extends AbstractContainerScreen<ByteConfigMenu> {

	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CreateBuildingWands.MODID, "textures/gui/byte_config.png");

	private final boolean[] cornerStates = new boolean[8];

	public ByteConfigScreen(ByteConfigMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	@Override
	protected void init() {
		super.init();
		this.addCornerButtons();
	}

	private void addCornerButtons() {
		int centerX = (this.width - this.imageWidth) / 2;
		int centerY = (this.height - this.imageHeight) / 2;

		int btnWidth = 34;
		int btnHeight = 20;
		int spacing = 4;

		for (int i = 0; i < 8; i++) {
			final int cornerIndex = i;

			int group = cornerIndex / 4;
			int column = cornerIndex % 2;
			int row = (cornerIndex % 4) / 2;

			int xOffset = centerX + 12 + (group * (btnWidth * 2 + 16)) + (column * (btnWidth + spacing));
			int yOffset = centerY + 24 + (row * (btnHeight + spacing));

			this.addRenderableWidget(Button.builder(
				Component.literal("C" + cornerIndex + ": OFF"),
				button -> {
					this.cornerStates[cornerIndex] = !this.cornerStates[cornerIndex];

					boolean isActive = this.cornerStates[cornerIndex];
					button.setMessage(Component.literal("C" + cornerIndex + (isActive ? ": ON" : ": OFF")));

					// TODO send network packet (according to gemini)
				})
				.bounds(xOffset, yOffset, btnWidth, btnHeight)
				.build()
			);
		}
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		int x = (this.width - this.imageWidth) / 2;
		int y = (this.height - this.imageHeight) / 2;
		graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
	}

	private boolean isCornerActive = false;
}
