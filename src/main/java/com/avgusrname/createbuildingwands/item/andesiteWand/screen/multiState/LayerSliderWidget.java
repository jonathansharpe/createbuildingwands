package com.avgusrname.createbuildingwands.item.andesiteWand.screen.multiState;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class LayerSliderWidget extends AbstractSliderButton {
    private final int min;
    private final int max;
    private final Consumer<Integer> onValueChanged;
    private final String label;

    public LayerSliderWidget(int x, int y, int width, int height, String label, int min, int max, int initialValue, Consumer<Integer> onValueChanged) {
        super(x, y, width, height, Component.empty(), (double)(initialValue - min) / (max - min));
        this.min = min;
        this.max = max;
        this.label = label;
        this.onValueChanged = onValueChanged;
        updateMessage();
    }

    public int getValue() {
        return min + (int) Math.round(value * (max - min));
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(label + ": " + getValue()));
    }

    @Override
    protected void applyValue() {
        onValueChanged.accept(getValue());
    }

    public void setValue(int newValue) {
        this.value = (double)(newValue - min) / (max - min);
        updateMessage();
    }
}
