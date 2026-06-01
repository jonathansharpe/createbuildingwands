package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

import java.util.List;
import java.util.Optional;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen.ByteCornerData.ByteCopycatCorner;
import com.avgusrname.createbuildingwands.networking.packet.CornerTogglePacket;
import com.avgusrname.createbuildingwands.networking.packet.WandPacket;
import com.avgusrname.createbuildingwands.networking.packet.WandPacket.WandCommand;
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

	private final Button[] cornerButtons = new Button[8];

	public ByteConfigScreen(ByteConfigMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 268;
		this.imageHeight = 214;
	}

	@Override
	protected void init() {
		super.init();
		this.renderables.clear();
		
		for (int i = 0; i < 8; i++) {
			int[] coords = ByteConfigMenu.getComponentCoordinates(i);

			int absoluteBtnX = this.leftPos + coords[0];
			int absoluteBtnY = this.topPos + coords[1];
		
			// CreateBuildingWands.LOGGER.info("[WandDebug init] btn coords: x: {}, y: {}", absoluteBtnX, absoluteBtnY);

			final int cornerIndex = i;
			ByteCopycatCorner tempCorner = ByteCopycatCorner.values()[cornerIndex];
			String initialLabel = formatCornerName(tempCorner.getNbtKey()) + ": OFF";

			// CreateBuildingWands.LOGGER.info("initialLabel: {}", initialLabel);

			this.cornerButtons[i] = Button.builder(Component.literal(initialLabel), button -> {
				PacketDistributor.sendToServer(new WandPacket(WandPacket.WandCommand.OPEN_BYTE_CONFIG_MENU, cornerIndex, Optional.empty(), this.menu.getWandHand()));
				// CreateBuildingWands.LOGGER.info("[WandDebug init] button has been clicked");
			}).bounds(absoluteBtnX, absoluteBtnY, ByteConfigMenu.BTN_WIDTH, ByteConfigMenu.BTN_HEIGHT).build();

			this.addRenderableWidget(this.cornerButtons[i]);
		}
		// CreateBuildingWands.LOGGER.info("[WandDebug init] about to update button messages after the for loop");
		this.updateButtonMessages();
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		int x = (this.width - this.imageWidth) / 2;
		int y = (this.height - this.imageHeight) / 2;
		graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// CreateBuildingWands.LOGGER.info("[WandDebug render] about to update buttons and then render everything");
		this.updateButtonMessages();
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	private void updateButtonMessages() {
		// INFO any log messages in this function will be called a LOT, like once every second, so log carefully
		// CreateBuildingWands.LOGGER.info("[WandDebug ByteConfigScreen] updateButtonMessages called");

		if (this.minecraft.player == null) return;

		ItemStack syncedWand = this.minecraft.player.getItemInHand(this.menu.getWandHand());

		int[] activeCorners = new int[] {0, 0, 0, 0, 0, 0, 0, 0};

		if (!syncedWand.isEmpty() && syncedWand.has(DataComponents.CUSTOM_DATA)) {
			CompoundTag clientTag = syncedWand.get(DataComponents.CUSTOM_DATA).copyTag();
			int[] savedArray = clientTag.getIntArray("active_corners");
			if (savedArray.length == 8) {
				activeCorners = savedArray;
			}
		}

		for (int i = 0; i < 8; i++) {
			Button btn = this.cornerButtons[i];
			if (btn != null) {
				ByteCopycatCorner corner = ByteCopycatCorner.values()[i];
				boolean isActive = activeCorners[i] == 1;
				String stateSuffix = isActive ? ": ON" : ": OFF";

				btn.setMessage(Component.literal(formatCornerName(corner.getNbtKey()) + stateSuffix));
			}
		}
	}

	// @Override
	// protected void containerTick() {
	// 	super.containerTick();
	// 	this.updateButtonMessages();
	// }

	private String formatCornerName(String nbtKey) {
		if (nbtKey == null || nbtKey.isEmpty()) return "unknown";

		String[] parts = nbtKey.split("_");
		if (parts.length != 2) return nbtKey;

		String prefix = parts[0].substring(0,1).toUpperCase() + parts[0].substring(1);
		String directional = parts[1].toUpperCase();

		switch (directional) {
			case "NORTHWEST" -> directional = "NW";
			case "NORTHEAST" -> directional = "NE";
			case "SOUTHWEST" -> directional = "SW";
			case "SOUTHEAST" -> directional = "SE";
			default -> directional = parts[1].substring(0,1).toUpperCase() + parts[1].substring(1);
		}

		return prefix + " " + directional;
	}
}
