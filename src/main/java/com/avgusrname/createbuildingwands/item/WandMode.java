package com.avgusrname.createbuildingwands.item;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;

public enum WandMode implements StringRepresentable {
    // dye color may be unnecessary
    SINGLE("single", DyeColor.YELLOW),
    LINE("line", DyeColor.BLUE),
    PLANE("plane", DyeColor.CYAN),
    CUBE("cube", DyeColor.CYAN),
    SPHERE("sphere", DyeColor.GREEN);

    public static final Codec<WandMode> CODEC = StringRepresentable.fromEnum(WandMode::values);

    private final String name;

    WandMode(String name, DyeColor color) {
        this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }

    public Component getDisplayName() {
        return Component.translatable("mode.createbuildingwands." + name);
    }

}
