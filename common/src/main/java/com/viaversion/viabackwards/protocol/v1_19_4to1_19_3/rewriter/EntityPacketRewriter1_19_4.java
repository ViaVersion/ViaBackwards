/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2025 ViaVersion and contributors
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
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.entities.storage.EntityPositionHandler;
import com.viaversion.viabackwards.api.entities.storage.EntityReplacement;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_19_4to1_19_3.Protocol1_19_4To1_19_3;
import com.viaversion.viabackwards.protocol.v1_19_4to1_19_3.storage.EntityTracker1_19_4;
import com.viaversion.viabackwards.protocol.v1_19_4to1_19_3.storage.LinkedEntityStorage;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19_3;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19_4;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_19_3;
import com.viaversion.viaversion.api.type.types.version.Types1_19_4;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.packet.ClientboundPackets1_19_3;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ClientboundPackets1_19_4;
import com.viaversion.viaversion.util.TagUtil;

public final class EntityPacketRewriter1_19_4 extends EntityRewriter<ClientboundPackets1_19_4, Protocol1_19_4To1_19_3> {

    private static final double TEXT_DISPLAY_Y_OFFSET = -0.25; // Move emulated armor stands down to match text display height offsets

    public EntityPacketRewriter1_19_4(final Protocol1_19_4To1_19_3 protocol) {
        super(protocol, Types1_19_3.ENTITY_DATA_TYPES.optionalComponentType, Types1_19_3.ENTITY_DATA_TYPES.booleanType);
    }

    @Override
    public void registerPackets() {
        registerRemoveEntities(ClientboundPackets1_19_4.REMOVE_ENTITIES);
        registerSetEntityData(ClientboundPackets1_19_4.SET_ENTITY_DATA, Types1_19_4.ENTITY_DATA_LIST, Types1_19_3.ENTITY_DATA_LIST);

        protocol.registerClientbound(ClientboundPackets1_19_4.ADD_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Entity id
                map(Types.UUID); // Entity UUID
                map(Types.VAR_INT); // Entity type
                map(Types.DOUBLE); // X
                map(Types.DOUBLE); // Y
                map(Types.DOUBLE); // Z
                map(Types.BYTE); // Pitch
                map(Types.BYTE); // Yaw
                map(Types.BYTE); // Head yaw
                map(Types.VAR_INT); // Data
                handler(wrapper -> {
                    final int entityId = wrapper.get(Types.VAR_INT, 0);
                    final int entityType = wrapper.get(Types.VAR_INT, 1);

                    if (!ViaBackwards.getConfig().mapDisplayEntities()) {
                        if (entityType == EntityTypes1_19_4.BLOCK_DISPLAY.getId() || entityType == EntityTypes1_19_4.ITEM_DISPLAY.getId() || entityType == EntityTypes1_19_4.TEXT_DISPLAY.getId()) {
                            wrapper.cancel();
                            return;
                        }
                    }

                    final double y = wrapper.get(Types.DOUBLE, 1);
                    if (entityType == EntityTypes1_19_4.TEXT_DISPLAY.getId()) {
                        wrapper.set(Types.DOUBLE, 1, y + TEXT_DISPLAY_Y_OFFSET);
                    }

                    // First track (and remap) entity, then put storage for block display entity
                    getSpawnTrackerWithDataHandler1_19(EntityTypes1_19_4.FALLING_BLOCK).handle(wrapper);
                    if (entityType != EntityTypes1_19_4.BLOCK_DISPLAY.getId()) {
                        return;
                    }

                    final StoredEntityData data = tracker(wrapper.user()).entityData(entityId);
                    if (data != null) {
                        final LinkedEntityStorage storage = new LinkedEntityStorage();
                        final double x = wrapper.get(Types.DOUBLE, 0);
                        final double z = wrapper.get(Types.DOUBLE, 2);
                        storage.setPosition(x, y, z);
                        data.put(storage);
                    }
                });
            }
        });

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

            // Handle infinite duration. Use a value the client still accepts without bugging out the display while still being practically infinite
            final int duration = wrapper.read(Types.VAR_INT);
            wrapper.write(Types.VAR_INT, duration == -1 ? 999999 : duration);
        });

        // Track the position of block display entities to later spawn the linked entities, we will put them
        // as passengers but the spawn position needs to be in the players view distance

        protocol.registerClientbound(ClientboundPackets1_19_4.TELEPORT_ENTITY, wrapper -> {
            final int entityId = wrapper.passthrough(Types.VAR_INT);
            final double x = wrapper.passthrough(Types.DOUBLE);
            final double y = wrapper.passthrough(Types.DOUBLE);
            final double z = wrapper.passthrough(Types.DOUBLE);

            final EntityTracker1_19_4 tracker = tracker(wrapper.user());
            if (tracker.entityType(entityId) == EntityTypes1_19_4.TEXT_DISPLAY) {
                wrapper.set(Types.DOUBLE, 1, y + TEXT_DISPLAY_Y_OFFSET);
            }

            final LinkedEntityStorage storage = tracker.linkedEntityStorage(entityId);
            if (storage != null) {
                storage.setPosition(x, y, z);
            }
        });

        final PacketHandler entityPositionHandler = wrapper -> {
            final int entityId = wrapper.passthrough(Types.VAR_INT);
            final double x = wrapper.passthrough(Types.SHORT) / EntityPositionHandler.RELATIVE_MOVE_FACTOR;
            final double y = wrapper.passthrough(Types.SHORT) / EntityPositionHandler.RELATIVE_MOVE_FACTOR;
            final double z = wrapper.passthrough(Types.SHORT) / EntityPositionHandler.RELATIVE_MOVE_FACTOR;

            final EntityTracker1_19_4 tracker = tracker(wrapper.user());
            final LinkedEntityStorage storage = tracker.linkedEntityStorage(entityId);
            if (storage != null) {
                storage.addRelativePosition(x, y, z);
            }
        };

        protocol.registerClientbound(ClientboundPackets1_19_4.MOVE_ENTITY_POS, entityPositionHandler);
        protocol.registerClientbound(ClientboundPackets1_19_4.MOVE_ENTITY_POS_ROT, entityPositionHandler);
    }

    @Override
    public void registerRewrites() {
        filter().handler((event, data) -> {
            int id = data.dataType().typeId();
            if (id >= 25) { // Sniffer state, Vector3f, Quaternion types
                event.cancel();
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
        }));
        filter().type(EntityTypes1_19_4.BLOCK_DISPLAY).index(22).handler((event, data) -> {
            final int value = data.value();

            final EntityTracker1_19_4 tracker = tracker(event.user());
            tracker.clearLinkedEntities(event.entityId());

            final LinkedEntityStorage storage = tracker.linkedEntityStorage(event.entityId());
            if (storage == null) {
                return;
            }
            final int linkedEntity = tracker.spawnEntity(EntityTypes1_19_3.FALLING_BLOCK, storage.x(), storage.y(), storage.z(), value);
            storage.setEntities(linkedEntity);

            final PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets1_19_3.SET_PASSENGERS, event.user());
            wrapper.write(Types.VAR_INT, event.entityId()); // Entity id
            wrapper.write(Types.VAR_INT_ARRAY_PRIMITIVE, new int[]{linkedEntity}); // Passenger entity ids
            wrapper.send(Protocol1_19_4To1_19_3.class);
        });
        filter().type(EntityTypes1_19_4.ITEM_DISPLAY).index(22).handler((event, data) -> {
            final Item value = data.value();

            final PacketWrapper setEquipment = PacketWrapper.create(ClientboundPackets1_19_3.SET_EQUIPMENT, event.user());
            setEquipment.write(Types.VAR_INT, event.entityId()); // Entity id
            setEquipment.write(Types.BYTE, (byte) 5); // Slot - head
            setEquipment.write(Types.ITEM1_13_2, value);

            setEquipment.send(Protocol1_19_4To1_19_3.class);
        });
        filter().type(EntityTypes1_19_4.DISPLAY).handler((event, data) -> {
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

        final EntityReplacement.EntityDataCreator displayDataCreator = storage -> {
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
