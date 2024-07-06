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
package com.viaversion.viabackwards.protocol.v1_19_4to1_19_3.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.viabackwards.api.entities.storage.EntityReplacement;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_19_4to1_19_3.Protocol1_19_4To1_19_3;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19_4;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_19_3;
import com.viaversion.viaversion.api.type.types.version.Types1_19_4;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.packet.ClientboundPackets1_19_3;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ClientboundPackets1_19_4;
import com.viaversion.viaversion.util.TagUtil;

public final class EntityPacketRewriter1_19_4 extends EntityRewriter<ClientboundPackets1_19_4, Protocol1_19_4To1_19_3> {

    public EntityPacketRewriter1_19_4(final Protocol1_19_4To1_19_3 protocol) {
        super(protocol, Types1_19_3.ENTITY_DATA_TYPES.optionalComponentType, Types1_19_3.ENTITY_DATA_TYPES.booleanType);
    }

    @Override
    public void registerPackets() {
        registerTrackerWithData1_19(ClientboundPackets1_19_4.ADD_ENTITY, EntityTypes1_19_4.FALLING_BLOCK);
        registerRemoveEntities(ClientboundPackets1_19_4.REMOVE_ENTITIES);
        registerSetEntityData(ClientboundPackets1_19_4.SET_ENTITY_DATA, Types1_19_4.ENTITY_DATA_LIST, Types1_19_3.ENTITY_DATA_LIST);

        protocol.registerClientbound(ClientboundPackets1_19_4.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // Entity id
                map(Types.BOOLEAN); // Hardcore
                map(Types.BYTE); // Gamemode
                map(Types.BYTE); // Previous Gamemode
                map(Types.STRING_ARRAY); // World List
                map(Types.NAMED_COMPOUND_TAG); // Dimension registry
                map(Types.STRING); // Dimension key
                map(Types.STRING); // World
                handler(dimensionDataHandler());
                handler(biomeSizeTracker());
                handler(worldDataTrackerHandlerByKey());
                handler(wrapper -> {
                    final CompoundTag registry = wrapper.get(Types.NAMED_COMPOUND_TAG, 0);
                    TagUtil.removeNamespaced(registry, "trim_pattern");
                    TagUtil.removeNamespaced(registry, "trim_material");
                    TagUtil.removeNamespaced(registry, "damage_type");

                    final ListTag<CompoundTag> biomes = TagUtil.getRegistryEntries(registry, "worldgen/biome");
                    for (final CompoundTag biomeTag : biomes) {
                        final CompoundTag biomeData = biomeTag.getCompoundTag("element");
                        final NumberTag hasPrecipitation = biomeData.getNumberTag("has_precipitation");
                        biomeData.putString("precipitation", hasPrecipitation.asByte() == 1 ? "rain" : "none");
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.PLAYER_POSITION, new PacketHandlers() {
            @Override
            protected void register() {
                map(Types.DOUBLE); // X
                map(Types.DOUBLE); // Y
                map(Types.DOUBLE); // Z
                map(Types.FLOAT); // Yaw
                map(Types.FLOAT); // Pitch
                map(Types.BYTE); // Relative arguments
                map(Types.VAR_INT); // Id
                create(Types.BOOLEAN, false); // Dismount vehicle
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.DAMAGE_EVENT, ClientboundPackets1_19_3.ENTITY_EVENT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT, Types.INT); // Entity id
                read(Types.VAR_INT); // Damage type
                read(Types.VAR_INT); // Cause entity
                read(Types.VAR_INT); // Direct cause entity
                handler(wrapper -> {
                    // Source position
                    if (wrapper.read(Types.BOOLEAN)) {
                        wrapper.read(Types.DOUBLE);
                        wrapper.read(Types.DOUBLE);
                        wrapper.read(Types.DOUBLE);
                    }
                });
                create(Types.BYTE, (byte) 2); // Generic hurt
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.HURT_ANIMATION, ClientboundPackets1_19_3.ANIMATE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Entity id
                read(Types.FLOAT); // Yaw
                create(Types.UNSIGNED_BYTE, (short) 1); // Hit
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Dimension
                map(Types.STRING); // World
                handler(worldDataTrackerHandlerByKey());
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.UPDATE_MOB_EFFECT, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity id
            wrapper.passthrough(Types.VAR_INT); // Effect id
            wrapper.passthrough(Types.BYTE); // Amplifier

            // Handle inifinite duration. Use a value the client still accepts without bugging out the display while still being practically infinite
            final int duration = wrapper.read(Types.VAR_INT);
            wrapper.write(Types.VAR_INT, duration == -1 ? 999999 : duration);
        });
    }

    @Override
    public void registerRewrites() {
        filter().handler((event, data) -> {
            int id = data.dataType().typeId();
            if (id >= 25) { // Sniffer state, Vector3f, Quaternion types
                return;
            } else if (id >= 15) { // Optional block state - just map down to block state
                id--;
            }

            data.setDataType(Types1_19_3.ENTITY_DATA_TYPES.byId(id));
        });
        registerEntityDataTypeHandler(Types1_19_3.ENTITY_DATA_TYPES.itemType, null, Types1_19_3.ENTITY_DATA_TYPES.optionalBlockStateType, Types1_19_3.ENTITY_DATA_TYPES.particleType,
            Types1_19_3.ENTITY_DATA_TYPES.componentType, Types1_19_3.ENTITY_DATA_TYPES.optionalComponentType);
        registerBlockStateHandler(EntityTypes1_19_4.ABSTRACT_MINECART, 11);

        filter().type(EntityTypes1_19_4.BOAT).index(11).handler((event, data) -> {
            final int boatType = data.value();
            if (boatType > 4) { // Cherry
                data.setValue(boatType - 1);
            }
        });

        filter().type(EntityTypes1_19_4.TEXT_DISPLAY).index(22).handler(((event, data) -> {
            // Send as custom display name
            event.setIndex(2);
            data.setDataType(Types1_19_3.ENTITY_DATA_TYPES.optionalComponentType);
            event.createExtraData(new EntityData(3, Types1_19_3.ENTITY_DATA_TYPES.booleanType, true)); // Show custom name

            final JsonElement element = data.value();
            protocol.getComponentRewriter().processText(event.user(), element);
        }));
        filter().type(EntityTypes1_19_4.DISPLAY).handler((event, data) -> {
            // TODO Maybe spawn an extra entity to ride the armor stand for blocks and items
            // Remove a large heap of display entity data
            if (event.index() > 7) {
                event.cancel();
            }
        });

        filter().type(EntityTypes1_19_4.INTERACTION).cancel(8); // Width
        filter().type(EntityTypes1_19_4.INTERACTION).cancel(9); // Height
        filter().type(EntityTypes1_19_4.INTERACTION).cancel(10); // Response

        filter().type(EntityTypes1_19_4.SNIFFER).cancel(17); // State
        filter().type(EntityTypes1_19_4.SNIFFER).cancel(18); // Drop seed at tick

        filter().type(EntityTypes1_19_4.ABSTRACT_HORSE).addIndex(18); // Owner UUID
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();

        final EntityReplacement.DataCreator displayDataCreator = storage -> {
            storage.add(new EntityData(0, Types1_19_3.ENTITY_DATA_TYPES.byteType, (byte) 0x20)); // Invisible
            storage.add(new EntityData(5, Types1_19_3.ENTITY_DATA_TYPES.booleanType, true)); // No gravity
            storage.add(new EntityData(15, Types1_19_3.ENTITY_DATA_TYPES.byteType, (byte) (0x01 | 0x10))); // Small marker
        };
        mapEntityTypeWithData(EntityTypes1_19_4.TEXT_DISPLAY, EntityTypes1_19_4.ARMOR_STAND).spawnEntityData(displayDataCreator);
        mapEntityTypeWithData(EntityTypes1_19_4.ITEM_DISPLAY, EntityTypes1_19_4.ARMOR_STAND).spawnEntityData(displayDataCreator);
        mapEntityTypeWithData(EntityTypes1_19_4.BLOCK_DISPLAY, EntityTypes1_19_4.ARMOR_STAND).spawnEntityData(displayDataCreator);

        mapEntityTypeWithData(EntityTypes1_19_4.INTERACTION, EntityTypes1_19_4.ARMOR_STAND).spawnEntityData(displayDataCreator); // Not much we can do about this one

        mapEntityTypeWithData(EntityTypes1_19_4.SNIFFER, EntityTypes1_19_4.RAVAGER).jsonName();
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_19_4.getTypeFromId(type);
    }
}
