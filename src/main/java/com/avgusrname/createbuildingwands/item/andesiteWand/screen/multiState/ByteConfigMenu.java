package com.avgusrname.createbuildingwands.item.andesiteWand.screen.multiState;

import com.avgusrname.createbuildingwands.item.andesiteWand.screen.ModMenuTypes;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandMaterialComponent;
import com.avgusrname.createbuildingwands.util.WandUtils;
import com.copycatsplus.copycats.content.copycat.bytes.CopycatByteBlock;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ByteConfigMenu extends MultiStateConfigMenu {

    public static final int SLOT_SIZE = 18;
    public static final int H_SPACING = 12;
    public static final int V_SPACING = 6;

    public static final int PANEL_INNER_X = 12;
    public static final int PANEL_INNER_Y = 18;

    public static final List<String> ORDERED_KEYS = List.of(
            CopycatByteBlock.BOTTOM_NW.getName(),
            CopycatByteBlock.BOTTOM_NE.getName(),
            CopycatByteBlock.BOTTOM_SW.getName(),
            CopycatByteBlock.BOTTOM_SE.getName(),
            CopycatByteBlock.TOP_NW.getName(),
            CopycatByteBlock.TOP_NE.getName(),
            CopycatByteBlock.TOP_SW.getName(),
            CopycatByteBlock.TOP_SE.getName()
    );

    public ByteConfigMenu(int pContainerId, Inventory pPlayerInventory, InteractionHand pHand) {
        super(ModMenuTypes.BYTE_CONFIG_MENU.get(), pContainerId, pPlayerInventory, pHand, 8);
        loadSlotsFromWand();
    }

    @Override
    public List<String> getOrderedKeys() { return ORDERED_KEYS;}

    @Override
    protected void onMaterialChanged(int slot, ItemStack stack) {
        saveSlotMaterialToWand(ORDERED_KEYS.get(slot), stack);
    }

    @Override
    protected int[] getComponentCoordinates(int index) {
        int row = index % 4;
        int col = index / 4;

        int columnWidth = BTN_WIDTH + 4 + SLOT_SIZE + H_SPACING;

        int x = PANEL_INNER_X + (col * columnWidth);
        int y = PANEL_INNER_Y + (row * (BTN_HEIGHT + V_SPACING));

        return new int[]{x, y};
    }

    @Override
    protected int getInventoryY() { return 132; }

    @Override
    protected int getHotbarY() { return 190; }

    @Override
    protected boolean isPartActive(int slot) {
        WandMaterialComponent component = WandUtils.getCurrentComponent(this.wandItem);
        return component.isActive(ORDERED_KEYS.get(slot));
    }

    @Override
    public @NotNull MenuType<?> getType() {
        return ModMenuTypes.BYTE_CONFIG_MENU.get();
    }

    public ByteConfigMenu(int pContainerId, Inventory pPlayerInventory, FriendlyByteBuf pExtraData) {
        this(pContainerId, pPlayerInventory, WandUtils.deserializeHand(pExtraData));
    }
}