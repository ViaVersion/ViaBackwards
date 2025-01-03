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
package com.viaversion.viabackwards.protocol.v1_13_1to1_13.rewriter;

import com.viaversion.viabackwards.api.rewriters.LegacyEntityRewriter;
import com.viaversion.viabackwards.protocol.v1_13_1to1_13.Protocol1_13_1To1_13;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_13;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ClientboundPackets1_13;
import java.util.List;

public class EntityPacketRewriter1_13_1 extends LegacyEntityRewriter<ClientboundPackets1_13, Protocol1_13_1To1_13> {

    public EntityPacketRewriter1_13_1(Protocol1_13_1To1_13 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_13.ADD_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity id
                map(Types.UUID); // 1 - UUID
                map(Types.BYTE); // 2 - Type
                map(Types.DOUBLE); // 3 - X
                map(Types.DOUBLE); // 4 - Y
                map(Types.DOUBLE); // 5 - Z
                map(Types.BYTE); // 6 - Pitch
                map(Types.BYTE); // 7 - Yaw
                map(Types.INT); // 8 - Data

                handler(wrapper -> {
                    int entityId = wrapper.get(Types.VAR_INT, 0);
                    byte type = wrapper.get(Types.BYTE, 0);
                    int data = wrapper.get(Types.INT, 0);
                    EntityTypes1_13.EntityType entType = EntityTypes1_13.ObjectType.getEntityType(type, data);
                    if (entType == null) {
                        return;
                    }

                    // Rewrite falling block
                    if (entType.is(EntityTypes1_13.EntityType.FALLING_BLOCK)) {
                        wrapper.set(Types.INT, 0, protocol.getMappingData().getNewBlockStateId(data));
                    }

                    // Track Entity
                    tracker(wrapper.user()).addEntity(entityId, entType);
                });
            }
        });

        registerTracker(ClientboundPackets1_13.ADD_EXPERIENCE_ORB, EntityTypes1_13.EntityType.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_13.ADD_GLOBAL_ENTITY, EntityTypes1_13.EntityType.LIGHTNING_BOLT);

        protocol.registerClientbound(ClientboundPackets1_13.ADD_MOB, new PacketHandlers() {
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
                map(Types1_13.ENTITY_DATA_LIST); // 12 - Entity data

                // Track Entity
                handler(getTrackerHandler());

                // Rewrite Entity data
                handler(wrapper -> {
                    List<EntityData> entityDataList = wrapper.get(Types1_13.ENTITY_DATA_LIST, 0);
                    handleEntityData(wrapper.get(Types.VAR_INT, 0), entityDataList, wrapper.user());
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.ADD_PLAYER, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity ID
                map(Types.UUID); // 1 - Player UUID
                map(Types.DOUBLE); // 2 - X
                map(Types.DOUBLE); // 3 - Y
                map(Types.DOUBLE); // 4 - Z
                map(Types.BYTE); // 5 - Yaw
                map(Types.BYTE); // 6 - Pitch
                map(Types1_13.ENTITY_DATA_LIST); // 7 - Entity data

                handler(getTrackerAndDataHandler(Types1_13.ENTITY_DATA_LIST, EntityTypes1_13.EntityType.PLAYER));
            }
        });

        registerTracker(ClientboundPackets1_13.ADD_PAINTING, EntityTypes1_13.EntityType.PAINTING);
        registerJoinGame(ClientboundPackets1_13.LOGIN, EntityTypes1_13.EntityType.PLAYER);
        registerRespawn(ClientboundPackets1_13.RESPAWN);
        registerRemoveEntities(ClientboundPackets1_13.REMOVE_ENTITIES);
        registerSetEntityData(ClientboundPackets1_13.SET_ENTITY_DATA, Types1_13.ENTITY_DATA_LIST);
    }

    @Override
    protected void registerRewrites() {
        // Rewrite items & blocks
        filter().handler((event, data) -> {
            if (data.dataType() == Types1_13.ENTITY_DATA_TYPES.itemType) {
                protocol.getItemRewriter().handleItemToClient(event.user(), (Item) data.getValue());
            } else if (data.dataType() == Types1_13.ENTITY_DATA_TYPES.optionalBlockStateType) {
                // Convert to new block id
                int value = (int) data.getValue();
                data.setValue(protocol.getMappingData().getNewBlockStateId(value));
            } else if (data.dataType() == Types1_13.ENTITY_DATA_TYPES.particleType) {
                protocol.getParticleRewriter().rewriteParticle(event.user(), (Particle) data.getValue());
            } else if (data.dataType() == Types1_13.ENTITY_DATA_TYPES.optionalComponentType || data.dataType() == Types1_13.ENTITY_DATA_TYPES.componentType) {
                JsonElement element = data.value();
                protocol.translatableRewriter().processText(event.user(), element);
            }
        });

        // Remove shooter UUID
        filter().type(EntityTypes1_13.EntityType.ABSTRACT_ARROW).cancel(7);

        // Move colors to old position
        filter().type(EntityTypes1_13.EntityType.SPECTRAL_ARROW).index(8).toIndex(7);

        // Move loyalty level to old position
        filter().type(EntityTypes1_13.EntityType.TRIDENT).index(8).toIndex(7);

        // Rewrite Minecart blocks
        registerBlockStateHandler(EntityTypes1_13.EntityType.ABSTRACT_MINECART, 9);
    }

    @Override
    public EntityType typeFromId(int typeId) {
        return EntityTypes1_13.EntityType.findById(typeId);
    }

    @Override
    public EntityType objectTypeFromId(int typeId, int data) {
        return EntityTypes1_13.ObjectType.getEntityType(typeId, data);
    }
}
