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

public class ByteConfigScreen extends MultiStateConfigScreen<ByteConfigMenu> {

	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CreateBuildingWands.MODID, "textures/gui/byte_config.png");

	public ByteConfigScreen(ByteConfigMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 268;
		this.imageHeight = 214;
	}

	@Override
	protected ResourceLocation getTexture() { return TEXTURE; }

	protected String formatKeyName(String key) {
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
}
