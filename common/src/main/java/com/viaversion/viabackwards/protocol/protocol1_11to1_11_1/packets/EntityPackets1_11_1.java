/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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

package com.viaversion.viabackwards.protocol.protocol1_11to1_11_1.packets;

import com.viaversion.viabackwards.api.rewriters.LegacyEntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_11to1_11_1.Protocol1_11To1_11_1;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_11;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_9;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;

public class EntityPackets1_11_1 extends LegacyEntityRewriter<ClientboundPackets1_9_3, Protocol1_11To1_11_1> {

    public EntityPackets1_11_1(Protocol1_11To1_11_1 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_9_3.SPAWN_ENTITY, new PacketHandlers() {
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
                handler(getObjectRewriter(id -> EntityTypes1_11.ObjectType.findById(id).orElse(null)));
            }
        });

        registerTracker(ClientboundPackets1_9_3.SPAWN_EXPERIENCE_ORB, EntityTypes1_11.EntityType.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_9_3.SPAWN_GLOBAL_ENTITY, EntityTypes1_11.EntityType.WEATHER);

        protocol.registerClientbound(ClientboundPackets1_9_3.SPAWN_MOB, new PacketHandlers() {
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
                map(Types1_9.METADATA_LIST); // 12 - Metadata

                // Track entity
                handler(getTrackerHandler());

                // Rewrite entity type / metadata
                handler(getMobSpawnRewriter(Types1_9.METADATA_LIST));
            }
        });

        registerTracker(ClientboundPackets1_9_3.SPAWN_PAINTING, EntityTypes1_11.EntityType.PAINTING);
        registerJoinGame(ClientboundPackets1_9_3.JOIN_GAME, EntityTypes1_11.EntityType.PLAYER);
        registerRespawn(ClientboundPackets1_9_3.RESPAWN);

        protocol.registerClientbound(ClientboundPackets1_9_3.SPAWN_PLAYER, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.UUID); // 1 - Player UUID
                map(Type.DOUBLE); // 2 - X
                map(Type.DOUBLE); // 3 - Y
                map(Type.DOUBLE); // 4 - Z
                map(Type.BYTE); // 5 - Yaw
                map(Type.BYTE); // 6 - Pitch
                map(Types1_9.METADATA_LIST); // 7 - Metadata list

                handler(getTrackerAndMetaHandler(Types1_9.METADATA_LIST, EntityTypes1_11.EntityType.PLAYER));
            }
        });

        registerRemoveEntities(ClientboundPackets1_9_3.DESTROY_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_9_3.ENTITY_METADATA, Types1_9.METADATA_LIST);
    }

    @Override
    protected void registerRewrites() {
        // Handle non-existing firework metadata (index 7 entity id for boosting)
        filter().type(EntityTypes1_11.EntityType.FIREWORK).cancel(7);

        // Handle non-existing pig metadata (index 14 - boost time)
        filter().type(EntityTypes1_11.EntityType.PIG).cancel(14);
    }

    @Override
    public EntityType typeFromId(int typeId) {
        return EntityTypes1_11.getTypeFromId(typeId, false);
    }

    @Override
    protected EntityType getObjectTypeFromId(final int typeId) {
        return EntityTypes1_11.getTypeFromId(typeId, true);
    }
}
