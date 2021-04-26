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
package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.entities.storage.EntityData;
import nl.matsv.viabackwards.api.entities.storage.MetaStorage;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.metadata.MetaType;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_14;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;

import java.util.List;

public abstract class EntityRewriter<T extends BackwardsProtocol> extends EntityRewriterBase<T> {

    protected EntityRewriter(T protocol) {
        this(protocol, MetaType1_14.OptChat, MetaType1_14.Boolean);
    }

    protected EntityRewriter(T protocol, MetaType displayType, MetaType displayVisibilityType) {
        super(protocol, displayType, 2, displayVisibilityType, 3);
    }

    public void registerSpawnTrackerWithData(ClientboundPacketType packetType, EntityType fallingBlockType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.UUID); // 1 - Entity UUID
                map(Type.VAR_INT); // 2 - Entity Type
                map(Type.DOUBLE); // 3 - X
                map(Type.DOUBLE); // 4 - Y
                map(Type.DOUBLE); // 5 - Z
                map(Type.BYTE); // 6 - Pitch
                map(Type.BYTE); // 7 - Yaw
                map(Type.INT); // 8 - Data
                handler(getSpawnTracketWithDataHandler(fallingBlockType));
            }
        });
    }

    public PacketHandler getSpawnTracketWithDataHandler(EntityType fallingBlockType) {
        return wrapper -> {
            EntityType entityType = setOldEntityId(wrapper);
            if (entityType == fallingBlockType) {
                int blockState = wrapper.get(Type.INT, 0);
                wrapper.set(Type.INT, 0, protocol.getMappingData().getNewBlockStateId(blockState));
            }
        };
    }

    public void registerSpawnTracker(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.UUID); // 1 - Entity UUID
                map(Type.VAR_INT); // 2 - Entity Type
                handler(wrapper -> setOldEntityId(wrapper));
            }
        });
    }

    private EntityType setOldEntityId(PacketWrapper wrapper) throws Exception {
        int typeId = wrapper.get(Type.VAR_INT, 1);
        EntityType entityType = getTypeFromId(typeId);
        addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), entityType);

        int oldTypeId = getOldEntityId(entityType.getId());
        if (typeId != oldTypeId) {
            wrapper.set(Type.VAR_INT, 1, oldTypeId);
        }

        return entityType;
    }

    /**
     * Helper method to handle a metadata list packet and its full initial meta rewrite.
     */
    protected void registerMetadataRewriter(ClientboundPacketType packetType, Type<List<Metadata>> oldMetaType, Type<List<Metadata>> newMetaType) {
        getProtocol().registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                if (oldMetaType != null) {
                    map(oldMetaType, newMetaType);
                } else {
                    map(newMetaType);
                }
                handler(wrapper -> {
                    int entityId = wrapper.get(Type.VAR_INT, 0);
                    EntityType type = getEntityType(wrapper.user(), entityId);

                    MetaStorage storage = new MetaStorage(wrapper.get(newMetaType, 0));
                    handleMeta(wrapper.user(), entityId, storage);

                    EntityData entityData = getEntityData(type);
                    //TODO only do this once for a first meta packet?
                    if (entityData != null) {
                        if (entityData.hasBaseMeta()) {
                            entityData.getDefaultMeta().createMeta(storage);
                        }
                    }

                    wrapper.set(newMetaType, 0, storage.getMetaDataList());
                });
            }
        });
    }

    protected void registerMetadataRewriter(ClientboundPacketType packetType, Type<List<Metadata>> metaType) {
        registerMetadataRewriter(packetType, null, metaType);
    }
}
