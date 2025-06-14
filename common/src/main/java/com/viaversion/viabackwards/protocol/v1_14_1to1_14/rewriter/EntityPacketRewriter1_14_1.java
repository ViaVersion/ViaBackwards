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
package com.viaversion.viabackwards.protocol.v1_14_1to1_14.rewriter;

import com.viaversion.viabackwards.api.rewriters.LegacyEntityRewriter;
import com.viaversion.viabackwards.protocol.v1_14_1to1_14.Protocol1_14_1To1_14;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_14;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_14;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ClientboundPackets1_14;
import java.util.List;

public class EntityPacketRewriter1_14_1 extends LegacyEntityRewriter<ClientboundPackets1_14, Protocol1_14_1To1_14> {

    public EntityPacketRewriter1_14_1(Protocol1_14_1To1_14 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerTracker(ClientboundPackets1_14.ADD_EXPERIENCE_ORB, EntityTypes1_14.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_14.ADD_GLOBAL_ENTITY, EntityTypes1_14.LIGHTNING_BOLT);
        registerTracker(ClientboundPackets1_14.ADD_PAINTING, EntityTypes1_14.PAINTING);
        registerTracker(ClientboundPackets1_14.ADD_PLAYER, EntityTypes1_14.PLAYER);
        registerJoinGame(ClientboundPackets1_14.LOGIN, EntityTypes1_14.PLAYER);
        registerRespawn(ClientboundPackets1_14.RESPAWN);
        registerRemoveEntities(ClientboundPackets1_14.REMOVE_ENTITIES);

        protocol.registerClientbound(ClientboundPackets1_14.ADD_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity id
                map(Types.UUID); // 1 - UUID
                map(Types.VAR_INT); // 2 - Type

                handler(getTrackerHandler());
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.ADD_MOB, new PacketHandlers() {
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
                map(Types1_14.ENTITY_DATA_LIST); // 12 - Entity data

                handler(wrapper -> {
                    int entityId = wrapper.get(Types.VAR_INT, 0);
                    int type = wrapper.get(Types.VAR_INT, 1);

                    // Register Type ID
                    tracker(wrapper.user()).addEntity(entityId, EntityTypes1_14.getTypeFromId(type));

                    List<EntityData> entityDataList = wrapper.get(Types1_14.ENTITY_DATA_LIST, 0);
                    handleEntityData(entityId, entityDataList, wrapper.user());
                });
            }
        });

        // Entity data
        registerSetEntityData(ClientboundPackets1_14.SET_ENTITY_DATA, Types1_14.ENTITY_DATA_LIST);
    }

    @Override
    protected void registerRewrites() {
        filter().type(EntityTypes1_14.VILLAGER).cancel(15);
        filter().type(EntityTypes1_14.VILLAGER).index(16).toIndex(15);
        filter().type(EntityTypes1_14.WANDERING_TRADER).cancel(15);
    }

    @Override
    public EntityType typeFromId(int typeId) {
        return EntityTypes1_14.getTypeFromId(typeId);
    }
}
