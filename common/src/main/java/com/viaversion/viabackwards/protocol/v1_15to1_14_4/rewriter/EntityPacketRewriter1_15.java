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
package com.viaversion.viabackwards.protocol.v1_15to1_14_4.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_15to1_14_4.Protocol1_15To1_14_4;
import com.viaversion.viabackwards.protocol.v1_15to1_14_4.data.EntityTypeMapping;
import com.viaversion.viabackwards.protocol.v1_15to1_14_4.data.ImmediateRespawn;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_15;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_14;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ServerboundPackets1_14;
import com.viaversion.viaversion.protocols.v1_14_4to1_15.packet.ClientboundPackets1_15;
import java.util.ArrayList;

public class EntityPacketRewriter1_15 extends EntityRewriter<ClientboundPackets1_15, Protocol1_15To1_14_4> {

    public EntityPacketRewriter1_15(Protocol1_15To1_14_4 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_15.SET_HEALTH, wrapper -> {
            float health = wrapper.passthrough(Types.FLOAT);
            if (health > 0) return;
            if (!wrapper.user().get(ImmediateRespawn.class).isImmediateRespawn()) return;

            // Instantly request respawn when 1.15 gamerule is set
            PacketWrapper statusPacket = wrapper.create(ServerboundPackets1_14.CLIENT_COMMAND);
            statusPacket.write(Types.VAR_INT, 0);
            statusPacket.sendToServer(Protocol1_15To1_14_4.class);
        });

        protocol.registerClientbound(ClientboundPackets1_15.GAME_EVENT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.UNSIGNED_BYTE);
                map(Types.FLOAT);
                handler(wrapper -> {
                    if (wrapper.get(Types.UNSIGNED_BYTE, 0) == 11) {
                        wrapper.user().get(ImmediateRespawn.class).setImmediateRespawn(wrapper.get(Types.FLOAT, 0) == 1);
                    }
                });
            }
        });

        registerTrackerWithData(ClientboundPackets1_15.ADD_ENTITY, EntityTypes1_15.FALLING_BLOCK);

        protocol.registerClientbound(ClientboundPackets1_15.ADD_MOB, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity ID
                map(Types.UUID); // 1 - Entity UUID
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
                handler(wrapper -> wrapper.write(Types1_14.ENTITY_DATA_LIST, new ArrayList<>())); // Metadata is no longer sent in 1.15, so we have to send an empty one

                handler(wrapper -> {
                    int type = wrapper.get(Types.VAR_INT, 1);
                    EntityType entityType = EntityTypes1_15.getTypeFromId(type);
                    tracker(wrapper.user()).addEntity(wrapper.get(Types.VAR_INT, 0), entityType);
                    wrapper.set(Types.VAR_INT, 1, EntityTypeMapping.getOldEntityId(type));
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_15.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT);
                read(Types.LONG); // Seed
            }
        });

        protocol.registerClientbound(ClientboundPackets1_15.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // 0 - Entity ID
                map(Types.UNSIGNED_BYTE); // 1 - Gamemode
                map(Types.INT); // 2 - Dimension

                read(Types.LONG); // Seed

                map(Types.UNSIGNED_BYTE); // 3 - Max Players
                map(Types.STRING); // 4 - Level Type
                map(Types.VAR_INT); // 5 - View Distance
                map(Types.BOOLEAN); // 6 - Reduce Debug Info

                handler(getTrackerHandler(EntityTypes1_15.PLAYER, Types.INT));

                handler(wrapper -> {
                    boolean immediateRespawn = !wrapper.read(Types.BOOLEAN); // Inverted
                    wrapper.user().get(ImmediateRespawn.class).setImmediateRespawn(immediateRespawn);
                });
            }
        });

        registerTracker(ClientboundPackets1_15.ADD_EXPERIENCE_ORB, EntityTypes1_15.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_15.ADD_GLOBAL_ENTITY, EntityTypes1_15.LIGHTNING_BOLT);
        registerTracker(ClientboundPackets1_15.ADD_PAINTING, EntityTypes1_15.PAINTING);

        protocol.registerClientbound(ClientboundPackets1_15.ADD_PLAYER, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity ID
                map(Types.UUID); // 1 - Player UUID
                map(Types.DOUBLE); // 2 - X
                map(Types.DOUBLE); // 3 - Y
                map(Types.DOUBLE); // 4 - Z
                map(Types.BYTE); // 5 - Yaw
                map(Types.BYTE); // 6 - Pitch
                handler(wrapper -> wrapper.write(Types1_14.ENTITY_DATA_LIST, new ArrayList<>())); // Metadata is no longer sent in 1.15, so we have to send an empty one

                handler(getTrackerHandler(EntityTypes1_15.PLAYER, Types.VAR_INT));
            }
        });

        registerRemoveEntities(ClientboundPackets1_15.REMOVE_ENTITIES);
        registerSetEntityData(ClientboundPackets1_15.SET_ENTITY_DATA, Types1_14.ENTITY_DATA_LIST);

        // Attributes (get rid of generic.flyingSpeed for the Bee remap)
        protocol.registerClientbound(ClientboundPackets1_15.UPDATE_ATTRIBUTES, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                map(Types.INT);
                handler(wrapper -> {
                    int entityId = wrapper.get(Types.VAR_INT, 0);
                    EntityType entityType = tracker(wrapper.user()).entityType(entityId);
                    if (entityType != EntityTypes1_15.BEE) return;

                    int size = wrapper.get(Types.INT, 0);
                    int newSize = size;
                    for (int i = 0; i < size; i++) {
                        String key = wrapper.read(Types.STRING);
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
        registerMetaTypeHandler(Types1_14.ENTITY_DATA_TYPES.itemType, null, Types1_14.ENTITY_DATA_TYPES.optionalBlockStateType, Types1_14.ENTITY_DATA_TYPES.particleType,
            Types1_14.ENTITY_DATA_TYPES.componentType, Types1_14.ENTITY_DATA_TYPES.optionalComponentType);

        filter().type(EntityTypes1_15.LIVING_ENTITY).removeIndex(12);

        filter().type(EntityTypes1_15.BEE).cancel(15);
        filter().type(EntityTypes1_15.BEE).cancel(16);

        mapEntityTypeWithData(EntityTypes1_15.BEE, EntityTypes1_15.PUFFERFISH).jsonName().spawnMetadata(storage -> {
            storage.add(new EntityData(14, Types1_14.ENTITY_DATA_TYPES.booleanType, false));
            storage.add(new EntityData(15, Types1_14.ENTITY_DATA_TYPES.varIntType, 2));
        });

        filter().type(EntityTypes1_15.ENDERMAN).cancel(16);
        filter().type(EntityTypes1_15.TRIDENT).cancel(10);

        // Redundant health removed in 1.15
        filter().type(EntityTypes1_15.WOLF).addIndex(17);
        filter().type(EntityTypes1_15.WOLF).index(8).handler((event, meta) -> {
            event.createExtraData(new EntityData(17/*WOLF_HEALTH*/, Types1_14.ENTITY_DATA_TYPES.floatType, event.data().value()));
        });
    }

    @Override
    public EntityType typeFromId(int typeId) {
        return EntityTypes1_15.getTypeFromId(typeId);
    }

    @Override
    public int newEntityId(final int newId) {
        return EntityTypeMapping.getOldEntityId(newId);
    }
}
