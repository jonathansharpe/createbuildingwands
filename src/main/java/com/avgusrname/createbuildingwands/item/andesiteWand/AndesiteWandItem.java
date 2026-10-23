package com.avgusrname.createbuildingwands.item.andesiteWand;

import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.avgusrname.createbuildingwands.item.WandClientPreview;
import com.avgusrname.createbuildingwands.item.WandMode;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandConfigMenu;
import com.avgusrname.createbuildingwands.item.andesiteWand.screen.WandPlacementContext;
import com.avgusrname.createbuildingwands.networking.packet.WandPreviewPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

// SPAGHETTI
public class AndesiteWandItem extends Item {

    private final WandConsumptionHelper wandConsumptionHelper = new WandConsumptionHelper();
    private final CopycatMaterialHelper copycatMaterialHelper = new CopycatMaterialHelper();
    private final WandPlacementHelper blockPlacementHelper = new WandPlacementHelper(this, this.copycatMaterialHelper, this.wandConsumptionHelper);

    public AndesiteWandItem(Properties properties) {
        super(properties);
    }

    /**
     * sets the wand mode as defined for the user
     * @param wand the wand
     * @param mode the mode at which to set
     */
    public static void setMode(ItemStack wand, WandMode mode) {
        wand.set(ModDataComponents.WAND_MODE.get(), mode);
        WandMode confirmedMode = wand.getOrDefault(ModDataComponents.WAND_MODE.get(), WandMode.SINGLE);
    }

    /**
     * will open the config menu, called when shift-right click or maybe in other instances. it at least separates this logic for easier reading
     * @param level the minecraft world
     * @param player the player holding the wand
     * @param hand the hand which is using the wand
     */
    private static void openConfig(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        MenuProvider containerProvider = new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.literal("Wand Configuration");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
                return new WandConfigMenu(id, inv, hand);
            }
        };

        serverPlayer.openMenu(containerProvider);
    }

    /**
     * function called when a right click while not facing a block is performed
     * @param level the minecraft world
     * @param player the player who performed the right click
     * @param hand the hand that performed the right click
     * @return the item stack to return. in the wands case, it'll just be the wand again.
     */
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack wand = player.getItemInHand(hand);

        if (!player.isCrouching()) {
            return InteractionResultHolder.pass(wand);
        }

        if (level.isClientSide()) {
            player.swing(hand);
            return InteractionResultHolder.consume(wand);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(wand);
        }

        if (wand.has(ModDataComponents.WAND_START_POS.get())) {
            wand.remove(ModDataComponents.WAND_START_POS.get());
            player.displayClientMessage(
                    Component.literal("Start position cleared").withStyle(ChatFormatting.WHITE), true);
            PacketDistributor.sendToPlayer(serverPlayer, new WandPreviewPacket(List.of(), Optional.empty()));
            return InteractionResultHolder.consume(wand);
        }

        serverPlayer.openMenu(getMenuProvider(hand), buf -> buf.writeEnum(hand));
        return InteractionResultHolder.consume(wand);
    }

    /**
     * called when a right click while facing a block is performed
     * @param context the world context
     * @return pass, fail, or consume depending on what happens
     */
    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack heldWand = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();

        if (level.isClientSide()) {
            assert player != null;
            handleClientSide(heldWand, player, clickedPos, clickedFace);
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        if (player.isCrouching()) {
            if (heldWand.has(ModDataComponents.WAND_START_POS.get())) {
                return removeStartPosition(serverPlayer, heldWand, player);
            }
            openConfig(level, player, context.getHand());
            return InteractionResult.CONSUME;
        }

        Block copycatBlock = heldWand.get(ModDataComponents.WAND_BLOCK_COPYCAT.get());
        Block regularBlock = heldWand.get(ModDataComponents.WAND_BLOCK_REGULAR.get());
        boolean isCopycat = copycatBlock != null && !copycatBlock.defaultBlockState().isAir();
        Block blockToPlace = isCopycat ? copycatBlock : regularBlock;

        if ((blockToPlace == null) || blockToPlace.defaultBlockState().isAir()) {
            return InteractionResult.PASS;
        }

        ItemStack materialStack = regularBlock != null ? new ItemStack(regularBlock.asItem()) : ItemStack.EMPTY;
        WandMode currentMode = heldWand.getOrDefault(ModDataComponents.WAND_MODE.get(), WandMode.SINGLE);
        BlockPlaceContext placeContext = new BlockPlaceContext(context);

        boolean placed = switch (currentMode) {
            // TODO this does not work with doors (or any other multi-block blocks) plz fix
            case SINGLE -> {
                if (!wandConsumptionHelper.consumeItemsForPlacement(serverPlayer, heldWand, blockToPlace, 1)) yield false;
                yield blockPlacementHelper.placeBlock(serverPlayer, clickedPos.relative(clickedFace),
                        new WandPlacementContext(blockToPlace, materialStack, isCopycat, placeContext));
            }
            case LINE, PLANE, CUBE ->
                    blockPlacementHelper.placeMultiple(currentMode, serverPlayer, heldWand, placeContext);
            case SPHERE -> false;
        };

        return placed ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    /**
     * removes the start position from the wand
     * @param serverPlayer the player holding the wand on the server side
     * @param heldWand the wand item the player is holding
     * @param player the player holding the wand on the client side
     * @return the InteractionResult, in this case CONSUME since the behavior is always consistent
     */
    private static @NotNull InteractionResult removeStartPosition(ServerPlayer serverPlayer, ItemStack heldWand, Player player) {
        heldWand.remove(ModDataComponents.WAND_START_POS.get());
        player.displayClientMessage(
                Component.literal("Start position cleared").withStyle(ChatFormatting.WHITE), true);
        PacketDistributor.sendToPlayer(serverPlayer, new WandPreviewPacket(List.of(), Optional.empty()));
        return InteractionResult.CONSUME;
    }

    /**
     * does anything client side stuff, notably displaying the block preview
     * @param heldWand the wand whose data is being accessed
     * @param player the player on the client side
     * @param clickedPos the coordinates clicked
     * @param clickedFace the face clicked
     */
    private void handleClientSide(ItemStack heldWand, Player player, BlockPos clickedPos, Direction clickedFace) {
        if (player.isCrouching()) return;

        WandMode currentMode = heldWand.getOrDefault(ModDataComponents.WAND_MODE.get(), WandMode.SINGLE);

        if (heldWand.has(ModDataComponents.WAND_START_POS.get())) {
            WandClientPreview.updateActiveState(null, null);
            WandClientPreview.clearPreviewPositions();
        } else {
            WandClientPreview.updateActiveState(clickedPos.relative(clickedFace), currentMode);

            Block copycat = heldWand.get(ModDataComponents.WAND_BLOCK_COPYCAT.get());
            Block regular = heldWand.get(ModDataComponents.WAND_BLOCK_REGULAR.get());

            ItemStack previewBlock = copycat != null && !copycat.defaultBlockState().isAir()
                    ? new ItemStack(copycat.asItem())
                    : regular != null ? new ItemStack(regular.asItem()) : ItemStack.EMPTY;

            WandClientPreview.setPreviewBlock(previewBlock);
        }
    }

    /**
     * gets the menu or something when right clicked
     * @param pHand the hand of the wand
     * @return the menu or something
     */
    private MenuProvider getMenuProvider(InteractionHand pHand) {
        return new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.translatable("screen.createbuildingwands.wand_config");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
                return new WandConfigMenu(id, inv, pHand);
            }
        };
    }
}
