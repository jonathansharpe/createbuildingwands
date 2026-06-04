package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
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
    private Item item;

    public ByteCornerData() {
        this.material = AllBlocks.COPYCAT_BASE.getDefaultState();
        this.enableCT = true;
        this.item = Items.AIR;
    }

    public ByteCornerData(BlockState material, boolean enableCT, Item item) {
        this.material = material;
        this.enableCT = enableCT;
        this.item = item;
    }

    public static final Codec<ByteCornerData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
                BlockState.CODEC.fieldOf("Material").forGetter(d -> d.material),
                Codec.BOOL.fieldOf("EnableCT").forGetter(d -> d.enableCT),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("Item").forGetter(d -> d.item)
        ).apply(instance, ByteCornerData::new)
    );

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

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public boolean hasConsumedItem() {
        return this.item != null && this.item != Items.AIR;
    }
}
