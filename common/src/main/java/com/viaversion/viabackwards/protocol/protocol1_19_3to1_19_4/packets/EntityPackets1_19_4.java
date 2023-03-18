/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
 * Copyright (C) 2023 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_19_3to1_19_4.packets;

import com.viaversion.viabackwards.api.entities.storage.EntityData;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_19_3to1_19_4.Protocol1_19_3To1_19_4;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_19_4Types;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_19_3;
import com.viaversion.viaversion.api.type.types.version.Types1_19_4;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_19_3to1_19_1.ClientboundPackets1_19_3;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.ClientboundPackets1_19_4;

public final class EntityPackets1_19_4 extends EntityRewriter<ClientboundPackets1_19_4, Protocol1_19_3To1_19_4> {

    public EntityPackets1_19_4(final Protocol1_19_3To1_19_4 protocol) {
        super(protocol, Types1_19_3.META_TYPES.optionalComponentType, Types1_19_3.META_TYPES.booleanType);
    }

    @Override
    public void registerPackets() {
        registerTrackerWithData1_19(ClientboundPackets1_19_4.SPAWN_ENTITY, Entity1_19_4Types.FALLING_BLOCK);
        registerRemoveEntities(ClientboundPackets1_19_4.REMOVE_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_19_4.ENTITY_METADATA, Types1_19_4.METADATA_LIST, Types1_19_3.METADATA_LIST);

        protocol.registerClientbound(ClientboundPackets1_19_4.JOIN_GAME, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // Entity id
                map(Type.BOOLEAN); // Hardcore
                map(Type.UNSIGNED_BYTE); // Gamemode
                map(Type.BYTE); // Previous Gamemode
                map(Type.STRING_ARRAY); // World List
                map(Type.NBT); // Dimension registry
                map(Type.STRING); // Dimension key
                map(Type.STRING); // World
                handler(dimensionDataHandler());
                handler(biomeSizeTracker());
                handler(worldDataTrackerHandlerByKey());
                handler(wrapper -> {
                    final CompoundTag registry = wrapper.get(Type.NBT, 0);
                    registry.remove("minecraft:trim_pattern");
                    registry.remove("minecraft:trim_material");
                    registry.remove("minecraft:damage_type");

                    final CompoundTag biomeRegistry = registry.get("minecraft:worldgen/biome");
                    final ListTag biomes = biomeRegistry.get("value");
                    for (final Tag biomeTag : biomes) {
                        final CompoundTag biomeData = ((CompoundTag) biomeTag).get("element");
                        final ByteTag hasPrecipitation = biomeData.get("has_precipitation");
                        biomeData.put("precipitation", new StringTag(hasPrecipitation.asByte() == 1 ? "rain" : "none"));
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.PLAYER_POSITION, new PacketHandlers() {
            @Override
            protected void register() {
                map(Type.DOUBLE); // X
                map(Type.DOUBLE); // Y
                map(Type.DOUBLE); // Z
                map(Type.FLOAT); // Yaw
                map(Type.FLOAT); // Pitch
                map(Type.BYTE); // Relative arguments
                map(Type.VAR_INT); // Id
                create(Type.BOOLEAN, false); // Dismount vehicle
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.DAMAGE_EVENT, ClientboundPackets1_19_3.ENTITY_STATUS, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT, Type.INT); // Entity id
                read(Type.VAR_INT); // Damage type
                read(Type.VAR_INT); // Cause entity
                read(Type.VAR_INT); // Direct cause entity
                handler(wrapper -> {
                    // Source position
                    if (wrapper.read(Type.BOOLEAN)) {
                        wrapper.read(Type.DOUBLE);
                        wrapper.read(Type.DOUBLE);
                        wrapper.read(Type.DOUBLE);
                    }
                });
                create(Type.BYTE, (byte) 2); // Generic hurt
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.HIT_ANIMATION, ClientboundPackets1_19_3.ENTITY_ANIMATION, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // Entity id
                read(Type.FLOAT); // Yaw
                create(Type.UNSIGNED_BYTE, (short) 1); // Hit
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Dimension
                map(Type.STRING); // World
                handler(worldDataTrackerHandlerByKey());
            }
        });
    }

    @Override
    public void registerRewrites() {
        filter().handler((event, meta) -> {
            int id = meta.metaType().typeId();
            if (id >= 25) { // Sniffer state, Vector3f, Quaternion types
                return;
            } else if (id >= 15) { // Optional block state - just map down to block state
                id--;
            }

            meta.setMetaType(Types1_19_3.META_TYPES.byId(id));
        });
        registerMetaTypeHandler(Types1_19_3.META_TYPES.itemType, Types1_19_3.META_TYPES.blockStateType, Types1_19_3.META_TYPES.particleType, Types1_19_3.META_TYPES.optionalComponentType);

        filter().filterFamily(Entity1_19_4Types.MINECART_ABSTRACT).index(11).handler((event, meta) -> {
            final int blockState = meta.value();
            meta.setValue(protocol.getMappingData().getNewBlockStateId(blockState));
        });

        filter().filterFamily(Entity1_19_4Types.BOAT).index(11).handler((event, meta) -> {
            final int boatType = meta.value();
            if (boatType > 4) { // Cherry
                meta.setValue(boatType - 1);
            }
        });

        filter().type(Entity1_19_4Types.TEXT_DISPLAY).index(22).handler(((event, meta) -> {
            // Send as custom display name
            event.setIndex(2);
            meta.setMetaType(Types1_19_3.META_TYPES.optionalComponentType);
            event.createExtraMeta(new Metadata(3, Types1_19_3.META_TYPES.booleanType, true)); // Show custom name

            final JsonElement element = meta.value();
            protocol.getTranslatableRewriter().processText(element);
        }));
        filter().filterFamily(Entity1_19_4Types.DISPLAY).handler((event, meta) -> {
            // TODO Maybe spawn an extra entity to ride the armor stand for blocks and items
            // Remove a large heap of display metadata
            if (event.index() > 7) {
                event.cancel();
            }
        });

        filter().type(Entity1_19_4Types.INTERACTION).removeIndex(8); // Width
        filter().type(Entity1_19_4Types.INTERACTION).removeIndex(9); // Height
        filter().type(Entity1_19_4Types.INTERACTION).removeIndex(10); // Response

        filter().type(Entity1_19_4Types.SNIFFER).removeIndex(17); // State
        filter().type(Entity1_19_4Types.SNIFFER).removeIndex(18); // Drop seed at tick

        filter().filterFamily(Entity1_19_4Types.ABSTRACT_HORSE).addIndex(18); // Owner UUID
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();

        final EntityData.MetaCreator displayMetaCreator = storage -> {
            storage.add(new Metadata(0, Types1_19_3.META_TYPES.byteType, (byte) 0x20)); // Invisible
            storage.add(new Metadata(5, Types1_19_3.META_TYPES.booleanType, true)); // No gravity
            storage.add(new Metadata(15, Types1_19_3.META_TYPES.byteType, (byte) (0x01 | 0x10))); // Small marker
        };
        mapEntityTypeWithData(Entity1_19_4Types.TEXT_DISPLAY, Entity1_19_4Types.ARMOR_STAND).spawnMetadata(displayMetaCreator);
        mapEntityTypeWithData(Entity1_19_4Types.ITEM_DISPLAY, Entity1_19_4Types.ARMOR_STAND).spawnMetadata(displayMetaCreator);
        mapEntityTypeWithData(Entity1_19_4Types.BLOCK_DISPLAY, Entity1_19_4Types.ARMOR_STAND).spawnMetadata(displayMetaCreator);

        mapEntityTypeWithData(Entity1_19_4Types.INTERACTION, Entity1_19_4Types.ARMOR_STAND).spawnMetadata(displayMetaCreator); // Not much we can do about this one

        mapEntityTypeWithData(Entity1_19_4Types.SNIFFER, Entity1_19_4Types.RAVAGER).jsonName();
    }

    @Override
    public EntityType typeFromId(final int type) {
        return Entity1_19_4Types.getTypeFromId(type);
    }
}