package com.avgusrname.createbuildingwands.component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.copycatsplus.copycats.content.copycat.bytes.CopycatByteBlock;
import com.mojang.serialization.Codec;
import com.simibubi.create.AllBlocks;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public record ByteBlockConfiguration(Map<String, Block> byteTextures) {
    public static final Codec<ByteBlockConfiguration> CODEC = Codec.unboundedMap(
        Codec.STRING,
        BuiltInRegistries.BLOCK.byNameCodec()
    ).xmap(ByteBlockConfiguration::new, ByteBlockConfiguration::byteTextures);

    public static final StreamCodec<RegistryFriendlyByteBuf, ByteBlockConfiguration> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.map(
                            HashMap::new,
                            ByteBufCodecs.STRING_UTF8,
                            ByteBufCodecs.registry(Registries.BLOCK)),
                    ByteBlockConfiguration::byteTextures,
                    ByteBlockConfiguration::new);
    
        public ByteBlockConfiguration() {
            this(new HashMap<>());
        }

        public boolean isByteEnabled(String byteName) {
            return byteTextures.containsKey(byteName) && !(byteTextures.get(byteName) == Blocks.AIR);
        }

        public Block getByteTextureBlock(String byteName) {
            Block block = byteTextures.getOrDefault(byteName, Blocks.AIR);

            return (block == Blocks.AIR) ? AllBlocks.COPYCAT_BASE.get() : block;
        }

        public ByteBlockConfiguration withByteEnabled(String byteName, boolean enabled) {
            Map<String, Block> newMap = new HashMap<>(this.byteTextures);
            if (enabled) {
                Block current = newMap.get(byteName);
                System.out.println("the value of current is: " + current);
                if (current == Blocks.AIR || current == null) {
                    newMap.put(byteName, AllBlocks.COPYCAT_BASE.get());
                    System.out.println("value of newMap is: " + newMap);
                }
            }
            else {
                newMap.remove(byteName);
            }

            return new ByteBlockConfiguration(newMap);
        }

        public ByteBlockConfiguration withByteTextureBlock(String byteName, Block textureBlock) {
            Map<String, Block> newMap = new HashMap<>(this.byteTextures);
            Block blockToStore = (textureBlock == null) ? Blocks.AIR : textureBlock;
            System.out.println("changing byte " + byteName + " with texture of " + textureBlock);

            newMap.put(byteName, blockToStore);
            return new ByteBlockConfiguration(newMap);
        }

        public Set<String> getEnabledBytes() {
            return byteTextures.keySet();
        }

        public Set<Block> getUniqueTextures() {
            return byteTextures.values().stream()
                .filter(block -> !(block == Blocks.AIR))
                .collect(Collectors.toSet());
        }

        public boolean isEmpty() {
            return byteTextures.isEmpty();
        }

        public static Set<String> getAllBytePropertyNames() {
            return CopycatByteBlock.byteMap.keySet();
        }
}
