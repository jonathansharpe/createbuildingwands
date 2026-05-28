package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

public class ByteCornerData {

    public enum ByteCopycatCorner {
        BOTTOM_SOUTHEAST("bottom_southeast"),
        BOTTOM_SOUTHWEST("bottom_southwest"),
        BOTTOM_NORTHEAST("bottom_northeast"),
        BOTTOM_NORTHWEST("bottom_northwest"),
        TOP_SOUTHEAST("top_southeast"),
        TOP_SOUTHWEST("top_southwest"),
        TOP_NORTHEAST("top_northeast"),
        TOP_NORTHWEST("top_northwest");

        private final String nbtKey;
        ByteCopycatCorner(String nbtKey) { this.nbtKey = nbtKey; }
        public String getNbtKey() { return this.nbtKey; }
    }

    private BlockState material;
    private boolean enableCT;
    private Item consumedItem;

    public ByteCornerData() {
        this.material = null;
        this.enableCT = true;
        this.consumedItem = Items.AIR;
    }

    public BlockState getMaterial() {
        return material;
    }

    public void setMaterial(BlockState material) {
        this.material = material;
    }

    public boolean isCTEnabled() {
        return enableCT;
    }

    public void setCTEnabled(boolean enableCT) {
        this.enableCT = enableCT;
    }

    public Item getConsumedItem() {
        return consumedItem;
    }

    public void setConsumedItem(Item consumedItem) {
        this.consumedItem = consumedItem;
    }

    public boolean hasConsumedItem() {
        return this.consumedItem != null && this.consumedItem != Items.AIR;
    }
}
