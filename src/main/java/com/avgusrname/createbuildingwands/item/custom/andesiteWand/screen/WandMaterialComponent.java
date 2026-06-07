package com.avgusrname.createbuildingwands.item.custom.andesiteWand.screen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;

import java.util.ArrayList;
import java.util.List;

public record WandMaterialComponent(List<ByteCornerData> corners) {
    public static final Codec<WandMaterialComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ByteCornerData.CODEC.listOf().fieldOf("corners").forGetter(WandMaterialComponent::corners)
            ).apply(instance, WandMaterialComponent::new)
    );

    public static WandMaterialComponent createEmptyDefault() {
        List<ByteCornerData> defaultCorners = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            defaultCorners.add(ByteCornerData.defaultEmpty());
        }
        return new WandMaterialComponent(List.copyOf(defaultCorners));
    }

    public WandMaterialComponent withCorner(int index, ByteCornerData newCornerData) {
        List<ByteCornerData> modifiedCorners = new ArrayList<>(this.corners);
        if (index >= 0 && index < modifiedCorners.size()) {
            modifiedCorners.set(index, newCornerData);
        }
        return new WandMaterialComponent(List.copyOf(modifiedCorners));
    }

    public WandMaterialComponent withToggledCorner(int index) {
        List<ByteCornerData> modifiedCorners = new ArrayList<>(this.corners);
        if (index >= 0 && index < modifiedCorners.size()) {
            ByteCornerData oldCorner = modifiedCorners.get(index);
            ByteCornerData toggledCorner = new ByteCornerData(
                    oldCorner.material(),
                    oldCorner.enableCT(),
                    oldCorner.consumedItem(),
                    !oldCorner.isActive()
            );
            modifiedCorners.set(index, toggledCorner);
        }
        return new WandMaterialComponent(List.copyOf(modifiedCorners));
    }
}
