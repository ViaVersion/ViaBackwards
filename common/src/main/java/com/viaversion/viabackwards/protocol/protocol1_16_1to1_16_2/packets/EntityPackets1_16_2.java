/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2024 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.viabackwards.protocol.protocol1_16_1to1_16_2.packets;

import com.google.common.collect.Sets;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_16_1to1_16_2.Protocol1_16_1To1_16_2;
import com.viaversion.viabackwards.protocol.protocol1_16_1to1_16_2.storage.BiomeStorage;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_16_2;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_16;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.protocol1_16to1_15_2.packets.EntityPackets;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.TagUtil;
import java.util.Set;

public class EntityPackets1_16_2 extends EntityRewriter<ClientboundPackets1_16_2, Protocol1_16_1To1_16_2> {

    private final Set<String> oldDimensions = Sets.newHashSet("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");
    private boolean warned;

    public EntityPackets1_16_2(Protocol1_16_1To1_16_2 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerTrackerWithData(ClientboundPackets1_16_2.SPAWN_ENTITY, EntityTypes1_16_2.FALLING_BLOCK);
        registerSpawnTracker(ClientboundPackets1_16_2.SPAWN_MOB);
        registerTracker(ClientboundPackets1_16_2.SPAWN_EXPERIENCE_ORB, EntityTypes1_16_2.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_16_2.SPAWN_PAINTING, EntityTypes1_16_2.PAINTING);
        registerTracker(ClientboundPackets1_16_2.SPAWN_PLAYER, EntityTypes1_16_2.PLAYER);
        registerRemoveEntities(ClientboundPackets1_16_2.DESTROY_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_16_2.ENTITY_METADATA, Types1_16.METADATA_LIST);

        protocol.registerClientbound(ClientboundPackets1_16_2.JOIN_GAME, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // Entity ID
                handler(wrapper -> {
                    boolean hardcore = wrapper.read(Type.BOOLEAN);
                    short gamemode = wrapper.read(Type.BYTE);
                    if (hardcore) {
                        gamemode |= 0x08;
                    }
                    wrapper.write(Type.UNSIGNED_BYTE, gamemode);
                });
                map(Type.BYTE); // Previous Gamemode
                map(Type.STRING_ARRAY); // World List
                handler(wrapper -> {
                    CompoundTag registry = wrapper.read(Type.NAMED_COMPOUND_TAG);
                    if (wrapper.user().getProtocolInfo().protocolVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
                        // Store biomes for <1.16 client handling
                        ListTag<CompoundTag> biomes = TagUtil.getRegistryEntries(registry, "worldgen/biome");
                        BiomeStorage biomeStorage = wrapper.user().get(BiomeStorage.class);
                        biomeStorage.clear();
                        for (CompoundTag biome : biomes) {
                            StringTag name = biome.getStringTag("name");
                            NumberTag id = biome.getNumberTag("id");
                            biomeStorage.addBiome(name.getValue(), id.asInt());
                        }
                    } else if (!warned) {
                        warned = true;
                        ViaBackwards.getPlatform().getLogger().warning("1.16 and 1.16.1 clients are only partially supported and may have wrong biomes displayed.");
                    }

                    // Just screw the registry and write the defaults for 1.16 and 1.16.1 clients
                    wrapper.write(Type.NAMED_COMPOUND_TAG, EntityPackets.DIMENSIONS_TAG);

                    CompoundTag dimensionData = wrapper.read(Type.NAMED_COMPOUND_TAG);
                    wrapper.write(Type.STRING, getDimensionFromData(dimensionData));
                });
                map(Type.STRING); // Dimension
                map(Type.LONG); // Seed
                handler(wrapper -> {
                    int maxPlayers = wrapper.read(Type.VAR_INT);
                    wrapper.write(Type.UNSIGNED_BYTE, (short) Math.min(maxPlayers, 255));
                });
                // ...
                handler(getTrackerHandler(EntityTypes1_16_2.PLAYER, Type.INT));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_16_2.RESPAWN, wrapper -> {
            CompoundTag dimensionData = wrapper.read(Type.NAMED_COMPOUND_TAG);
            wrapper.write(Type.STRING, getDimensionFromData(dimensionData));
        });
    }

    private String getDimensionFromData(CompoundTag dimensionData) {
        // This may technically break other custom dimension settings for 1.16/1.16.1 clients, so those cases are considered semi "unsupported" here
        StringTag effectsLocation = dimensionData.getStringTag("effects");
        return effectsLocation != null && oldDimensions.contains(Key.namespaced(effectsLocation.getValue())) ?
            effectsLocation.getValue() : "minecraft:overworld";
    }

    @Override
    protected void registerRewrites() {
        registerMetaTypeHandler(Types1_16.META_TYPES.itemType, Types1_16.META_TYPES.blockStateType, null,
            Types1_16.META_TYPES.particleType, Types1_16.META_TYPES.componentType, Types1_16.META_TYPES.optionalComponentType);

        filter().type(EntityTypes1_16_2.ABSTRACT_PIGLIN).index(15).toIndex(16);
        filter().type(EntityTypes1_16_2.ABSTRACT_PIGLIN).index(16).toIndex(15);
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();

        mapEntityTypeWithData(EntityTypes1_16_2.PIGLIN_BRUTE, EntityTypes1_16_2.PIGLIN).jsonName();
    }

    @Override
    public EntityType typeFromId(int typeId) {
        return EntityTypes1_16_2.getTypeFromId(typeId);
    }
}
