package com.jsharpexyz.createbuildingwands.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.StringRepresentable.EnumCodec;
import net.minecraft.world.item.DyeColor;

import com.mojang.serialization.Codec;

public enum WandMode implements StringRepresentable {
    SINGLE("single", DyeColor.YELLOW),
    LINE("line", DyeColor.BLUE),
    RECTANGLE("rectangle", DyeColor.BLUE),
    SPHERE("sphere", DyeColor.GREEN);

    public static final Codec<WandMode> CODEC = StringRepresentable.fromEnum(WandMode::values);

    private final String name;
    private final DyeColor color;

    WandMode(String name, DyeColor color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public Component getDisplayName() {
        return Component.translatable("mode.createbuildingwands." + name);
    }

    public DyeColor getColor() {
        return color;
    }
}
