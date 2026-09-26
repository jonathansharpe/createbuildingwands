package com.avgusrname.createbuildingwands.item.andesiteWand.screen;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

public record WandPlacementContext(
        Block blockToPlace,
        ItemStack materialStack,
        boolean isCopycat,
        BlockPlaceContext blockPlaceContext
) {}
