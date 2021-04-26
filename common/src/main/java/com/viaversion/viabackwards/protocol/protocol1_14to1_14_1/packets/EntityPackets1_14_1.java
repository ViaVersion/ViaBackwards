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
package com.viaversion.viabackwards.protocol.protocol1_14to1_14_1.packets;

import com.viaversion.viabackwards.api.entities.storage.MetaStorage;
import com.viaversion.viabackwards.api.rewriters.LegacyEntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_14to1_14_1.Protocol1_14To1_14_1;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_14Types;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_14;
import com.viaversion.viaversion.protocols.protocol1_14to1_13_2.ClientboundPackets1_14;

public class EntityPackets1_14_1 extends LegacyEntityRewriter<Protocol1_14To1_14_1> {

    public EntityPackets1_14_1(Protocol1_14To1_14_1 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerExtraTracker(ClientboundPackets1_14.SPAWN_EXPERIENCE_ORB, Entity1_14Types.EXPERIENCE_ORB);
        registerExtraTracker(ClientboundPackets1_14.SPAWN_GLOBAL_ENTITY, Entity1_14Types.LIGHTNING_BOLT);
        registerExtraTracker(ClientboundPackets1_14.SPAWN_PAINTING, Entity1_14Types.PAINTING);
        registerExtraTracker(ClientboundPackets1_14.SPAWN_PLAYER, Entity1_14Types.PLAYER);
        registerExtraTracker(ClientboundPackets1_14.JOIN_GAME, Entity1_14Types.PLAYER, Type.INT);
        registerEntityDestroy(ClientboundPackets1_14.DESTROY_ENTITIES);

        protocol.registerOutgoing(ClientboundPackets1_14.SPAWN_ENTITY, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.UUID); // 1 - UUID
                map(Type.VAR_INT); // 2 - Type

                handler(getTrackerHandler());
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_14.SPAWN_MOB, new PacketRemapper() {
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
                map(Types1_14.METADATA_LIST); // 12 - Metadata

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int entityId = wrapper.get(Type.VAR_INT, 0);
                        int type = wrapper.get(Type.VAR_INT, 1);

                        // Register Type ID
                        addTrackedEntity(wrapper, entityId, Entity1_14Types.getTypeFromId(type));

                        MetaStorage storage = new MetaStorage(wrapper.get(Types1_14.METADATA_LIST, 0));
                        handleMeta(wrapper.user(), entityId, storage);
                    }
                });
            }
        });

        // Entity Metadata
        registerMetadataRewriter(ClientboundPackets1_14.ENTITY_METADATA, Types1_14.METADATA_LIST);
    }

    @Override
    protected void registerRewrites() {
        registerMetaHandler().filter(Entity1_14Types.VILLAGER, 15).removed();
        registerMetaHandler().filter(Entity1_14Types.VILLAGER, 16).handleIndexChange(15);
        registerMetaHandler().filter(Entity1_14Types.WANDERING_TRADER, 15).removed();
    }

    @Override
    protected EntityType getTypeFromId(int typeId) {
        return Entity1_14Types.getTypeFromId(typeId);
    }
}
