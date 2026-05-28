package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

import java.util.List;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen.ByteCornerData.ByteCopycatCorner;
import com.avgusrname.createbuildingwands.networking.packet.CornerTogglePacket;
import com.simibubi.create.CreateBuildInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

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
					PacketDistributor.sendToServer(new CornerTogglePacket(cornerIndex));
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

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.updateButtonMessages();
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	private void updateButtonMessages() {
		int actualSlotIndex = this.menu.getLockedWandSlotIndex() + 35;
		if (actualSlotIndex < 0 || actualSlotIndex >= this.menu.slots.size()) return;

		ItemStack wandStack = this.menu.slots.get(actualSlotIndex).getItem();

		int buttonIndex = 0;
		for (var widget : this.renderables) {
			if (widget instanceof Button button && buttonIndex < 8) {
				ByteCopycatCorner corner = ByteCopycatCorner.values()[buttonIndex];

				boolean isEnabled = true;

				if (wandStack.has(DataComponents.CUSTOM_DATA)) {
					CompoundTag tag = wandStack.get(DataComponents.CUSTOM_DATA).copyTag();
					CompoundTag materialData = tag.getCompound("material_data");

					if (materialData.contains(corner.getNbtKey())) {
						CompoundTag cornerTag = materialData.getCompound(corner.getNbtKey());
						if (cornerTag.contains("enableCT")) {
							isEnabled = cornerTag.getBoolean("enableCT");
						}
					}
				}

				button.setMessage(Component.literal("C" + buttonIndex + (isEnabled ? ": ON" : ": OFF")));
				buttonIndex++;
			}
		}
	}

	private boolean isCornerActive = false;
}
