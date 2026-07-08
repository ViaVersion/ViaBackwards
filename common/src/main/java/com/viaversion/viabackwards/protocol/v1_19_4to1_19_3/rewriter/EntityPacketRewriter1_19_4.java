/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
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
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.data.entity.TrackedEntity;
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
import com.viaversion.viaversion.libs.fastutil.ints.IntArrayList;
import com.viaversion.viaversion.libs.fastutil.ints.IntList;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import com.viaversion.viaversion.libs.mcstructs.text.TextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.utils.TextUtils;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.packet.ClientboundPackets1_19_3;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ClientboundPackets1_19_4;
import com.viaversion.viaversion.util.SerializerVersion;
import com.viaversion.viaversion.util.TagUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class EntityPacketRewriter1_19_4 extends EntityRewriter<ClientboundPackets1_19_4, Protocol1_19_4To1_19_3> {

    private static final double TEXT_DISPLAY_LINE_HEIGHT = 0.25;

    public EntityPacketRewriter1_19_4(final Protocol1_19_4To1_19_3 protocol) {
        super(protocol, Types1_19_3.ENTITY_DATA_TYPES.optionalComponentType, Types1_19_3.ENTITY_DATA_TYPES.booleanType);
    }

    @Override
    public void registerPackets() {
        registerSetEntityData(ClientboundPackets1_19_4.SET_ENTITY_DATA, Types1_19_4.ENTITY_DATA_LIST, Types1_19_3.ENTITY_DATA_LIST);

        protocol.replaceClientbound(ClientboundPackets1_19_4.ADD_ENTITY, new PacketHandlers() {
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
                        wrapper.set(Types.DOUBLE, 1, y - TEXT_DISPLAY_LINE_HEIGHT);
                    }

                    // First track (and remap) the entity, then store its position. Positions of all entities are
                    // needed to anchor and move the emulated text display lines when riding other entities
                    getSpawnTrackerWithDataHandler1_19().handle(wrapper);

                    final StoredEntityData data = tracker(wrapper.user()).entityData(entityId);
                    final LinkedEntityStorage storage = new LinkedEntityStorage();
                    final double x = wrapper.get(Types.DOUBLE, 0);
                    final double z = wrapper.get(Types.DOUBLE, 2);
                    storage.setPosition(x, y, z);
                    data.put(storage);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.ADD_PLAYER, wrapper -> {
            final int entityId = wrapper.passthrough(Types.VAR_INT);
            wrapper.passthrough(Types.UUID);
            final double x = wrapper.passthrough(Types.DOUBLE);
            final double y = wrapper.passthrough(Types.DOUBLE);
            final double z = wrapper.passthrough(Types.DOUBLE);

            // Also track players and their positions as possible vehicles of text displays
            final EntityTracker1_19_4 tracker = tracker(wrapper.user());
            tracker.addEntity(entityId, EntityTypes1_19_4.PLAYER);

            final StoredEntityData data = tracker.entityData(entityId);
            final LinkedEntityStorage storage = new LinkedEntityStorage();
            storage.setPosition(x, y, z);
            data.put(storage);
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
                read(Types.OPTIONAL_VAR_INT); // Cause entity
                read(Types.OPTIONAL_VAR_INT); // Direct cause entity
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

        // Track the position of entities to later spawn and move the linked entities.
        // Block display passengers move with their vehicle, but the text display line entities have to be
        // moved manually - both when the display itself moves and when a vehicle moves

        protocol.registerClientbound(ClientboundPackets1_19_4.TELEPORT_ENTITY, wrapper -> {
            final int entityId = wrapper.passthrough(Types.VAR_INT);
            final double x = wrapper.passthrough(Types.DOUBLE);
            final double y = wrapper.passthrough(Types.DOUBLE);
            final double z = wrapper.passthrough(Types.DOUBLE);

            final EntityTracker1_19_4 tracker = tracker(wrapper.user());
            final TrackedEntity entity = tracker.entity(entityId);
            if (entity == null) {
                return;
            }

            final boolean textDisplay = entity.entityType() == EntityTypes1_19_4.TEXT_DISPLAY;
            if (textDisplay) {
                // Move emulated armor stands down to match text display height offsets
                wrapper.set(Types.DOUBLE, 1, y - TEXT_DISPLAY_LINE_HEIGHT);
            }

            final LinkedEntityStorage storage = entity.data().get(LinkedEntityStorage.class);
            if (storage == null) {
                return;
            }

            final double deltaX = x - storage.x();
            final double deltaY = y - storage.y();
            final double deltaZ = z - storage.z();
            storage.setPosition(x, y, z);

            if (textDisplay) {
                teleportTextDisplayLines(wrapper.user(), storage);
            }

            final int[] passengers = storage.passengers();
            if (passengers != null) {
                // Move emulated text display passengers along, keeping their offset to the vehicle
                for (final int passenger : passengers) {
                    final LinkedEntityStorage passengerStorage = riddenTextDisplayStorage(tracker, passenger, entityId);
                    if (passengerStorage != null) {
                        passengerStorage.addRelativePosition(deltaX, deltaY, deltaZ);
                        sendTeleport(wrapper.user(), passenger, passengerStorage.x(), passengerStorage.y() - TEXT_DISPLAY_LINE_HEIGHT, passengerStorage.z());
                        teleportTextDisplayLines(wrapper.user(), passengerStorage);
                    }
                }
            }
        });

        final PacketHandler entityPositionHandler = wrapper -> {
            final int entityId = wrapper.passthrough(Types.VAR_INT);
            final EntityTracker1_19_4 tracker = tracker(wrapper.user());
            final TrackedEntity entity = tracker.entity(entityId);
            if (entity == null || !entity.hasData()) {
                return;
            }

            final LinkedEntityStorage storage = entity.data().get(LinkedEntityStorage.class);
            if (storage == null) {
                return;
            }

            final short compressedX = wrapper.passthrough(Types.SHORT);
            final short compressedY = wrapper.passthrough(Types.SHORT);
            final short compressedZ = wrapper.passthrough(Types.SHORT);
            final double deltaX = compressedX / EntityPositionHandler.RELATIVE_MOVE_FACTOR;
            final double deltaY = compressedY / EntityPositionHandler.RELATIVE_MOVE_FACTOR;
            final double deltaZ = compressedZ / EntityPositionHandler.RELATIVE_MOVE_FACTOR;
            storage.addRelativePosition(deltaX, deltaY, deltaZ);

            if (entity.entityType() == EntityTypes1_19_4.TEXT_DISPLAY) {
                moveTextDisplayLines(wrapper.user(), storage, compressedX, compressedY, compressedZ);
            }

            // Move passengers along with vehicle
            final int[] passengers = storage.passengers();
            if (passengers != null) {
                for (final int passenger : passengers) {
                    final LinkedEntityStorage passengerStorage = riddenTextDisplayStorage(tracker, passenger, entityId);
                    if (passengerStorage == null) {
                        continue;
                    }

                    passengerStorage.addRelativePosition(deltaX, deltaY, deltaZ);
                    sendRelativeMove(wrapper.user(), passenger, compressedX, compressedY, compressedZ);
                    moveTextDisplayLines(wrapper.user(), passengerStorage, compressedX, compressedY, compressedZ);
                }
            }
        };

        protocol.registerClientbound(ClientboundPackets1_19_4.MOVE_ENTITY_POS, entityPositionHandler);
        protocol.registerClientbound(ClientboundPackets1_19_4.MOVE_ENTITY_POS_ROT, entityPositionHandler);

        protocol.registerClientbound(ClientboundPackets1_19_4.SET_PASSENGERS, wrapper -> {
            final int vehicleId = wrapper.passthrough(Types.VAR_INT);
            final int[] passengers = wrapper.read(Types.VAR_INT_ARRAY_PRIMITIVE);
            final EntityTracker1_19_4 tracker = tracker(wrapper.user());
            final LinkedEntityStorage vehicleStorage = tracker.linkedEntityStorage(vehicleId);
            if (vehicleStorage == null) {
                wrapper.write(Types.VAR_INT_ARRAY_PRIMITIVE, passengers);
                return;
            }

            // Unlink all previous passengers. Those still riding are attached again below
            if (vehicleStorage.passengers() != null) {
                for (final int previousPassenger : vehicleStorage.passengers()) {
                    final LinkedEntityStorage passengerStorage = tracker.linkedEntityStorage(previousPassenger);
                    if (passengerStorage != null && passengerStorage.isVehicle(vehicleId)) {
                        passengerStorage.setVehicleId(null);
                    }
                }
            }

            boolean hasTextDisplay = false;
            final IntList filteredPassengers = new IntArrayList(passengers.length);
            for (final int passenger : passengers) {
                if (tracker.entityType(passenger) != EntityTypes1_19_4.TEXT_DISPLAY) {
                    filteredPassengers.add(passenger);
                    continue;
                }

                hasTextDisplay = true;

                final LinkedEntityStorage passengerStorage = tracker.linkedEntityStorage(passenger);
                if (passengerStorage == null) {
                    filteredPassengers.add(passenger);
                    continue;
                }

                passengerStorage.setVehicleId(vehicleId);
                if (passengerStorage.entities() == null) {
                    // Single-line text display
                    filteredPassengers.add(passenger);
                    continue;
                }

                // Keep the position if it is already close to the vehicle, else we'd have to calculate entity heights for passenger offsets
                final double distanceX = passengerStorage.x() - vehicleStorage.x();
                final double distanceY = passengerStorage.y() - vehicleStorage.y();
                final double distanceZ = passengerStorage.z() - vehicleStorage.z();
                if ((distanceX * distanceX) + (distanceY * distanceY) + (distanceZ * distanceZ) > 4 * 4) {
                    passengerStorage.setPosition(vehicleStorage.x(), vehicleStorage.y(), vehicleStorage.z());
                    sendTeleport(wrapper.user(), passenger, passengerStorage.x(), passengerStorage.y() - TEXT_DISPLAY_LINE_HEIGHT, passengerStorage.z());
                    teleportTextDisplayLines(wrapper.user(), passengerStorage);
                }
            }

            // Only needed to move text display passengers along, so skip storing other passenger lists
            vehicleStorage.setPassengers(hasTextDisplay ? passengers : null);
            wrapper.write(Types.VAR_INT_ARRAY_PRIMITIVE, filteredPassengers.size() == passengers.length ? passengers : filteredPassengers.toIntArray());
        });
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

            // The armor stand name is rendered as a single line an doesn't support new lines,
            // so extra lines have to be moved to their own armor stands, stacked upwards.
            final EntityTracker1_19_4 tracker = tracker(event.user());
            final JsonElement component = data.value();
            if (!containsNewLine(component)) { // Avoid parsing the full component where possible
                clearTextDisplayLines(event.user(), tracker, event.entityId());
                return;
            }

            final TextComponent[] lines = TextUtils.split(SerializerVersion.V1_19_4.toComponent(component), "\n", false);
            if (lines.length <= 1) {
                clearTextDisplayLines(event.user(), tracker, event.entityId());
                return;
            }

            data.setValue(SerializerVersion.V1_19_4.toJson(lines[lines.length - 1])); // Bottom line stays on the actual entity

            final LinkedEntityStorage storage = tracker.linkedEntityStorage(event.entityId());
            if (storage == null) {
                return;
            }

            final int extraLines = lines.length - 1;
            int[] entities = storage.entities();
            if (entities == null || entities.length != extraLines) {
                final boolean mountedOnClient = entities == null && storage.vehicleId() != null;
                tracker.clearLinkedEntities(event.entityId());

                final LinkedEntityStorage vehicleStorage = mountedOnClient ? tracker.linkedEntityStorage(storage.vehicleId()) : null;
                if (vehicleStorage != null) {
                    // The display position wasn't tracked while it was mounted client-side; anchor it to the vehicle
                    storage.setPosition(vehicleStorage.x(), vehicleStorage.y(), vehicleStorage.z());
                }

                entities = new int[extraLines];
                for (int i = 0; i < extraLines; i++) {
                    final double y = storage.y() + (TEXT_DISPLAY_LINE_HEIGHT * i);
                    entities[i] = tracker.spawnEntity(EntityTypes1_19_3.ARMOR_STAND, storage.x(), y, storage.z(), 0);
                }
                storage.setEntities(entities);

                if (vehicleStorage != null) {
                    // The now multi-line display was mounted client-side; dismount and position it manually from now on
                    sendVehiclePassengers(event.user(), tracker, storage.vehicleId());
                    sendTeleport(event.user(), event.entityId(), storage.x(), storage.y() - TEXT_DISPLAY_LINE_HEIGHT, storage.z());
                }
            }

            // Send in inverse order
            for (int i = 0; i < extraLines; i++) {
                sendLineData(event.user(), entities[i], SerializerVersion.V1_19_4.toJson(lines[extraLines - 1 - i]));
            }
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

    private void clearTextDisplayLines(final UserConnection connection, final EntityTracker1_19_4 tracker, final int entityId) {
        final LinkedEntityStorage storage = tracker.linkedEntityStorage(entityId);
        if (storage == null || storage.entities() == null) {
            return;
        }

        tracker.clearLinkedEntities(entityId);
        sendVehiclePassengers(connection, tracker, storage.vehicleId()); // Mount the now single-line display again
    }

    private LinkedEntityStorage riddenTextDisplayStorage(final EntityTracker1_19_4 tracker, final int entityId, final int vehicleId) {
        final TrackedEntity entity = tracker.entity(entityId);
        if (entity == null || entity.entityType() != EntityTypes1_19_4.TEXT_DISPLAY) {
            return null;
        }
        final LinkedEntityStorage storage = entity.data().get(LinkedEntityStorage.class);
        return storage != null && storage.entities() != null && storage.isVehicle(vehicleId) ? storage : null;
    }

    private boolean isMultiLineTextDisplay(final EntityTracker1_19_4 tracker, final int entityId) {
        if (tracker.entityType(entityId) != EntityTypes1_19_4.TEXT_DISPLAY) {
            return false;
        }
        final LinkedEntityStorage storage = tracker.linkedEntityStorage(entityId);
        return storage != null && storage.entities() != null;
    }

    private int[] withoutMultiLineTextDisplays(final EntityTracker1_19_4 tracker, final int[] passengers) {
        final IntList filtered = new IntArrayList(passengers.length);
        for (final int passenger : passengers) {
            if (!isMultiLineTextDisplay(tracker, passenger)) {
                filtered.add(passenger);
            }
        }
        return filtered.size() == passengers.length ? passengers : filtered.toIntArray();
    }

    private void sendVehiclePassengers(final UserConnection connection, final EntityTracker1_19_4 tracker, @Nullable final Integer vehicleId) {
        if (vehicleId == null) {
            return;
        }
        final LinkedEntityStorage vehicleStorage = tracker.linkedEntityStorage(vehicleId);
        if (vehicleStorage == null || vehicleStorage.passengers() == null) {
            return;
        }

        final PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets1_19_3.SET_PASSENGERS, connection);
        wrapper.write(Types.VAR_INT, vehicleId);
        wrapper.write(Types.VAR_INT_ARRAY_PRIMITIVE, withoutMultiLineTextDisplays(tracker, vehicleStorage.passengers()));
        wrapper.send(Protocol1_19_4To1_19_3.class);
    }

    private void teleportTextDisplayLines(final UserConnection connection, final LinkedEntityStorage storage) {
        final int[] entities = storage.entities();
        if (entities == null) {
            return;
        }
        for (int i = 0; i < entities.length; i++) {
            sendTeleport(connection, entities[i], storage.x(), storage.y() + (TEXT_DISPLAY_LINE_HEIGHT * i), storage.z());
        }
    }

    private void moveTextDisplayLines(final UserConnection connection, final LinkedEntityStorage storage, final short deltaX, final short deltaY, final short deltaZ) {
        final int[] entities = storage.entities();
        if (entities == null) {
            return;
        }
        for (final int entity : entities) {
            sendRelativeMove(connection, entity, deltaX, deltaY, deltaZ);
        }
    }

    private void sendTeleport(final UserConnection connection, final int entityId, final double x, final double y, final double z) {
        final PacketWrapper teleport = PacketWrapper.create(ClientboundPackets1_19_3.TELEPORT_ENTITY, connection);
        teleport.write(Types.VAR_INT, entityId);
        teleport.write(Types.DOUBLE, x);
        teleport.write(Types.DOUBLE, y);
        teleport.write(Types.DOUBLE, z);
        teleport.write(Types.BYTE, (byte) 0); // Yaw
        teleport.write(Types.BYTE, (byte) 0); // Pitch
        teleport.write(Types.BOOLEAN, false); // On ground
        teleport.send(Protocol1_19_4To1_19_3.class);
    }

    private void sendRelativeMove(final UserConnection connection, final int entityId, final short deltaX, final short deltaY, final short deltaZ) {
        final PacketWrapper move = PacketWrapper.create(ClientboundPackets1_19_3.MOVE_ENTITY_POS, connection);
        move.write(Types.VAR_INT, entityId);
        move.write(Types.SHORT, deltaX);
        move.write(Types.SHORT, deltaY);
        move.write(Types.SHORT, deltaZ);
        move.write(Types.BOOLEAN, false); // On ground
        move.send(Protocol1_19_4To1_19_3.class);
    }

    private static boolean containsNewLine(final JsonElement element) {
        if (element.isJsonPrimitive()) {
            final JsonPrimitive primitive = element.getAsJsonPrimitive();
            return primitive.isString() && primitive.getAsString().indexOf('\n') != -1;
        } else if (element.isJsonArray()) {
            for (final JsonElement entry : element.getAsJsonArray()) {
                if (containsNewLine(entry)) {
                    return true;
                }
            }
        } else if (element.isJsonObject()) {
            for (final Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if (containsNewLine(entry.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sendLineData(final UserConnection connection, final int entityId, final JsonElement text) {
        final List<EntityData> data = new ArrayList<>();
        data.add(new EntityData(0, Types1_19_3.ENTITY_DATA_TYPES.byteType, (byte) 0x20)); // Invisible
        data.add(new EntityData(2, Types1_19_3.ENTITY_DATA_TYPES.optionalComponentType, text)); // Custom name
        data.add(new EntityData(3, Types1_19_3.ENTITY_DATA_TYPES.booleanType, true)); // Show custom name
        data.add(new EntityData(5, Types1_19_3.ENTITY_DATA_TYPES.booleanType, true)); // No gravity
        data.add(new EntityData(15, Types1_19_3.ENTITY_DATA_TYPES.byteType, (byte) (0x01 | 0x10))); // Small marker

        final PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets1_19_3.SET_ENTITY_DATA, connection);
        wrapper.write(Types.VAR_INT, entityId);
        wrapper.write(Types1_19_3.ENTITY_DATA_LIST, data);
        wrapper.send(Protocol1_19_4To1_19_3.class);
    }

    @Override
    public void onMappingDataLoaded() {
        super.onMappingDataLoaded();
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
