package com.avgusrname.createbuildingwands.item.andesiteWand.screen.multiState;

import com.avgusrname.createbuildingwands.CreateBuildingWands;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

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
