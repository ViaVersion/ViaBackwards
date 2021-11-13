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
package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.entities.storage.EntityData;
import com.viaversion.viabackwards.api.entities.storage.EntityObjectData;
import com.viaversion.viabackwards.api.entities.storage.WrappedMetadata;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.ObjectType;
import com.viaversion.viaversion.api.minecraft.metadata.MetaType;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_9;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class LegacyEntityRewriter<T extends BackwardsProtocol> extends EntityRewriterBase<T> {
    private final Map<ObjectType, EntityData> objectTypes = new HashMap<>();

    protected LegacyEntityRewriter(T protocol) {
        this(protocol, MetaType1_9.String, MetaType1_9.Boolean);
    }

    protected LegacyEntityRewriter(T protocol, MetaType displayType, MetaType displayVisibilityType) {
        super(protocol, displayType, 2, displayVisibilityType, 3);
    }

    protected EntityObjectData mapObjectType(ObjectType oldObjectType, ObjectType replacement, int data) {
        EntityObjectData entData = new EntityObjectData(protocol, oldObjectType.getType().name(), oldObjectType.getId(), replacement.getId(), data);
        objectTypes.put(oldObjectType, entData);
        return entData;
    }

    protected @Nullable EntityData getObjectData(ObjectType type) {
        return objectTypes.get(type);
    }

    protected void registerRespawn(ClientboundPacketType packetType) {
        protocol.registerClientbound(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT);
                handler(wrapper -> {
                    ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                    clientWorld.setEnvironment(wrapper.get(Type.INT, 0));
                });
            }
        });
    }

    protected void registerJoinGame(ClientboundPacketType packetType, EntityType playerType) {
        protocol.registerClientbound(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Entity ID
                map(Type.UNSIGNED_BYTE); // 1 - Gamemode
                map(Type.INT); // 2 - Dimension
                handler(wrapper -> {
                    ClientWorld clientChunks = wrapper.user().get(ClientWorld.class);
                    clientChunks.setEnvironment(wrapper.get(Type.INT, 1));
                    addTrackedEntity(wrapper, wrapper.get(Type.INT, 0), playerType);
                });
            }
        });
    }

    @Override
    public void registerMetadataRewriter(ClientboundPacketType packetType, Type<List<Metadata>> oldMetaType, Type<List<Metadata>> newMetaType) {
        protocol.registerClientbound(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                if (oldMetaType != null) {
                    map(oldMetaType, newMetaType);
                } else {
                    map(newMetaType);
                }
                handler(wrapper -> {
                    List<Metadata> metadata = wrapper.get(newMetaType, 0);
                    handleMetadata(wrapper.get(Type.VAR_INT, 0), metadata, wrapper.user());
                });
            }
        });
    }

    @Override
    public void registerMetadataRewriter(ClientboundPacketType packetType, Type<List<Metadata>> metaType) {
        registerMetadataRewriter(packetType, null, metaType);
    }

    protected PacketHandler getMobSpawnRewriter(Type<List<Metadata>> metaType) {
        return wrapper -> {
            int entityId = wrapper.get(Type.VAR_INT, 0);
            EntityType type = tracker(wrapper.user()).entityType(entityId);

            List<Metadata> metadata = wrapper.get(metaType, 0);
            handleMetadata(entityId, metadata, wrapper.user());

            EntityData entityData = entityDataForType(type);
            if (entityData != null) {
                wrapper.set(Type.VAR_INT, 1, entityData.replacementId());
                if (entityData.hasBaseMeta()) {
                    entityData.defaultMeta().createMeta(new WrappedMetadata(metadata));
                }
            }
        };
    }

    protected PacketHandler getObjectTrackerHandler() {
        return wrapper -> addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), getObjectTypeFromId(wrapper.get(Type.BYTE, 0)));
    }

    protected PacketHandler getTrackerAndMetaHandler(Type<List<Metadata>> metaType, EntityType entityType) {
        return wrapper -> {
            addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), entityType);
            List<Metadata> metadata = wrapper.get(metaType, 0);
            handleMetadata(wrapper.get(Type.VAR_INT, 0), metadata, wrapper.user());
        };
    }

    protected PacketHandler getObjectRewriter(Function<Byte, ObjectType> objectGetter) {
        return wrapper -> {
            ObjectType type = objectGetter.apply(wrapper.get(Type.BYTE, 0));
            if (type == null) {
                ViaBackwards.getPlatform().getLogger().warning("Could not find Entity Type" + wrapper.get(Type.BYTE, 0));
                return;
            }

            EntityData data = getObjectData(type);
            if (data != null) {
                wrapper.set(Type.BYTE, 0, (byte) data.replacementId());
                if (data.objectData() != -1) {
                    wrapper.set(Type.INT, 0, data.objectData());
                }
            }
        };
    }

    protected EntityType getObjectTypeFromId(int typeId) {
        return typeFromId(typeId);
    }

    @Deprecated
    protected void addTrackedEntity(PacketWrapper wrapper, int entityId, EntityType type) throws Exception {
        tracker(wrapper.user()).addEntity(entityId, type);
    }
}
