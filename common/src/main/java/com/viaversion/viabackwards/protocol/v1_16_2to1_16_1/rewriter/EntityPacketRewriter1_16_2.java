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
package com.viaversion.viabackwards.protocol.v1_16_2to1_16_1.rewriter;

import com.google.common.collect.Sets;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_16_2to1_16_1.Protocol1_16_2To1_16_1;
import com.viaversion.viabackwards.protocol.v1_16_2to1_16_1.storage.BiomeStorage;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_16_2;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_16;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.data.DimensionRegistries1_16;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ClientboundPackets1_16_2;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.TagUtil;
import java.util.Set;

public class EntityPacketRewriter1_16_2 extends EntityRewriter<ClientboundPackets1_16_2, Protocol1_16_2To1_16_1> {

    private final Set<String> oldDimensions = Sets.newHashSet("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");
    private boolean warned;

    public EntityPacketRewriter1_16_2(Protocol1_16_2To1_16_1 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerTrackerWithData(ClientboundPackets1_16_2.ADD_ENTITY, EntityTypes1_16_2.FALLING_BLOCK);
        registerSpawnTracker(ClientboundPackets1_16_2.ADD_MOB);
        registerTracker(ClientboundPackets1_16_2.ADD_EXPERIENCE_ORB, EntityTypes1_16_2.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_16_2.ADD_PAINTING, EntityTypes1_16_2.PAINTING);
        registerTracker(ClientboundPackets1_16_2.ADD_PLAYER, EntityTypes1_16_2.PLAYER);
        registerRemoveEntities(ClientboundPackets1_16_2.REMOVE_ENTITIES);
        registerSetEntityData(ClientboundPackets1_16_2.SET_ENTITY_DATA, Types1_16.ENTITY_DATA_LIST);

        protocol.registerClientbound(ClientboundPackets1_16_2.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // Entity ID
                handler(wrapper -> {
                    boolean hardcore = wrapper.read(Types.BOOLEAN);
                    short gamemode = wrapper.read(Types.BYTE);
                    if (hardcore) {
                        gamemode |= 0x08;
                    }
                    wrapper.write(Types.UNSIGNED_BYTE, gamemode);
                });
                map(Types.BYTE); // Previous Gamemode
                map(Types.STRING_ARRAY); // World List
                handler(wrapper -> {
                    CompoundTag registry = wrapper.read(Types.NAMED_COMPOUND_TAG);
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
                    } else if (!warned && !ViaBackwards.getConfig().suppressEmulationWarnings()) {
                        warned = true;
                        protocol.getLogger().warning("1.16 and 1.16.1 clients are only partially supported and may have wrong biomes displayed.");
                    }

                    // Just screw the registry and write the defaults for 1.16 and 1.16.1 clients
                    wrapper.write(Types.NAMED_COMPOUND_TAG, DimensionRegistries1_16.getDimensionsTag());

                    CompoundTag dimensionData = wrapper.read(Types.NAMED_COMPOUND_TAG);
                    wrapper.write(Types.STRING, getDimensionFromData(dimensionData));
                });
                map(Types.STRING); // Dimension
                map(Types.LONG); // Seed
                handler(wrapper -> {
                    int maxPlayers = wrapper.read(Types.VAR_INT);
                    wrapper.write(Types.UNSIGNED_BYTE, (short) Math.min(maxPlayers, 255));
                });
                // ...
                handler(getPlayerTrackerHandler());
            }
        });

        protocol.registerClientbound(ClientboundPackets1_16_2.RESPAWN, wrapper -> {
            CompoundTag dimensionData = wrapper.read(Types.NAMED_COMPOUND_TAG);
            wrapper.write(Types.STRING, getDimensionFromData(dimensionData));

            tracker(wrapper.user()).clearEntities();
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
        registerEntityDataTypeHandler(Types1_16.ENTITY_DATA_TYPES.itemType, null, Types1_16.ENTITY_DATA_TYPES.optionalBlockStateType,
            Types1_16.ENTITY_DATA_TYPES.particleType, Types1_16.ENTITY_DATA_TYPES.componentType, Types1_16.ENTITY_DATA_TYPES.optionalComponentType);

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
