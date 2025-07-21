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

package com.viaversion.viabackwards.protocol.v1_10to1_9_3.rewriter;

import com.viaversion.viabackwards.api.entities.storage.EntityReplacement;
import com.viaversion.viabackwards.api.entities.storage.WrappedEntityData;
import com.viaversion.viabackwards.api.rewriters.LegacyEntityRewriter;
import com.viaversion.viabackwards.protocol.v1_10to1_9_3.Protocol1_10To1_9_3;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_10;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_11;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_9;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;
import java.util.List;

public class EntityPacketRewriter1_10 extends LegacyEntityRewriter<ClientboundPackets1_9_3, Protocol1_10To1_9_3> {

    public EntityPacketRewriter1_10(Protocol1_10To1_9_3 protocol) {
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

                handler(protocol.getItemRewriter().getFallingBlockHandler());
            }
        });

        registerTracker(ClientboundPackets1_9_3.ADD_EXPERIENCE_ORB, EntityTypes1_10.EntityType.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_9_3.ADD_GLOBAL_ENTITY, EntityTypes1_10.EntityType.LIGHTNING_BOLT);

        protocol.registerClientbound(ClientboundPackets1_9_3.ADD_MOB, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity id
                map(Types.UUID); // 1 - UUID
                map(Types.UNSIGNED_BYTE); // 2 - Entity Type
                map(Types.DOUBLE); // 3 - X
                map(Types.DOUBLE); // 4 - Y
                map(Types.DOUBLE); // 5 - Z
                map(Types.BYTE); // 6 - Yaw
                map(Types.BYTE); // 7 - Pitch
                map(Types.BYTE); // 8 - Head Pitch
                map(Types.SHORT); // 9 - Velocity X
                map(Types.SHORT); // 10 - Velocity Y
                map(Types.SHORT); // 11 - Velocity Z
                map(Types.ENTITY_DATA_LIST1_9); // 12 - Entity data

                // Track entity
                handler(getTrackerHandler(Types.UNSIGNED_BYTE, 0));

                // Rewrite entity type / data
                handler(wrapper -> {
                    int entityId = wrapper.get(Types.VAR_INT, 0);
                    EntityType type = tracker(wrapper.user()).entityType(entityId);
                    if (type == null) {
                        return;
                    }

                    List<EntityData> entityDataList = wrapper.get(Types.ENTITY_DATA_LIST1_9, 0);
                    handleEntityData(wrapper.get(Types.VAR_INT, 0), entityDataList, wrapper.user());

                    EntityReplacement entityReplacement = entityDataForType(type);
                    if (entityReplacement != null) {
                        WrappedEntityData storage = new WrappedEntityData(entityDataList);
                        wrapper.set(Types.UNSIGNED_BYTE, 0, (short) entityReplacement.replacementId());
                        if (entityReplacement.hasBaseData())
                            entityReplacement.defaultData().createData(storage);
                    }
                });

            }
        });

        registerTracker(ClientboundPackets1_9_3.ADD_PAINTING, EntityTypes1_10.EntityType.PAINTING);
        registerJoinGame(ClientboundPackets1_9_3.LOGIN, EntityTypes1_10.EntityType.PLAYER);
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
                map(Types.ENTITY_DATA_LIST1_9); // 7 - Entity data list

                handler(getTrackerAndDataHandler(Types.ENTITY_DATA_LIST1_9, EntityTypes1_11.EntityType.PLAYER));
            }
        });

        registerRemoveEntities(ClientboundPackets1_9_3.REMOVE_ENTITIES);
        registerSetEntityData(ClientboundPackets1_9_3.SET_ENTITY_DATA, Types.ENTITY_DATA_LIST1_9);
    }

    @Override
    protected void registerRewrites() {
        mapEntityTypeWithData(EntityTypes1_10.EntityType.POLAR_BEAR, EntityTypes1_10.EntityType.SHEEP).plainName();

        // Change the sheep color when the polar bear is standing up (index 13 -> Standing up)
        filter().type(EntityTypes1_10.EntityType.POLAR_BEAR).index(13).handler((event, data) -> {
            boolean b = (boolean) data.getValue();

            data.setTypeAndValue(EntityDataTypes1_9.BYTE, b ? (byte) (14 & 0x0F) : (byte) (0));
        });


        // Handle husk (index 13 -> Zombie Type)
        filter().type(EntityTypes1_10.EntityType.ZOMBIE).index(13).handler((event, data) -> {
            if ((int) data.getValue() == 6) { // Is type Husk
                data.setValue(0);
            }
        });

        // Handle Stray (index 12 -> Skeleton Type)
        filter().type(EntityTypes1_10.EntityType.SKELETON).index(12).handler((event, data) -> {
            if ((int) data.getValue() == 2) {
                data.setValue(0); // Change to default skeleton
            }
        });

        // Added index from incorrect entity data hierarchy, where potions add to item entities
        filter().type(EntityTypes1_10.EntityType.POTION).addIndex(6);

        // Handle the missing NoGravity tag for every entity data
        filter().removeIndex(5);
    }

    @Override
    public EntityType typeFromId(int typeId) {
        return EntityTypes1_10.EntityType.findById(typeId);
    }

    @Override
    public EntityType objectTypeFromId(int typeId, int data) {
        return EntityTypes1_10.ObjectType.getEntityType(typeId, data);
    }
}
