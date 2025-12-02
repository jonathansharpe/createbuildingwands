package com.jsharpexyz.createbuildingwands.item.custom.andesiteWand;

import com.jsharpexyz.createbuildingwands.CreateBuildingWands;
import com.jsharpexyz.createbuildingwands.screen.custom.WandConfigMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;

public class AndesiteWandItem extends Item {
    public AndesiteWandItem(Properties properties) {
        super(properties);
    }

    private static void openConfig(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            MenuProvider containerProvider = new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("Wand config");
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    return new WandConfigMenu(id, inv, hand);
                }

                public void writeExtraData(FriendlyByteBuf buffer) {
                    buffer.writeEnum(hand);
                }
            };

            serverPlayer.openMenu(containerProvider);
        }
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemStack = pPlayer.getItemInHand(pHand);
        if (pPlayer.isShiftKeyDown()) {
            openConfig(pLevel, pPlayer, pHand);
            return InteractionResultHolder.consume(itemStack);
        }
        else {
            //TODO: add logic to place blocks here
            return InteractionResultHolder.pass(itemStack);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Player player = pContext.getPlayer();
        Level level = pContext.getLevel();
        if (player != null && player.isShiftKeyDown()) {
            openConfig(level, player, pContext.getHand());
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
