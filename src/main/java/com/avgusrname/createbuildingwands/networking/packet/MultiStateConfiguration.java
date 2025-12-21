package com.avgusrname.createbuildingwands.networking.packet;

import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.copycatsplus.copycats.content.copycat.bytes.CopycatByteBlock;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public record MultiStateConfiguration(
    Map<String, PartConfig> parts
) {
    public record PartConfig(
        boolean enabled,
        ItemStack texture
    ) {
        public static final Codec<PartConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.BOOL.fieldOf("enabled").forGetter(PartConfig::enabled),
                ItemStack.OPTIONAL_CODEC.fieldOf("texture").forGetter(PartConfig::texture)
            ).apply(instance, PartConfig::new)
        );
        
        public static final StreamCodec<RegistryFriendlyByteBuf, PartConfig> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.BOOL,
                PartConfig::enabled,
                ItemStack.OPTIONAL_STREAM_CODEC,
                PartConfig::texture,
                PartConfig::new
            );
        
        public PartConfig() {
            this(false, ItemStack.EMPTY);
        }

        public PartConfig withEnabled(boolean enabled) {
            return new PartConfig(enabled, this.texture);
        }
        
        public PartConfig withTexture(ItemStack texture) {
            return new PartConfig(this.enabled, texture);
        }
    }
    public static final Codec<MultiStateConfiguration> CODEC = Codec.unboundedMap(
        Codec.STRING,
        PartConfig.CODEC
    ).xmap(MultiStateConfiguration::new, MultiStateConfiguration::parts);

    public static final StreamCodec<RegistryFriendlyByteBuf, MultiStateConfiguration> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public MultiStateConfiguration decode(RegistryFriendlyByteBuf buffer) {
                int size = buffer.readVarInt();
                Map<String, PartConfig> map = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    String key = buffer.readUtf();
                    PartConfig value = PartConfig.STREAM_CODEC.decode(buffer);
                    map.put(key, value);
                }
                return new MultiStateConfiguration(map);
            }
            
            @Override
            public void encode(RegistryFriendlyByteBuf buffer, MultiStateConfiguration config) {
                buffer.writeVarInt(config.parts.size());
                for (Map.Entry<String, PartConfig> entry : config.parts.entrySet()) {
                    buffer.writeUtf(entry.getKey());
                    PartConfig.STREAM_CODEC.encode(buffer, entry.getValue());
                }
            }
        };
    
    public MultiStateConfiguration() {
        this(new HashMap<>());
    }

    public PartConfig getPartConfig(String property) {
        return parts.getOrDefault(property, new PartConfig());
    }

    public boolean isPartEnabled(String property) {
        return getPartConfig(property).enabled();
    }

    public ItemStack getPartTexture(String property) {
        return getPartConfig(property).texture();
    }

    public MultiStateConfiguration withPartConfig(String property, PartConfig config) {
        Map<String, PartConfig> newMap = new HashMap<>(parts);
        if (!config.enabled() && config.texture().isEmpty()) {
            newMap.remove(property);
        }
        else {
            newMap.put(property, config);
        }
        return new MultiStateConfiguration(newMap);
    }

    public MultiStateConfiguration withPartEnabled(String property, boolean enabled) {
        PartConfig current = getPartConfig(property);
        return withPartConfig(property, current.withEnabled(enabled));
    }

    public MultiStateConfiguration withPartTexture(String property, ItemStack texture) {
        PartConfig current = getPartConfig(property);
        return withPartConfig(property, current.withTexture(texture));
    }

    public Set<String> getConfiguredProperties() {
        return parts.keySet();
    }

    public Set<String> getEnabledProperties() {
        return parts.entrySet().stream()
            .filter(e -> e.getValue().enabled())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    public boolean isEmpty() {
        return parts.isEmpty();
    }

    public static MultiStateConfiguration forByteBlock() {
        MultiStateConfiguration config = new MultiStateConfiguration();
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    String property = "x" + x + "y" + y + "z" + z;
                    config = config.withPartEnabled(property, true);
                }
            }
        }
        return config;
    }

    public static List<String> getPropertiesForBlock(Block block){
        if (block instanceof CopycatByteBlock) {
            List<String> properties = new ArrayList<>();
            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++) {
                    for (int z = 0; z < 2; z++) {
                        properties.add("x" + x + "y" + y + "z" + z);
                    }
                }
            }
            return properties;
        }
        else if (block.getDescriptionId().contains("slab")) {
            return List.of("top", "bottom");
        }
        else if (block instanceof IMultiStateCopycatBlock multiBlock) {
            return new ArrayList<>(multiBlock.storageProperties());
        }

        return List.of();
    }
}
