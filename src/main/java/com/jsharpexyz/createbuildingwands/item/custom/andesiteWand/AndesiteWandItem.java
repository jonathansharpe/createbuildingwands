package com.jsharpexyz.createbuildingwands.item.custom.andesiteWand;

import com.jsharpexyz.createbuildingwands.CreateBuildingWands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.registries.BuiltInRegistries;

public class AndesiteWandItem extends Item {
    public AndesiteWandItem(Properties properties) {
        super(properties);
    }

    private static void openConfig(Level level, Player player) {
        if (!level.isClientSide()) {
            player.sendSystemMessage(Component.literal("load the wand config"));
            //TODO: add the actual logic to open the config menu
        }
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemStack = pPlayer.getItemInHand(pHand);
        if (pPlayer.isShiftKeyDown()) {
            openConfig(pLevel, pPlayer);
            return InteractionResultHolder.success(itemStack);
        }
        else {
            //TODO: add logic to place blocks here
            return InteractionResultHolder.success(itemStack);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Player player = pContext.getPlayer();
        Level level = pContext.getLevel();
        if (player != null && player.isShiftKeyDown()) {
            String itemID = BuiltInRegistries.ITEM.getKey(this).toString();
            CreateBuildingWands.LOGGER.info("WAND CLICK DETECTED; Item ID: {}", itemID);
            openConfig(level, player);
            return InteractionResult.CONSUME;
        }
        else {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal("place blocks with the wand"));
            }
            return InteractionResult.SUCCESS;
        }
    }
}
