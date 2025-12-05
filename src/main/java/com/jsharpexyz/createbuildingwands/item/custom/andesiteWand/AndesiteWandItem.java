package com.jsharpexyz.createbuildingwands.item.custom.andesiteWand;

import com.jsharpexyz.createbuildingwands.CreateBuildingWands;
import com.jsharpexyz.createbuildingwands.component.ModDataComponents;
import com.jsharpexyz.createbuildingwands.item.custom.WandMode;
import com.jsharpexyz.createbuildingwands.item.custom.andesiteWand.screen.WandConfigMenu;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import javax.annotation.Nullable;

public class AndesiteWandItem extends Item {
    public AndesiteWandItem(Properties properties) {
        super(properties);
    }

    public static WandMode getMode(ItemStack stack) {
        WandMode mode = stack.getOrDefault(ModDataComponents.WAND_MODE.get(), WandMode.SINGLE);
        System.out.println("[WAND DEBUG] Getter returning: " + mode.name());
        return mode;
    }

    public static void setMode(ItemStack stack, WandMode mode) {
        stack.set(ModDataComponents.WAND_MODE.get(), mode);
        WandMode confirmedMode = stack.getOrDefault(ModDataComponents.WAND_MODE.get(), WandMode.SINGLE);
        System.out.println("[WAND DEBUG - SETTER] Attempted to set: " + mode.name() + " | Confirmed Value: " + confirmedMode.name());
    }

    private static void openConfig(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            MenuProvider containerProvider = new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("Wand Configuration");
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
        Level level = pContext.getLevel();
        Player player = pContext.getPlayer();
        ItemStack wand = pContext.getItemInHand();

        if (level.isClientSide()) {
            if (player == null) {
                return InteractionResult.PASS;
            }
            else {
                player.swing(pContext.getHand());
            }
        }
        if (player == null || level.isClientSide()) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            openConfig(level, player, pContext.getHand());
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        WandMode mode = getMode(wand);
        BlockPos clickedPos = pContext.getClickedPos();
        Direction face = pContext.getClickedFace();

        BlockState targetState = Blocks.STONE.defaultBlockState();

        boolean success = switch (mode) {
            case SINGLE -> placeSingle(level, player, clickedPos, face, targetState);
            case LINE -> false;
            case RECTANGLE -> false;
            case SPHERE -> false; 
        };

        if (success) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return InteractionResult.FAIL;
    }

    private boolean placeSingle(Level level, Player player, BlockPos clickedPos, Direction face, BlockState targetState) {
        BlockPos placementPos = clickedPos.relative(face);

        if (level.getBlockState(placementPos).canBeReplaced()) {
            return level.setBlock(placementPos, targetState, 3);
        }
        return false;
    }

    private MenuProvider getMenuProvider(UseOnContext pContext) {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.createbuildingwands.wand_config");
            }

            @Override
            @Nullable
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new WandConfigMenu(id, inv, pContext.getHand());
            }
        };
    }
}
