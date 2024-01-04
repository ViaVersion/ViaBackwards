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
package com.viaversion.viabackwards.protocol.protocol1_14to1_14_1.packets;

import com.viaversion.viabackwards.api.rewriters.LegacyEntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_14to1_14_1.Protocol1_14To1_14_1;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_14;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_14;
import com.viaversion.viaversion.protocols.protocol1_14to1_13_2.ClientboundPackets1_14;
import java.util.List;

public class EntityPackets1_14_1 extends LegacyEntityRewriter<ClientboundPackets1_14, Protocol1_14To1_14_1> {

    public EntityPackets1_14_1(Protocol1_14To1_14_1 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerTracker(ClientboundPackets1_14.SPAWN_EXPERIENCE_ORB, EntityTypes1_14.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_14.SPAWN_GLOBAL_ENTITY, EntityTypes1_14.LIGHTNING_BOLT);
        registerTracker(ClientboundPackets1_14.SPAWN_PAINTING, EntityTypes1_14.PAINTING);
        registerTracker(ClientboundPackets1_14.SPAWN_PLAYER, EntityTypes1_14.PLAYER);
        registerTracker(ClientboundPackets1_14.JOIN_GAME, EntityTypes1_14.PLAYER, Type.INT);
        registerRemoveEntities(ClientboundPackets1_14.DESTROY_ENTITIES);

        protocol.registerClientbound(ClientboundPackets1_14.SPAWN_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.UUID); // 1 - UUID
                map(Type.VAR_INT); // 2 - Type

                handler(getTrackerHandler());
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.SPAWN_MOB, new PacketHandlers() {
            @Override
            public void register() {
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
                map(Types1_14.METADATA_LIST); // 12 - Metadata

                handler(wrapper -> {
                    int entityId = wrapper.get(Type.VAR_INT, 0);
                    int type = wrapper.get(Type.VAR_INT, 1);

                    // Register Type ID
                    tracker(wrapper.user()).addEntity(entityId, EntityTypes1_14.getTypeFromId(type));

                    List<Metadata> metadata = wrapper.get(Types1_14.METADATA_LIST, 0);
                    handleMetadata(entityId, metadata, wrapper.user());
                });
            }
        });

        // Entity Metadata
        registerMetadataRewriter(ClientboundPackets1_14.ENTITY_METADATA, Types1_14.METADATA_LIST);
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
