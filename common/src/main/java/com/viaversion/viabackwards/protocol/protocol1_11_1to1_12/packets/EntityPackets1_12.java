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

package com.viaversion.viabackwards.protocol.protocol1_11_1to1_12.packets;

import com.viaversion.viabackwards.api.rewriters.LegacyEntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_11_1to1_12.Protocol1_11_1To1_12;
import com.viaversion.viabackwards.protocol.protocol1_11_1to1_12.data.ParrotStorage;
import com.viaversion.viabackwards.protocol.protocol1_11_1to1_12.data.ShoulderTracker;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_12;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_12;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_12;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.protocols.protocol1_12to1_11_1.ClientboundPackets1_12;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;

public class EntityPackets1_12 extends LegacyEntityRewriter<ClientboundPackets1_12, Protocol1_11_1To1_12> {

    public EntityPackets1_12(Protocol1_11_1To1_12 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_12.SPAWN_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.UUID); // 1 - UUID
                map(Type.BYTE); // 2 - Type
                map(Type.DOUBLE); // 3 - x
                map(Type.DOUBLE); // 4 - y
                map(Type.DOUBLE); // 5 - z
                map(Type.BYTE); // 6 - Pitch
                map(Type.BYTE); // 7 - Yaw
                map(Type.INT); // 8 - data

                // Track Entity
                handler(getObjectTrackerHandler());
                handler(getObjectRewriter(id -> EntityTypes1_12.ObjectType.findById(id).orElse(null)));

                handler(protocol.getItemRewriter().getFallingBlockHandler());
            }
        });

        registerTracker(ClientboundPackets1_12.SPAWN_EXPERIENCE_ORB, EntityTypes1_12.EntityType.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_12.SPAWN_GLOBAL_ENTITY, EntityTypes1_12.EntityType.WEATHER);

        protocol.registerClientbound(ClientboundPackets1_12.SPAWN_MOB, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.UUID); // 1 - UUID
                map(Type.VAR_INT); // 2 - Entity Type
                map(Type.DOUBLE); // 3 - X
                map(Type.DOUBLE); // 4 - Y
                map(Type.DOUBLE); // 5 - Z
                map(Type.BYTE); // 6 - Yaw
                map(Type.BYTE); // 7 - Pitch
                map(Type.BYTE); // 8 - Head Pitch
                map(Type.SHORT); // 9 - Velocity X
                map(Type.SHORT); // 10 - Velocity Y
                map(Type.SHORT); // 11 - Velocity Z
                map(Types1_12.METADATA_LIST); // 12 - Metadata

                // Track entity
                handler(getTrackerHandler());

                // Rewrite entity type / metadata
                handler(getMobSpawnRewriter(Types1_12.METADATA_LIST));
            }
        });

        registerTracker(ClientboundPackets1_12.SPAWN_PAINTING, EntityTypes1_12.EntityType.PAINTING);

        protocol.registerClientbound(ClientboundPackets1_12.SPAWN_PLAYER, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.UUID); // 1 - Player UUID
                map(Type.DOUBLE); // 2 - X
                map(Type.DOUBLE); // 3 - Y
                map(Type.DOUBLE); // 4 - Z
                map(Type.BYTE); // 5 - Yaw
                map(Type.BYTE); // 6 - Pitch
                map(Types1_12.METADATA_LIST); // 7 - Metadata list

                handler(getTrackerAndMetaHandler(Types1_12.METADATA_LIST, EntityTypes1_12.EntityType.PLAYER));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_12.JOIN_GAME, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // 0 - Entity ID
                map(Type.UNSIGNED_BYTE); // 1 - Gamemode
                map(Type.INT); // 2 - Dimension

                handler(getTrackerHandler(EntityTypes1_12.EntityType.PLAYER, Type.INT));

                handler(getDimensionHandler(1));

                handler(wrapper -> {
                    ShoulderTracker tracker = wrapper.user().get(ShoulderTracker.class);
                    tracker.setEntityId(wrapper.get(Type.INT, 0));
                });

                // Send fake inventory achievement
                handler(packetWrapper -> {
                    PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets1_9_3.STATISTICS, packetWrapper.user());

                    wrapper.write(Type.VAR_INT, 1);
                    wrapper.write(Type.STRING, "achievement.openInventory");
                    wrapper.write(Type.VAR_INT, 1);

                    wrapper.scheduleSend(Protocol1_11_1To1_12.class);
                });
            }
        });

        registerRespawn(ClientboundPackets1_12.RESPAWN);
        registerRemoveEntities(ClientboundPackets1_12.DESTROY_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_12.ENTITY_METADATA, Types1_12.METADATA_LIST);

        protocol.registerClientbound(ClientboundPackets1_12.ENTITY_PROPERTIES, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT);
                map(Type.INT);
                handler(wrapper -> {
                    int size = wrapper.get(Type.INT, 0);
                    int newSize = size;
                    for (int i = 0; i < size; i++) {
                        String key = wrapper.read(Type.STRING);
                        // Remove new attribute
                        if (key.equals("generic.flyingSpeed")) {
                            newSize--;
                            wrapper.read(Type.DOUBLE);
                            int modSize = wrapper.read(Type.VAR_INT);
                            for (int j = 0; j < modSize; j++) {
                                wrapper.read(Type.UUID);
                                wrapper.read(Type.DOUBLE);
                                wrapper.read(Type.BYTE);
                            }
                        } else {
                            wrapper.write(Type.STRING, key);
                            wrapper.passthrough(Type.DOUBLE);
                            int modSize = wrapper.passthrough(Type.VAR_INT);
                            for (int j = 0; j < modSize; j++) {
                                wrapper.passthrough(Type.UUID);
                                wrapper.passthrough(Type.DOUBLE);
                                wrapper.passthrough(Type.BYTE);
                            }
                        }
                    }

                    if (newSize != size) {
                        wrapper.set(Type.INT, 0, newSize);
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        mapEntityTypeWithData(EntityTypes1_12.EntityType.PARROT, EntityTypes1_12.EntityType.BAT).plainName().spawnMetadata(storage -> storage.add(new Metadata(12, MetaType1_12.Byte, (byte) 0x00)));
        mapEntityTypeWithData(EntityTypes1_12.EntityType.ILLUSION_ILLAGER, EntityTypes1_12.EntityType.EVOCATION_ILLAGER).plainName();

        filter().handler((event, meta) -> {
            if (meta.metaType() == MetaType1_12.Chat) {
                ChatPackets1_12.COMPONENT_REWRITER.processText(event.user(), (JsonElement) meta.getValue());
            }
        });

        // Handle Illager
        filter().type(EntityTypes1_12.EntityType.EVOCATION_ILLAGER).cancel(12);
        filter().type(EntityTypes1_12.EntityType.EVOCATION_ILLAGER).index(13).toIndex(12);

        filter().type(EntityTypes1_12.EntityType.ILLUSION_ILLAGER).index(0).handler((event, meta) -> {
            byte mask = (byte) meta.getValue();

            if ((mask & 0x20) == 0x20) {
                mask &= ~0x20;
            }

            meta.setValue(mask);
        });

        // Create Parrot storage
        filter().type(EntityTypes1_12.EntityType.PARROT).handler((event, meta) -> {
            StoredEntityData data = storedEntityData(event);
            if (!data.has(ParrotStorage.class)) {
                data.put(new ParrotStorage());
            }
        });
        // Parrot remove animal metadata
        filter().type(EntityTypes1_12.EntityType.PARROT).cancel(12); // Is baby
        filter().type(EntityTypes1_12.EntityType.PARROT).index(13).handler((event, meta) -> {
            StoredEntityData data = storedEntityData(event);
            ParrotStorage storage = data.get(ParrotStorage.class);
            boolean isSitting = (((byte) meta.getValue()) & 0x01) == 0x01;
            boolean isTamed = (((byte) meta.getValue()) & 0x04) == 0x04;

            if (!storage.isTamed() && isTamed) {
                // TODO do something to let the user know it's done
            }

            storage.setTamed(isTamed);

            if (isSitting) {
                event.setIndex(12);
                meta.setValue((byte) 0x01);
                storage.setSitting(true);
            } else if (storage.isSitting()) {
                event.setIndex(12);
                meta.setValue((byte) 0x00);
                storage.setSitting(false);
            } else {
                event.cancel();
            }
        }); // Flags (Is sitting etc, might be useful in the future
        filter().type(EntityTypes1_12.EntityType.PARROT).cancel(14); // Owner
        filter().type(EntityTypes1_12.EntityType.PARROT).cancel(15); // Variant

        // Left shoulder entity data
        filter().type(EntityTypes1_12.EntityType.PLAYER).index(15).handler((event, meta) -> {
            CompoundTag tag = (CompoundTag) meta.getValue();
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
        filter().type(EntityTypes1_12.EntityType.PLAYER).index(16).handler((event, meta) -> {
            CompoundTag tag = (CompoundTag) event.meta().getValue();
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
