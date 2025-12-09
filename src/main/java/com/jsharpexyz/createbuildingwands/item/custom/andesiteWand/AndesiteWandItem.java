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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
                successfulPlacement = placeLine(level, player, heldWand, clickedPos, clickedFace, targetState);
                break;
            case PLANE:
                successfulPlacement = placePlane(level, player, heldWand, clickedPos, clickedFace, targetState);
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
            // get the players inventory
            // verify that their inventory contains the block they want to place
            // if it does, place the block
            // if it doesn't print a message saying "you don't have enough [block] in your inventory"
            Item requiredItem = targetState.getBlock().asItem();
            ItemStack blockToConsume = new ItemStack(requiredItem, 1);
            if (player.isCreative()) {
                return level.setBlock(placementPos, targetState, 3);
            }
            // non creative logic here, i.e. survival
            else {
                if (player.getInventory().contains(blockToConsume)) {
                    if (consumeItem(player.getInventory(), blockToConsume)) {
                        return level.setBlock(placementPos, targetState, 3);
                    }
                }
                // if the player does not have the block
                else {
                    // TODO: rework to match Create's style, like when placing train tracks
                    Component blockName = blockToConsume.getHoverName();

                    Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("You need ")
                            .append(blockName.copy().withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(" in your inventory."))
                            .withStyle(ChatFormatting.RED),
                        true
                    );
                }
            }
        }
        return false;
    }

    private boolean placeLine(Level level, Player player, ItemStack wand, BlockPos clickedPos, Direction face, BlockState targetState) {

        // add logic for shift-right clicking to cancel the line placement

        if (wand.has(ModDataComponents.WAND_START_POS.get())) {
            BlockPos startPos = wand.get(ModDataComponents.WAND_START_POS.get());
            BlockPos endPos = clickedPos.relative(face);

            List<BlockPos> positions = lineBlockPositions(startPos, endPos);
            int count = positions.size();

            if (!player.isCreative()) {
                if (!canConsumeMultipleItems(player.getInventory(), targetState, count)) {
                    Component blockName = targetState.getBlock().asItem().getDescription();
                    player.displayClientMessage(
                        Component.literal("Need ")
                            .append(Component.literal(String.valueOf(count)).withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(" x "))
                            .append(blockName.copy().withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(" to complete the line."))
                            .withStyle(ChatFormatting.RED),
                            true
                    );
                    return false;
                }
            }

            int placedCount = 0;
            for (BlockPos pos : positions) {
                if (level.getBlockState(pos).canBeReplaced()) {
                    if (level.setBlock(pos, targetState, 3)) {
                        placedCount++;
                    }
                }
            }

            if (!player.isCreative() && placedCount > 0) {
                consumeMultipleItems(player.getInventory(), targetState, placedCount);
            }
            wand.remove(ModDataComponents.WAND_START_POS.get());
            player.displayClientMessage(
                Component.literal("Placed " + placedCount + " blocks in a line.").withStyle(ChatFormatting.GREEN),
                true
            );
            return true;
        }
        // if the wand has no start position, set it
        else {
            BlockPos startPos = clickedPos.relative(face);
            wand.set(ModDataComponents.WAND_START_POS.get(), startPos);

            // notify player
            Component blockName = targetState.getBlock().asItem().getDescription();
            player.displayClientMessage(
                Component.literal("Line start position set. Click another location to place a line of ")
                .append(blockName.copy().withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("."))
                .withStyle(ChatFormatting.AQUA),
                true
            );
            
            return true;
        }
    }
    
    private boolean placePlane(Level level, Player player, ItemStack wand, BlockPos clickedPos, Direction face, BlockState targetState) {
        // implement logic to place a plane of blocks
        // if there is a wand position, place the blocks
        if (wand.has(ModDataComponents.WAND_START_POS.get())) {
            BlockPos startPos = wand.get(ModDataComponents.WAND_START_POS.get());
            BlockPos endPos = clickedPos.relative(face);
            List<BlockPos> positions = planeBlockPositions(startPos, endPos);
            int count = positions.size();

            if (!player.isCreative()) {
                if (!canConsumeMultipleItems(player.getInventory(), targetState, count)) {
                    Component blockName = targetState.getBlock().asItem().getDescription();
                    player.displayClientMessage(
                        Component.literal("Need ")
                            .append(Component.literal(String.valueOf(count)).withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(" x "))
                            .append(blockName.copy().withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(" to complete the plane."))
                            .withStyle(ChatFormatting.RED),
                            true
                    );
                    return false;
                }
            }
            int placedCount = 0;
            for (BlockPos pos : positions) {
                if (level.getBlockState(pos).canBeReplaced()) {
                    if (level.setBlock(pos, targetState, 3)) {
                        placedCount++;
                    }
                }
            }

            if (!player.isCreative() && placedCount > 0) {
                consumeMultipleItems(player.getInventory(), targetState, placedCount);
            }
            wand.remove(ModDataComponents.WAND_START_POS.get());
            player.displayClientMessage(
                Component.literal("Placed " + placedCount + " blocks in a line.").withStyle(ChatFormatting.GREEN),
                true
            );
            return true;
        }
        // if there's no start position, set it
        else {
            BlockPos startPos = clickedPos.relative(face);
            wand.set(ModDataComponents.WAND_START_POS.get(), startPos);

            // notify player
            Component blockName = targetState.getBlock().asItem().getDescription();
            player.displayClientMessage(
                    Component.literal("Plane start position set. Click another location to place a plane of ")
                            .append(blockName.copy().withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal("."))
                            .withStyle(ChatFormatting.AQUA),
                    true);

            return true;
        }
    }

    private List<BlockPos> lineBlockPositions(BlockPos start, BlockPos end) {
        List<BlockPos> positions = new ArrayList<>();

        int dx = end.getX() - start.getX();
        int dy = end.getY() - start.getY();
        int dz = end.getZ() - start.getZ();

        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));

        if (steps == 0) {
            positions.add(start);
            return positions;
        }

        double stepX = (double) dx / steps;
        double stepY = (double) dy / steps;
        double stepZ = (double) dz / steps;

        double currentX = start.getX();
        double currentY = start.getY();
        double currentZ = start.getZ();

        for (int i = 0; i <= steps; i++) {
            BlockPos currentPos = new BlockPos(
                (int) Math.round(currentX),
                (int) Math.round(currentY),
                (int) Math.round(currentZ)
            );

            if (positions.isEmpty() || !positions.get(positions.size() - 1).equals(currentPos)) {
                positions.add(currentPos);
            }

            currentX += stepX;
            currentY += stepY;
            currentZ += stepZ;
        }
        
        if (!positions.contains(end)) {
            positions.add(end);
        }

        return positions;
    }

    private List<BlockPos> planeBlockPositions(BlockPos start, BlockPos end) {
        List<BlockPos> positions = new ArrayList<>();

        // one of the axes must remain the same
        // could add logic for sloped planes but begin with straight ones first
        int minX = Math.min(start.getX(), end.getX());
        int maxX = Math.max(start.getX(), end.getX());
        int minY = Math.min(start.getY(), end.getY());
        int maxY = Math.max(start.getY(), end.getY());
        int minZ = Math.min(start.getZ(), end.getZ());
        int maxZ = Math.max(start.getZ(), end.getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos currentPos = new BlockPos(
                        x,
                        y,
                        z
                    );
                    if (positions.isEmpty() || !positions.get(positions.size() - 1).equals(currentPos)) {
                        positions.add(currentPos);
                    }
                }
            }
        }
        return positions;
    }

    private boolean canConsumeItem(Inventory inventory, ItemStack itemToConsume) {
        return inventory.contains(itemToConsume);
    }

    private boolean consumeItem(Inventory inventory, ItemStack itemToConsume) {
        int slotIndex = inventory.findSlotMatchingItem(itemToConsume);

        if (slotIndex != -1) {
            ItemStack stackInSlot = inventory.getItem(slotIndex);

            stackInSlot.shrink(itemToConsume.getCount());

            if (stackInSlot.isEmpty()) {
                inventory.setItem(slotIndex, ItemStack.EMPTY);
            }

            inventory.setChanged();
            return true;
        }
        return false;
    }

    private boolean canConsumeMultipleItems(Inventory inventory, BlockState blockState, int count) {
        Item requiredItem = blockState.getBlock().asItem();
        int itemsFound = 0;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == requiredItem) {
                itemsFound += stack.getCount();
                if (itemsFound >= count) {
                    return true;
                }
            }
        }
        return false;
    }

    private void consumeMultipleItems(Inventory inventory, BlockState blockState, int count) {
        Item requiredItem = blockState.getBlock().asItem();
        int remainingToConsume = count;

        for (int i = 0; i < inventory.getContainerSize() && remainingToConsume > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == requiredItem) {
                int toTake = Math.min(stack.getCount(), remainingToConsume);
                stack.shrink(toTake);
                remainingToConsume -= toTake;
            }
            inventory.setChanged();
        }
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
