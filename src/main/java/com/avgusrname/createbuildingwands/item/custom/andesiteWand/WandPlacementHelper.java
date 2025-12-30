package com.avgusrname.createbuildingwands.item.custom.andesiteWand;

import java.util.Map;

import com.avgusrname.createbuildingwands.component.ByteBlockConfiguration;
import com.avgusrname.createbuildingwands.component.ModDataComponents;
import com.copycatsplus.copycats.content.copycat.bytes.CopycatByteBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class WandPlacementHelper {
    public static void placeCopycatByte(Level level, BlockPos pos, ItemStack wand, Block copycatByteBlock) {
        if (!(copycatByteBlock instanceof CopycatByteBlock byteBlock)) {
            System.out.println("ERROR: Not a copycat byte block!");
            return;
        }

        ByteBlockConfiguration config = wand.get(ModDataComponents.BYTE_BLOCK_CONFIG.get());

        if (config == null || config.isEmpty()) {
            System.out.println("No byte config found, cannot place byte block");
            return;
        }

        System.out.println("Placing copycat byte with " + config.getEnabledBytes().size() + " enabled bytes");

        BlockState baseState = byteBlock.defaultBlockState();

        for (String byteName : config.getEnabledBytes()) {
            if (CopycatByteBlock.byteMap.containsKey(byteName)) {
                CopycatByteBlock.Byte bite = CopycatByteBlock.byteMap.get(byteName);
                BooleanProperty property = CopycatByteBlock.byByte(bite);

                baseState = baseState.setValue(property, true);
                System.out.println("   Enabling byte: " + byteName);
            }
            else {
                System.out.println("   WARNING: Unknown byte property: " + byteName);
            }
        }

        if (!level.setBlock(pos, baseState, 3)) {
            System.out.println("Failed to place the block at " + pos);
            return;
        }

        System.out.println("Block placed successfully at " + pos);

        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            System.out.println("ERROR: No block entity found!");
            return;
        }

        CompoundTag nbt = new CompoundTag();
        CompoundTag materialData = new CompoundTag();

        for (String byteName : config.getEnabledBytes()) {
            Block textureBlock = config.getByteTextureBlock(byteName);

            if (textureBlock == Blocks.AIR) {
                System.out.println("   WARNING: Byte " + byteName + " has no texture!");
                continue;
            }

            String materialId = BuiltInRegistries.BLOCK.getKey(textureBlock).toString();
            String itemId = BuiltInRegistries.ITEM.getKey(new ItemStack(textureBlock).getItem()).toString();

            System.out.println("   Setting texture for " + byteName + ": " + materialId);

            CompoundTag byteNBT = new CompoundTag();

            CompoundTag materialTag = new CompoundTag();
            materialTag.putString("Name", materialId);
            byteNBT.put("material", materialTag);

            CompoundTag consumedTag = new CompoundTag();
            consumedTag.putString("id", itemId);
            consumedTag.putInt("count", 1);
            byteNBT.put("consumedItem", consumedTag);

            byteNBT.putBoolean("enableCT", true);

            materialData.put(byteName, byteNBT);
        }

        nbt.put("material_data", materialData);

        System.out.println("Loading material data into block entity...");
        System.out.println("NBT: " + nbt);

        be.loadWithComponents(nbt, level.registryAccess());
        be.setChanged();
        level.sendBlockUpdated(pos, baseState, baseState, Block.UPDATE_ALL);

        System.out.println("Copycat byte placement complete!");
    }
}
