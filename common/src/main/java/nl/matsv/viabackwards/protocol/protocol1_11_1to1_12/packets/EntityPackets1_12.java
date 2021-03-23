/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
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

package nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.packets;

import nl.matsv.viabackwards.api.exceptions.RemovedValueException;
import nl.matsv.viabackwards.api.rewriters.LegacyEntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.Protocol1_11_1To1_12;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.data.ParrotStorage;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.data.ShoulderTracker;
import nl.matsv.viabackwards.utils.Block;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.entities.Entity1_12Types;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_12;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.version.Types1_12;
import us.myles.ViaVersion.protocols.protocol1_12to1_11_1.ClientboundPackets1_12;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;

import java.util.Optional;

public class EntityPackets1_12 extends LegacyEntityRewriter<Protocol1_11_1To1_12> {

    public EntityPackets1_12(Protocol1_11_1To1_12 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerOutgoing(ClientboundPackets1_12.SPAWN_ENTITY, new PacketRemapper() {
            @Override
            public void registerMap() {
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
                handler(getObjectRewriter(id -> Entity1_12Types.ObjectType.findById(id).orElse(null)));

                // Handle FallingBlock blocks
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Optional<Entity1_12Types.ObjectType> type = Entity1_12Types.ObjectType.findById(wrapper.get(Type.BYTE, 0));
                        if (type.isPresent() && type.get() == Entity1_12Types.ObjectType.FALLING_BLOCK) {
                            int objectData = wrapper.get(Type.INT, 0);
                            int objType = objectData & 4095;
                            int data = objectData >> 12 & 15;

                            Block block = getProtocol().getBlockItemPackets().handleBlock(objType, data);
                            if (block == null) {
                                return;
                            }

                            wrapper.set(Type.INT, 0, block.getId() | block.getData() << 12);
                        }
                    }
                });
            }
        });

        registerExtraTracker(ClientboundPackets1_12.SPAWN_EXPERIENCE_ORB, Entity1_12Types.EntityType.EXPERIENCE_ORB);
        registerExtraTracker(ClientboundPackets1_12.SPAWN_GLOBAL_ENTITY, Entity1_12Types.EntityType.WEATHER);

        protocol.registerOutgoing(ClientboundPackets1_12.SPAWN_MOB, new PacketRemapper() {
            @Override
            public void registerMap() {
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

        registerExtraTracker(ClientboundPackets1_12.SPAWN_PAINTING, Entity1_12Types.EntityType.PAINTING);

        protocol.registerOutgoing(ClientboundPackets1_12.SPAWN_PLAYER, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.UUID); // 1 - Player UUID
                map(Type.DOUBLE); // 2 - X
                map(Type.DOUBLE); // 3 - Y
                map(Type.DOUBLE); // 4 - Z
                map(Type.BYTE); // 5 - Yaw
                map(Type.BYTE); // 6 - Pitch
                map(Types1_12.METADATA_LIST); // 7 - Metadata list

                handler(getTrackerAndMetaHandler(Types1_12.METADATA_LIST, Entity1_12Types.EntityType.PLAYER));
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_12.JOIN_GAME, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Entity ID
                map(Type.UNSIGNED_BYTE); // 1 - Gamemode
                map(Type.INT); // 2 - Dimension

                handler(getTrackerHandler(Entity1_12Types.EntityType.PLAYER, Type.INT));

                handler(getDimensionHandler(1));

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ShoulderTracker tracker = wrapper.user().get(ShoulderTracker.class);
                        tracker.setEntityId(wrapper.get(Type.INT, 0));
                    }
                });

                // Send fake inventory achievement
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper packetWrapper) throws Exception {
                        PacketWrapper wrapper = new PacketWrapper(0x07, null, packetWrapper.user());

                        wrapper.write(Type.VAR_INT, 1);
                        wrapper.write(Type.STRING, "achievement.openInventory");
                        wrapper.write(Type.VAR_INT, 1);

                        wrapper.send(Protocol1_11_1To1_12.class);
                    }
                });
            }
        });

        registerRespawn(ClientboundPackets1_12.RESPAWN);
        registerEntityDestroy(ClientboundPackets1_12.DESTROY_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_12.ENTITY_METADATA, Types1_12.METADATA_LIST);

        protocol.registerOutgoing(ClientboundPackets1_12.ENTITY_PROPERTIES, new PacketRemapper() {
            @Override
            public void registerMap() {
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
        mapEntity(Entity1_12Types.EntityType.PARROT, Entity1_12Types.EntityType.BAT).mobName("Parrot").spawnMetadata(storage -> storage.add(new Metadata(12, MetaType1_12.Byte, (byte) 0x00)));
        mapEntity(Entity1_12Types.EntityType.ILLUSION_ILLAGER, Entity1_12Types.EntityType.EVOCATION_ILLAGER).mobName("Illusioner");

        // Handle Illager
        registerMetaHandler().filter(Entity1_12Types.EntityType.EVOCATION_ILLAGER, true, 12).removed();
        registerMetaHandler().filter(Entity1_12Types.EntityType.EVOCATION_ILLAGER, true, 13).handleIndexChange(12);

        registerMetaHandler().filter(Entity1_12Types.EntityType.ILLUSION_ILLAGER, 0).handle(e -> {
            byte mask = (byte) e.getData().getValue();

            if ((mask & 0x20) == 0x20)
                mask &= ~0x20;

            e.getData().setValue(mask);
            return e.getData();
        });

        // Create Parrot storage
        registerMetaHandler().filter(Entity1_12Types.EntityType.PARROT, true).handle(e -> {
            if (!e.getEntity().has(ParrotStorage.class))
                e.getEntity().put(new ParrotStorage());
            return e.getData();
        });
        // Parrot remove animal metadata
        registerMetaHandler().filter(Entity1_12Types.EntityType.PARROT, 12).removed(); // Is baby
        registerMetaHandler().filter(Entity1_12Types.EntityType.PARROT, 13).handle(e -> {
            Metadata data = e.getData();
            ParrotStorage storage = e.getEntity().get(ParrotStorage.class);
            boolean isSitting = (((byte) data.getValue()) & 0x01) == 0x01;
            boolean isTamed = (((byte) data.getValue()) & 0x04) == 0x04;

            if (!storage.isTamed() && isTamed) {
                // TODO do something to let the user know it's done
            }

            storage.setTamed(isTamed);

            if (isSitting) {
                data.setId(12);
                data.setValue((byte) 0x01);
                storage.setSitting(true);
            } else if (storage.isSitting()) {
                data.setId(12);
                data.setValue((byte) 0x00);
                storage.setSitting(false);
            } else
                throw RemovedValueException.EX;

            return data;
        }); // Flags (Is sitting etc, might be useful in the future
        registerMetaHandler().filter(Entity1_12Types.EntityType.PARROT, 14).removed(); // Owner
        registerMetaHandler().filter(Entity1_12Types.EntityType.PARROT, 15).removed(); // Variant

        // Left shoulder entity data
        registerMetaHandler().filter(Entity1_12Types.EntityType.PLAYER, 15).handle(e -> {
            CompoundTag tag = (CompoundTag) e.getData().getValue();
            ShoulderTracker tracker = e.getUser().get(ShoulderTracker.class);

            if (tag.isEmpty() && tracker.getLeftShoulder() != null) {
                tracker.setLeftShoulder(null);
                tracker.update();
            } else if (tag.contains("id") && e.getEntity().getEntityId() == tracker.getEntityId()) {
                String id = (String) tag.get("id").getValue();
                if (tracker.getLeftShoulder() == null || !tracker.getLeftShoulder().equals(id)) {
                    tracker.setLeftShoulder(id);
                    tracker.update();
                }
            }

            throw RemovedValueException.EX;
        });

        // Right shoulder entity data
        registerMetaHandler().filter(Entity1_12Types.EntityType.PLAYER, 16).handle(e -> {
            CompoundTag tag = (CompoundTag) e.getData().getValue();
            ShoulderTracker tracker = e.getUser().get(ShoulderTracker.class);

            if (tag.isEmpty() && tracker.getRightShoulder() != null) {
                tracker.setRightShoulder(null);
                tracker.update();
            } else if (tag.contains("id") && e.getEntity().getEntityId() == tracker.getEntityId()) {
                String id = (String) tag.get("id").getValue();
                if (tracker.getRightShoulder() == null || !tracker.getRightShoulder().equals(id)) {
                    tracker.setRightShoulder(id);
                    tracker.update();
                }
            }

            throw RemovedValueException.EX;
        });
    }

    @Override
    protected EntityType getTypeFromId(int typeId) {
        return Entity1_12Types.getTypeFromId(typeId, false);
    }

    @Override
    protected EntityType getObjectTypeFromId(final int typeId) {
        return Entity1_12Types.getTypeFromId(typeId, true);
    }
}
