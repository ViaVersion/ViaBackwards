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

package com.viaversion.viabackwards.protocol.v1_11_1to1_11.rewriter;

import com.viaversion.viabackwards.api.rewriters.LegacyEntityRewriter;
import com.viaversion.viabackwards.protocol.v1_11_1to1_11.Protocol1_11_1To1_11;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_11;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_9;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;

public class EntityPacketRewriter1_11_1 extends LegacyEntityRewriter<ClientboundPackets1_9_3, Protocol1_11_1To1_11> {

    public EntityPacketRewriter1_11_1(Protocol1_11_1To1_11 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_9_3.ADD_ENTITY, new PacketHandlers() {
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
                handler(getObjectRewriter(EntityTypes1_11.ObjectType::findById));
            }
        });

        registerTracker(ClientboundPackets1_9_3.ADD_EXPERIENCE_ORB, EntityTypes1_11.EntityType.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_9_3.ADD_GLOBAL_ENTITY, EntityTypes1_11.EntityType.LIGHTNING_BOLT);

        protocol.registerClientbound(ClientboundPackets1_9_3.ADD_MOB, new PacketHandlers() {
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
                map(Types1_9.ENTITY_DATA_LIST); // 12 - Entity data

                // Track entity
                handler(getTrackerHandler());

                // Rewrite entity type / data
                handler(getMobSpawnRewriter1_11(Types1_9.ENTITY_DATA_LIST));
            }
        });

        registerTracker(ClientboundPackets1_9_3.ADD_PAINTING, EntityTypes1_11.EntityType.PAINTING);
        registerJoinGame(ClientboundPackets1_9_3.LOGIN, EntityTypes1_11.EntityType.PLAYER);
        registerRespawn(ClientboundPackets1_9_3.RESPAWN);

        protocol.registerClientbound(ClientboundPackets1_9_3.ADD_PLAYER, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity ID
                map(Types.UUID); // 1 - Player UUID
                map(Types.DOUBLE); // 2 - X
                map(Types.DOUBLE); // 3 - Y
                map(Types.DOUBLE); // 4 - Z
                map(Types.BYTE); // 5 - Yaw
                map(Types.BYTE); // 6 - Pitch
                map(Types1_9.ENTITY_DATA_LIST); // 7 - Entity data list

                handler(getTrackerAndDataHandler(Types1_9.ENTITY_DATA_LIST, EntityTypes1_11.EntityType.PLAYER));
            }
        });

        registerRemoveEntities(ClientboundPackets1_9_3.REMOVE_ENTITIES);
        registerSetEntityData(ClientboundPackets1_9_3.SET_ENTITY_DATA, Types1_9.ENTITY_DATA_LIST);
    }

    @Override
    protected void registerRewrites() {
        // Handle non-existing firework entity data (index 7 entity id for boosting)
        filter().type(EntityTypes1_11.EntityType.FIREWORK_ROCKET).cancel(7);

        // Handle non-existing pig entity data (index 14 - boost time)
        filter().type(EntityTypes1_11.EntityType.PIG).cancel(14);
    }

    @Override
    public EntityType typeFromId(int typeId) {
        return EntityTypes1_11.getTypeFromId(typeId, false);
    }

    @Override
    public EntityType objectTypeFromId(int typeId) {
        return EntityTypes1_11.getTypeFromId(typeId, true);
    }
}
