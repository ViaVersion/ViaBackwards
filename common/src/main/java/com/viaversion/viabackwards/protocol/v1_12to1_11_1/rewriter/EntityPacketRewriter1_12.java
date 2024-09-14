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

package com.viaversion.viabackwards.protocol.v1_12to1_11_1.rewriter;

import com.viaversion.viabackwards.api.rewriters.LegacyEntityRewriter;
import com.viaversion.viabackwards.protocol.v1_12to1_11_1.Protocol1_12To1_11_1;
import com.viaversion.viabackwards.protocol.v1_12to1_11_1.storage.ParrotStorage;
import com.viaversion.viabackwards.protocol.v1_12to1_11_1.storage.ShoulderTracker;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_12;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_12;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_12;
import com.viaversion.viaversion.api.type.types.version.Types1_9;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.v1_11_1to1_12.packet.ClientboundPackets1_12;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;
import com.viaversion.nbt.tag.CompoundTag;

public class EntityPacketRewriter1_12 extends LegacyEntityRewriter<ClientboundPackets1_12, Protocol1_12To1_11_1> {

    public EntityPacketRewriter1_12(Protocol1_12To1_11_1 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_12.ADD_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity id
                map(Types.UUID); // 1 - UUID
                map(Types.BYTE); // 2 - Type
                map(Types.DOUBLE); // 3 - x
                map(Types.DOUBLE); // 4 - y
                map(Types.DOUBLE); // 5 - z
                map(Types.BYTE); // 6 - Pitch
                map(Types.BYTE); // 7 - Yaw
                map(Types.INT); // 8 - data

                // Track Entity
                handler(getObjectTrackerHandler());
                handler(getObjectRewriter(EntityTypes1_12.ObjectType::findById));

                handler(protocol.getItemRewriter().getFallingBlockHandler());
            }
        });

        registerTracker(ClientboundPackets1_12.ADD_EXPERIENCE_ORB, EntityTypes1_12.EntityType.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_12.ADD_GLOBAL_ENTITY, EntityTypes1_12.EntityType.LIGHTNING_BOLT);

        protocol.registerClientbound(ClientboundPackets1_12.ADD_MOB, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity id
                map(Types.UUID); // 1 - UUID
                map(Types.VAR_INT); // 2 - Entity Type
                map(Types.DOUBLE); // 3 - X
                map(Types.DOUBLE); // 4 - Y
                map(Types.DOUBLE); // 5 - Z
                map(Types.BYTE); // 6 - Yaw
                map(Types.BYTE); // 7 - Pitch
                map(Types.BYTE); // 8 - Head Pitch
                map(Types.SHORT); // 9 - Velocity X
                map(Types.SHORT); // 10 - Velocity Y
                map(Types.SHORT); // 11 - Velocity Z
                map(Types1_12.ENTITY_DATA_LIST, Types1_9.ENTITY_DATA_LIST); // 12 - Entity data

                // Track entity
                handler(getTrackerHandler());

                // Rewrite entity type / data
                handler(getMobSpawnRewriter1_11(Types1_9.ENTITY_DATA_LIST));
            }
        });

        registerTracker(ClientboundPackets1_12.ADD_PAINTING, EntityTypes1_12.EntityType.PAINTING);

        protocol.registerClientbound(ClientboundPackets1_12.ADD_PLAYER, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity ID
                map(Types.UUID); // 1 - Player UUID
                map(Types.DOUBLE); // 2 - X
                map(Types.DOUBLE); // 3 - Y
                map(Types.DOUBLE); // 4 - Z
                map(Types.BYTE); // 5 - Yaw
                map(Types.BYTE); // 6 - Pitch
                map(Types1_12.ENTITY_DATA_LIST, Types1_9.ENTITY_DATA_LIST); // 7 - Entity data list

                handler(getTrackerAndDataHandler(Types1_9.ENTITY_DATA_LIST, EntityTypes1_12.EntityType.PLAYER));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_12.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // 0 - Entity ID
                map(Types.UNSIGNED_BYTE); // 1 - Gamemode
                map(Types.INT); // 2 - Dimension

                handler(getDimensionHandler());
                handler(getPlayerTrackerHandler());

                handler(wrapper -> {
                    ShoulderTracker tracker = wrapper.user().get(ShoulderTracker.class);
                    tracker.setEntityId(wrapper.get(Types.INT, 0));
                });

                // Send fake inventory achievement
                handler(packetWrapper -> {
                    PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets1_9_3.AWARD_STATS, packetWrapper.user());

                    wrapper.write(Types.VAR_INT, 1);
                    wrapper.write(Types.STRING, "achievement.openInventory");
                    wrapper.write(Types.VAR_INT, 1);

                    wrapper.scheduleSend(Protocol1_12To1_11_1.class);
                });
            }
        });

        registerRespawn(ClientboundPackets1_12.RESPAWN);
        registerRemoveEntities(ClientboundPackets1_12.REMOVE_ENTITIES);
        registerSetEntityData(ClientboundPackets1_12.SET_ENTITY_DATA, Types1_12.ENTITY_DATA_LIST, Types1_9.ENTITY_DATA_LIST);

        protocol.registerClientbound(ClientboundPackets1_12.UPDATE_ATTRIBUTES, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                map(Types.INT);
                handler(wrapper -> {
                    int size = wrapper.get(Types.INT, 0);
                    int newSize = size;
                    for (int i = 0; i < size; i++) {
                        String key = wrapper.read(Types.STRING);
                        // Remove new attribute
                        if (key.equals("generic.flyingSpeed")) {
                            newSize--;
                            wrapper.read(Types.DOUBLE);
                            int modSize = wrapper.read(Types.VAR_INT);
                            for (int j = 0; j < modSize; j++) {
                                wrapper.read(Types.UUID);
                                wrapper.read(Types.DOUBLE);
                                wrapper.read(Types.BYTE);
                            }
                        } else {
                            wrapper.write(Types.STRING, key);
                            wrapper.passthrough(Types.DOUBLE);
                            int modSize = wrapper.passthrough(Types.VAR_INT);
                            for (int j = 0; j < modSize; j++) {
                                wrapper.passthrough(Types.UUID);
                                wrapper.passthrough(Types.DOUBLE);
                                wrapper.passthrough(Types.BYTE);
                            }
                        }
                    }

                    if (newSize != size) {
                        wrapper.set(Types.INT, 0, newSize);
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        mapEntityTypeWithData(EntityTypes1_12.EntityType.PARROT, EntityTypes1_12.EntityType.BAT).plainName().spawnEntityData(storage -> storage.add(new EntityData(12, EntityDataTypes1_12.BYTE, (byte) 0x00)));
        mapEntityTypeWithData(EntityTypes1_12.EntityType.ILLUSIONER, EntityTypes1_12.EntityType.EVOKER).plainName();

        filter().handler((event, data) -> {
            if (data.dataType() == EntityDataTypes1_12.COMPONENT) {
                protocol.getComponentRewriter().processText(event.user(), (JsonElement) data.getValue());
            }
        });

        // Handle Illager
        filter().type(EntityTypes1_12.EntityType.EVOKER).removeIndex(12);

        filter().type(EntityTypes1_12.EntityType.ILLUSIONER).index(0).handler((event, data) -> {
            byte mask = (byte) data.getValue();

            if ((mask & 0x20) == 0x20) {
                mask &= ~0x20;
            }

            data.setValue(mask);
        });

        // Create Parrot storage
        filter().type(EntityTypes1_12.EntityType.PARROT).handler((event, data) -> {
            StoredEntityData entityData = storedEntityData(event);
            if (!entityData.has(ParrotStorage.class)) {
                entityData.put(new ParrotStorage());
            }
        });
        // Parrot remove animal entity data
        filter().type(EntityTypes1_12.EntityType.PARROT).cancel(12); // Is baby
        filter().type(EntityTypes1_12.EntityType.PARROT).index(13).handler((event, data) -> {
            StoredEntityData entityData = storedEntityData(event);
            ParrotStorage storage = entityData.get(ParrotStorage.class);
            boolean isSitting = (((byte) data.getValue()) & 0x01) == 0x01;
            boolean isTamed = (((byte) data.getValue()) & 0x04) == 0x04;

            if (!storage.isTamed() && isTamed) {
                // TODO do something to let the user know it's done
            }

            storage.setTamed(isTamed);

            if (isSitting) {
                event.setIndex(12);
                data.setValue((byte) 0x01);
                storage.setSitting(true);
            } else if (storage.isSitting()) {
                event.setIndex(12);
                data.setValue((byte) 0x00);
                storage.setSitting(false);
            } else {
                event.cancel();
            }
        }); // Flags (Is sitting etc, might be useful in the future
        filter().type(EntityTypes1_12.EntityType.PARROT).cancel(14); // Owner
        filter().type(EntityTypes1_12.EntityType.PARROT).cancel(15); // Variant

        // Left shoulder entity data
        filter().type(EntityTypes1_12.EntityType.PLAYER).index(15).handler((event, data) -> {
            CompoundTag tag = (CompoundTag) data.getValue();
            ShoulderTracker tracker = event.user().get(ShoulderTracker.class);

            if (tag.isEmpty() && tracker.getLeftShoulder() != null) {
                tracker.setLeftShoulder(null);
                tracker.update();
            } else if (tag.getStringTag("id") != null && event.entityId() == tracker.getEntityId()) {
                String id = tag.getStringTag("id").getValue();
                if (tracker.getLeftShoulder() == null || !tracker.getLeftShoulder().equals(id)) {
                    tracker.setLeftShoulder(id);
                    tracker.update();
                }
            }

            event.cancel();
        });

        // Right shoulder entity data
        filter().type(EntityTypes1_12.EntityType.PLAYER).index(16).handler((event, data) -> {
            CompoundTag tag = (CompoundTag) event.data().getValue();
            ShoulderTracker tracker = event.user().get(ShoulderTracker.class);

            if (tag.isEmpty() && tracker.getRightShoulder() != null) {
                tracker.setRightShoulder(null);
                tracker.update();
            } else if (tag.getStringTag("id") != null && event.entityId() == tracker.getEntityId()) {
                String id = tag.getStringTag("id").getValue();
                if (tracker.getRightShoulder() == null || !tracker.getRightShoulder().equals(id)) {
                    tracker.setRightShoulder(id);
                    tracker.update();
                }
            }

            event.cancel();
        });
    }

    @Override
    public EntityType typeFromId(int typeId) {
        return EntityTypes1_12.getTypeFromId(typeId, false);
    }

    @Override
    public EntityType objectTypeFromId(int typeId) {
        return EntityTypes1_12.getTypeFromId(typeId, true);
    }
}
