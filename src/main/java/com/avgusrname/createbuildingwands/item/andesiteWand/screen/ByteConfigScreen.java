package com.avgusrname.createbuildingwands.item.andesiteWand.screen;

import com.avgusrname.createbuildingwands.CreateBuildingWands;
import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.avgusrname.createbuildingwands.networking.packet.CornerTogglePacket;

import com.copycatsplus.copycats.content.copycat.bytes.CopycatByteBlock;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

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
		
			final int cornerIndex = i;
			String key = ByteConfigMenu.ORDERED_KEYS.get(i);

			this.cornerButtons[i] = Button.builder(Component.literal(formatKeyName(key)), button -> {
				PacketDistributor.sendToServer(new CornerTogglePacket(cornerIndex));
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
	public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// CreateBuildingWands.LOGGER.info("[WandDebug render] about to update buttons and then render everything");
		this.updateButtonMessages();
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	private String formatKeyName(String key) {
		// e.g. "bottom_northwest" -> "Bottom NW"
		return switch (key) {
			case "bottom_northwest" -> "Bottom NW";
			case "bottom_northeast" -> "Bottom NE";
			case "bottom_southwest" -> "Bottom SW";
			case "bottom_southeast" -> "Bottom SE";
			case "top_northwest"    -> "Top NW";
			case "top_northeast"    -> "Top NE";
			case "top_southwest"    -> "Top SW";
			case "top_southeast"    -> "Top SE";
			default -> key;
		};
	}

	private void updateButtonMessages() {
		// INFO any log messages in this function will be called a LOT, like once every second, so log carefully
		// CreateBuildingWands.LOGGER.info("[WandDebug ByteConfigScreen] updateButtonMessages called");

        assert this.minecraft != null;
        if (this.minecraft.player == null) return;

		ItemStack syncedWand = this.minecraft.player.getItemInHand(this.menu.getWandHand());
		WandMaterialComponent materialComponent = syncedWand.getOrDefault(
				ModDataComponents.WAND_MATERIALS.get(),
				WandMaterialComponent.createEmpty()
		);

		BlockState cornerState = materialComponent.cornerState();

		for (int i = 0; i < 8; i++) {
			Button btn = this.cornerButtons[i];
			if (btn == null) continue;

			String key = ByteConfigMenu.ORDERED_KEYS.get(i);
			BooleanProperty prop = CopycatByteBlock.byByte(CopycatByteBlock.byteMap.get(key));
			boolean isActive = cornerState.getValue(prop);

			btn.setMessage(Component.literal(formatKeyName(key) + (isActive ? ": ON" : ": OFF")));
		}
	}
}
