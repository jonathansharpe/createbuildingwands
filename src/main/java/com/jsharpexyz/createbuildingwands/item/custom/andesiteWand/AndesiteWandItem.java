package com.jsharpexyz.createbuildingwands.item.custom.andesiteWand;

import com.jsharpexyz.createbuildingwands.CreateBuildingWands;
import com.jsharpexyz.createbuildingwands.component.BlockReferenceComponent;
import com.jsharpexyz.createbuildingwands.component.ModDataComponents;
import com.jsharpexyz.createbuildingwands.item.custom.WandMode;
import com.jsharpexyz.createbuildingwands.item.custom.andesiteWand.screen.WandConfigMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.Blocks;
import javax.annotation.Nullable;

public class AndesiteWandItem extends Item {

    public static final String SELECTED_BLOCK_TAG_KEY = "WandSelectedBlock";

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
        // so this again checks to make sure this is happening on the server side and also that the player is an instance of a ServerPlayer. the ServerPlayer class extends the Player class, but has server attributes, like ServerGamePacketListenerImpl which seems to have to do with the server
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // creating a new instance of a MenuProvider, which is an interface type. it inherits other interfaces, which seemingly wouldn't be possible with other classes. this new instance overrides the necessary classes.
            MenuProvider containerProvider = new MenuProvider() {
                // this just makes the display name which will be displayed when the menu is drawn
                @Override
                public Component getDisplayName() {
                    return Component.literal("Wand Configuration");
                }

                // this makes the menu giving the important info. the level is seemingly no longer relevant since at this point we're in a menu, and not interacting with the world, but merely the players inventory and the menu itself.
                @Override
                @Nullable
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    return new WandConfigMenu(id, inv, hand);
                }
            };

            // this then opens the menu we just created
            serverPlayer.openMenu(containerProvider);
        }
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemStack = pPlayer.getItemInHand(pHand);
        if (pPlayer.isShiftKeyDown()) {
            if (pLevel.isClientSide()) {
                pPlayer.swing(pHand);
            }
            else {
                if (pPlayer instanceof ServerPlayer serverPlayer) {
                    MenuProvider provider = this.getMenuProvider(pHand);

                    serverPlayer.openMenu(
                        provider,
                        buf -> buf.writeEnum(pHand)
                    );
                }
            }
            // openConfig(pLevel, pPlayer, pHand);
            return InteractionResultHolder.consume(itemStack);
        }
        else {
            return InteractionResultHolder.pass(itemStack);
        }
    }

    // this is right clicking the item while facing a block
    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        // the Level type is seemingly the entire dimension that a player is in. it has fields like max size, world border, etc. i guess we need that so we can place blocks in the world
        Level level = pContext.getLevel();

        // the coordinates of the block
        BlockPos clickedPos = pContext.getClickedPos();

        // ItemStack is an 
        ItemStack heldWand = pContext.getItemInHand();

        // the player is the player, makes sense right
        Player player = pContext.getPlayer();

        // presumably this checks to make sure there actually is a player, but shouldn't this be superfluous? how would a wand ever be right clicked if there's no player? idk i will try commenting it out when everything else works
        if (player == null) {
            return InteractionResult.FAIL;
        }

        // checks if the player is shifting, which will bring up the config menu instead. this is the same logic regardless of whether or not the player is looking at a block
        if (player.isCrouching()) {
            // checks to make sure the player is not client side, i think this is to ensure the server actually knows about it?
            if (!level.isClientSide()) {
                // calls the openConfig method, passing through the important context
                openConfig(level, player, pContext.getHand());
            }
            return InteractionResult.CONSUME;
        }

        BlockState targetState = Blocks.STONE.defaultBlockState();
        BlockReferenceComponent blockComponent = heldWand.get(ModDataComponents.WAND_BLOCK.get());

        if (blockComponent != null) {
            ItemStack storedStack = blockComponent.blockStack();
            if (!storedStack.isEmpty()) {
                Block blockToPlace = Block.byItem(storedStack.getItem());
                if (blockToPlace != Blocks.AIR) {
                    targetState = blockToPlace.defaultBlockState();
                }
            }
        }

        if (targetState.is(Blocks.STONE) && (blockComponent == null || blockComponent.blockStack().isEmpty())) {
            return InteractionResult.PASS;
        }

        WandMode currentMode = heldWand.getOrDefault(ModDataComponents.WAND_MODE.get(), WandMode.SINGLE);

        Direction clickedFace = pContext.getClickedFace();

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        boolean successfulPlacement = false;
        switch (currentMode) {
            case SINGLE:
                successfulPlacement = placeSingle(level, player, clickedPos, clickedFace, targetState);
                break;
                // TODO: implement methods for the other building shape modes
            case LINE:
                successfulPlacement = false;
                break;
            case RECTANGLE:
                successfulPlacement = false;
                break;
            case SPHERE:
                successfulPlacement = false;
                break;

        }

        return successfulPlacement ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    public static ItemStack getBlockSelection(Level pLevel, ItemStack wand) {
        CompoundTag tag = getSafeCustomTag(wand);

        if (tag.contains(SELECTED_BLOCK_TAG_KEY, CompoundTag.TAG_COMPOUND)) {
            return ItemStack.parseOptional(pLevel.registryAccess(), tag.getCompound(SELECTED_BLOCK_TAG_KEY));
        }
        return ItemStack.EMPTY;
    }

    public static void setBlockSelection(Level pLevel, ItemStack wand, ItemStack blockStack) {
        CompoundTag tag = getSafeCustomTag(wand);

        if (blockStack.isEmpty()) {
            tag.remove(SELECTED_BLOCK_TAG_KEY);
        }
        else {
            tag.put(SELECTED_BLOCK_TAG_KEY, blockStack.save(pLevel.registryAccess()));
        }

        wand.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static CompoundTag getSafeCustomTag(ItemStack wand) {
        CustomData customData = wand.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.copyTag();
        }
        else {
            return new CompoundTag();
        }
    }

    private boolean placeSingle(Level level, Player player, BlockPos clickedPos, Direction face, BlockState targetState) {
        BlockPos placementPos = clickedPos.relative(face);

        if (level.getBlockState(placementPos).canBeReplaced()) {
            return level.setBlock(placementPos, targetState, 3);
        }
        return false;
    }

    private MenuProvider getMenuProvider(InteractionHand pHand) {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.createbuildingwands.wand_config");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new WandConfigMenu(id, inv, pHand);
            }
        };
    }
}
