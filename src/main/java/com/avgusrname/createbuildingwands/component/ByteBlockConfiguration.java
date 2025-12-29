package com.avgusrname.createbuildingwands.component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.copycatsplus.copycats.content.copycat.bytes.CopycatByteBlock;
import com.mojang.serialization.Codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record ByteBlockConfiguration(Map<String, ItemStack> byteTextures) {
    public static final Codec<ByteBlockConfiguration> CODEC = Codec.unboundedMap(
        Codec.STRING,
        ItemStack.OPTIONAL_CODEC
    ).xmap(ByteBlockConfiguration::new, ByteBlockConfiguration::byteTextures);

    public static final StreamCodec<RegistryFriendlyByteBuf, ByteBlockConfiguration> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public ByteBlockConfiguration decode(RegistryFriendlyByteBuf buffer) {
                int size = buffer.readVarInt();
                Map<String, ItemStack> map = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    String key = buffer.readUtf();
                    ItemStack value = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                    map.put(key, value);
                }
                return new ByteBlockConfiguration(map);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, ByteBlockConfiguration config) {
                buffer.writeVarInt(config.byteTextures.size());
                for (Map.Entry<String, ItemStack> entry : config.byteTextures.entrySet()) {
                    buffer.writeUtf(entry.getKey());
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, entry.getValue());
                }
            }
        };
    
        public ByteBlockConfiguration() {
            this(new HashMap<>());
        }

        public boolean isByteEnabled(String byteName) {
            return byteTextures.containsKey(byteName) && !byteTextures.get(byteName).isEmpty();
        }

        public ItemStack getByteTexture(String byteName) {
            return byteTextures.getOrDefault(byteName, ItemStack.EMPTY);
        }

        public ByteBlockConfiguration withByteEnabled(String byteName, boolean enabled) {
            Map<String, ItemStack> newMap = new HashMap<>(byteTextures);
            if (enabled) {
                if (!newMap.containsKey(byteName)) {
                    newMap.put(byteName, ItemStack.EMPTY);
                }
                else {
                    newMap.remove(byteName);
                }
            }
            return new ByteBlockConfiguration(newMap);
        }

        public ByteBlockConfiguration withByteTexture(String byteName, ItemStack texture) {
            Map<String, ItemStack> newMap = new HashMap<>(byteTextures);
            newMap.put(byteName, texture.copy());
            return new ByteBlockConfiguration(newMap);
        }

        public Set<String> getEnabledBytes() {
            return byteTextures.keySet();
        }

        public Set<ItemStack> getUniqueTextures() {
            Map<Item, ItemStack> uniqueItems = new HashMap<>();

            for (ItemStack stack : byteTextures.values()) {
                if (!stack.isEmpty()) {
                    Item item = stack.getItem();
                    uniqueItems.putIfAbsent(item, stack);
                }
            }

            return new HashSet<>(uniqueItems.values());
        }

        public boolean isEmpty() {
            return byteTextures.isEmpty();
        }

        public static Set<String> getAllBytePropertyNames() {
            return CopycatByteBlock.byteMap.keySet();
        }
}
