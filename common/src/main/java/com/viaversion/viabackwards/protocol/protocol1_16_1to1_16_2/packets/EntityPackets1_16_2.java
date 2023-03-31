/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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
import com.viaversion.viaversion.api.minecraft.entities.Entity1_16Types;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_16_2Types;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_16;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.protocol1_16to1_15_2.packets.EntityPackets;
import java.util.Set;

public class EntityPackets1_16_2 extends EntityRewriter<ClientboundPackets1_16_2, Protocol1_16_1To1_16_2> {

    private final Set<String> oldDimensions = Sets.newHashSet("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");
    private boolean warned;

    public EntityPackets1_16_2(Protocol1_16_1To1_16_2 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerTrackerWithData(ClientboundPackets1_16_2.SPAWN_ENTITY, Entity1_16_2Types.FALLING_BLOCK);
        registerSpawnTracker(ClientboundPackets1_16_2.SPAWN_MOB);
        registerTracker(ClientboundPackets1_16_2.SPAWN_EXPERIENCE_ORB, Entity1_16_2Types.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_16_2.SPAWN_PAINTING, Entity1_16_2Types.PAINTING);
        registerTracker(ClientboundPackets1_16_2.SPAWN_PLAYER, Entity1_16_2Types.PLAYER);
        registerRemoveEntities(ClientboundPackets1_16_2.DESTROY_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_16_2.ENTITY_METADATA, Types1_16.METADATA_LIST);

        protocol.registerClientbound(ClientboundPackets1_16_2.JOIN_GAME, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // Entity ID
                handler(wrapper -> {
                    boolean hardcore = wrapper.read(Type.BOOLEAN);
                    short gamemode = wrapper.read(Type.UNSIGNED_BYTE);
                    if (hardcore) {
                        gamemode |= 0x08;
                    }
                    wrapper.write(Type.UNSIGNED_BYTE, gamemode);
                });
                map(Type.BYTE); // Previous Gamemode
                map(Type.STRING_ARRAY); // World List
                handler(wrapper -> {
                    CompoundTag registry = wrapper.read(Type.NBT);
                    if (wrapper.user().getProtocolInfo().getProtocolVersion() <= ProtocolVersion.v1_15_2.getVersion()) {
                        // Store biomes for <1.16 client handling
                        CompoundTag biomeRegistry = registry.get("minecraft:worldgen/biome");
                        ListTag biomes = biomeRegistry.get("value");
                        BiomeStorage biomeStorage = wrapper.user().get(BiomeStorage.class);
                        biomeStorage.clear();
                        for (Tag biome : biomes) {
                            CompoundTag biomeCompound = (CompoundTag) biome;
                            StringTag name = biomeCompound.get("name");
                            NumberTag id = biomeCompound.get("id");
                            biomeStorage.addBiome(name.getValue(), id.asInt());
                        }
                    } else if (!warned) {
                        warned = true;
                        ViaBackwards.getPlatform().getLogger().warning("1.16 and 1.16.1 clients are only partially supported and may have wrong biomes displayed.");
                    }

                    // Just screw the registry and write the defaults for 1.16 and 1.16.1 clients
                    wrapper.write(Type.NBT, EntityPackets.DIMENSIONS_TAG);

                    CompoundTag dimensionData = wrapper.read(Type.NBT);
                    wrapper.write(Type.STRING, getDimensionFromData(dimensionData));
                });
                map(Type.STRING); // Dimension
                map(Type.LONG); // Seed
                handler(wrapper -> {
                    int maxPlayers = wrapper.read(Type.VAR_INT);
                    wrapper.write(Type.UNSIGNED_BYTE, (short) Math.min(maxPlayers, 255));
                });
                // ...
                handler(getTrackerHandler(Entity1_16_2Types.PLAYER, Type.INT));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_16_2.RESPAWN, wrapper -> {
            CompoundTag dimensionData = wrapper.read(Type.NBT);
            wrapper.write(Type.STRING, getDimensionFromData(dimensionData));
        });
    }

    private String getDimensionFromData(CompoundTag dimensionData) {
        // This may technically break other custom dimension settings for 1.16/1.16.1 clients, so those cases are considered semi "unsupported" here
        StringTag effectsLocation = dimensionData.get("effects");
        return effectsLocation != null && oldDimensions.contains(effectsLocation.getValue()) ?
                effectsLocation.getValue() : "minecraft:overworld";
    }

    @Override
    protected void registerRewrites() {
        registerMetaTypeHandler(Types1_16.META_TYPES.itemType, Types1_16.META_TYPES.blockStateType,
                Types1_16.META_TYPES.particleType, Types1_16.META_TYPES.optionalComponentType, Types1_16.META_TYPES.componentType);

        mapTypes(Entity1_16_2Types.values(), Entity1_16Types.class);
        mapEntityTypeWithData(Entity1_16_2Types.PIGLIN_BRUTE, Entity1_16_2Types.PIGLIN).jsonName();

        filter().filterFamily(Entity1_16_2Types.ABSTRACT_PIGLIN).index(15).toIndex(16);
        filter().filterFamily(Entity1_16_2Types.ABSTRACT_PIGLIN).index(16).toIndex(15);
    }

    @Override
    public EntityType typeFromId(int typeId) {
        return Entity1_16_2Types.getTypeFromId(typeId);
    }
}
