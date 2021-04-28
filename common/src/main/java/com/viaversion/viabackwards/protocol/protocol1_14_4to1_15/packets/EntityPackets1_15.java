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
package com.viaversion.viabackwards.protocol.protocol1_14_4to1_15.packets;

import com.viaversion.viabackwards.api.exceptions.RemovedValueException;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_14_4to1_15.Protocol1_14_4To1_15;
import com.viaversion.viabackwards.protocol.protocol1_14_4to1_15.data.EntityTypeMapping;
import com.viaversion.viabackwards.protocol.protocol1_14_4to1_15.data.ImmediateRespawn;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_15Types;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.metadata.MetaType;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_14;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.Particle;
import com.viaversion.viaversion.api.type.types.version.Types1_14;
import com.viaversion.viaversion.protocols.protocol1_15to1_14_4.ClientboundPackets1_15;

import java.util.ArrayList;

public class EntityPackets1_15 extends EntityRewriter<Protocol1_14_4To1_15> {

    public EntityPackets1_15(Protocol1_14_4To1_15 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_15.UPDATE_HEALTH, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    float health = wrapper.passthrough(Type.FLOAT);
                    if (health > 0) return;
                    if (!wrapper.user().get(ImmediateRespawn.class).isImmediateRespawn()) return;

                    // Instantly request respawn when 1.15 gamerule is set
                    PacketWrapper statusPacket = wrapper.create(0x04);
                    statusPacket.write(Type.VAR_INT, 0);
                    statusPacket.sendToServer(Protocol1_14_4To1_15.class);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_15.GAME_EVENT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE);
                map(Type.FLOAT);
                handler(wrapper -> {
                    if (wrapper.get(Type.UNSIGNED_BYTE, 0) == 11) {
                        wrapper.user().get(ImmediateRespawn.class).setImmediateRespawn(wrapper.get(Type.FLOAT, 0) == 1);
                    }
                });
            }
        });

        registerSpawnTrackerWithData(ClientboundPackets1_15.SPAWN_ENTITY, Entity1_15Types.FALLING_BLOCK);

        protocol.registerClientbound(ClientboundPackets1_15.SPAWN_MOB, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.UUID); // 1 - Entity UUID
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
                create(wrapper -> wrapper.write(Types1_14.METADATA_LIST, new ArrayList<>())); // Metadata is no longer sent in 1.15, so we have to send an empty one

                handler(wrapper -> {
                    int type = wrapper.get(Type.VAR_INT, 1);
                    EntityType entityType = Entity1_15Types.getTypeFromId(type);
                    addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), entityType);
                    wrapper.set(Type.VAR_INT, 1, EntityTypeMapping.getOldEntityId(type));
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_15.RESPAWN, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT);
                map(Type.LONG, Type.NOTHING); // Seed
            }
        });

        protocol.registerClientbound(ClientboundPackets1_15.JOIN_GAME, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Entity ID
                map(Type.UNSIGNED_BYTE); // 1 - Gamemode
                map(Type.INT); // 2 - Dimension

                map(Type.LONG, Type.NOTHING); // Seed

                map(Type.UNSIGNED_BYTE); // 3 - Max Players
                map(Type.STRING); // 4 - Level Type
                map(Type.VAR_INT); // 5 - View Distance
                map(Type.BOOLEAN); // 6 - Reduce Debug Info

                handler(getTrackerHandler(Entity1_15Types.PLAYER, Type.INT));

                handler(wrapper -> wrapper.user().get(ImmediateRespawn.class).setImmediateRespawn(wrapper.read(Type.BOOLEAN)));
            }
        });

        registerExtraTracker(ClientboundPackets1_15.SPAWN_EXPERIENCE_ORB, Entity1_15Types.EXPERIENCE_ORB);
        registerExtraTracker(ClientboundPackets1_15.SPAWN_GLOBAL_ENTITY, Entity1_15Types.LIGHTNING_BOLT);
        registerExtraTracker(ClientboundPackets1_15.SPAWN_PAINTING, Entity1_15Types.PAINTING);

        protocol.registerClientbound(ClientboundPackets1_15.SPAWN_PLAYER, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.UUID); // 1 - Player UUID
                map(Type.DOUBLE); // 2 - X
                map(Type.DOUBLE); // 3 - Y
                map(Type.DOUBLE); // 4 - Z
                map(Type.BYTE); // 5 - Yaw
                map(Type.BYTE); // 6 - Pitch
                create(wrapper -> wrapper.write(Types1_14.METADATA_LIST, new ArrayList<>())); // Metadata is no longer sent in 1.15, so we have to send an empty one

                handler(getTrackerHandler(Entity1_15Types.PLAYER, Type.VAR_INT));
            }
        });

        registerEntityDestroy(ClientboundPackets1_15.DESTROY_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_15.ENTITY_METADATA, Types1_14.METADATA_LIST);

        // Attributes (get rid of generic.flyingSpeed for the Bee remap)
        protocol.registerClientbound(ClientboundPackets1_15.ENTITY_PROPERTIES, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.INT);
                handler(wrapper -> {
                    int entityId = wrapper.get(Type.VAR_INT, 0);
                    EntityType entityType = getEntityType(wrapper.user(), entityId);
                    if (entityType != Entity1_15Types.BEE) return;

                    int size = wrapper.get(Type.INT, 0);
                    int newSize = size;
                    for (int i = 0; i < size; i++) {
                        String key = wrapper.read(Type.STRING);
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
        registerMetaHandler().handle(e -> {
            Metadata meta = e.getData();
            MetaType type = meta.getMetaType();
            if (type == MetaType1_14.Slot) {
                Item item = (Item) meta.getValue();
                meta.setValue(protocol.getBlockItemPackets().handleItemToClient(item));
            } else if (type == MetaType1_14.BlockID) {
                int blockstate = (int) meta.getValue();
                meta.setValue(protocol.getMappingData().getNewBlockStateId(blockstate));
            } else if (type == MetaType1_14.PARTICLE) {
                rewriteParticle((Particle) meta.getValue());
            }
            return meta;
        });

        registerMetaHandler().filter(Entity1_15Types.LIVINGENTITY, true).handle(e -> {
            int index = e.getIndex();
            if (index == 12) {
                throw RemovedValueException.EX;
            } else if (index > 12) {
                e.getData().setId(index - 1);
            }
            return e.getData();
        });

        registerMetaHandler().filter(Entity1_15Types.BEE, 15).removed();
        registerMetaHandler().filter(Entity1_15Types.BEE, 16).removed();

        mapEntity(Entity1_15Types.BEE, Entity1_15Types.PUFFERFISH).jsonName("Bee").spawnMetadata(storage -> {
            storage.add(new Metadata(14, MetaType1_14.Boolean, false));
            storage.add(new Metadata(15, MetaType1_14.VarInt, 2));
        });

        registerMetaHandler().filter(Entity1_15Types.ENDERMAN, 16).removed();
        registerMetaHandler().filter(Entity1_15Types.TRIDENT, 10).removed();

        registerMetaHandler().filter(Entity1_15Types.WOLF).handle(e -> {
            int index = e.getIndex();
            if (index >= 17) {
                e.getData().setId(index + 1); // redundant health removed in 1.15
            }
            return e.getData();
        });
        registerMetaHandler().filter(Entity1_15Types.WOLF, 8).handle(e -> {
            e.createMeta(new Metadata(17/*WOLF_HEALTH*/, MetaType1_14.Float, e.getData().getValue()));
            return e.getData();
        });
    }

    @Override
    protected EntityType getTypeFromId(int typeId) {
        return Entity1_15Types.getTypeFromId(typeId);
    }

    @Override
    public int getOldEntityId(final int newId) {
        return EntityTypeMapping.getOldEntityId(newId);
    }
}
