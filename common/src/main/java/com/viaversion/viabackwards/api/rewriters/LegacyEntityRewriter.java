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
package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.entities.storage.EntityData;
import com.viaversion.viabackwards.api.entities.storage.EntityObjectData;
import com.viaversion.viabackwards.api.entities.storage.WrappedMetadata;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.ObjectType;
import com.viaversion.viaversion.api.minecraft.metadata.MetaType;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_9;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class LegacyEntityRewriter<C extends ClientboundPacketType, T extends BackwardsProtocol<C, ?, ?, ?>> extends EntityRewriterBase<C, T> {
    private final Map<ObjectType, EntityData> objectTypes = new HashMap<>();

    protected LegacyEntityRewriter(T protocol) {
        this(protocol, MetaType1_9.STRING, MetaType1_9.BOOLEAN);
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

    protected void registerRespawn(C packetType) {
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT);
                handler(wrapper -> {
                    ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                    clientWorld.setEnvironment(wrapper.get(Types.INT, 0));
                });
            }
        });
    }

    protected void registerJoinGame(C packetType, EntityType playerType) {
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // 0 - Entity ID
                map(Types.UNSIGNED_BYTE); // 1 - Gamemode
                map(Types.INT); // 2 - Dimension
                handler(wrapper -> {
                    ClientWorld clientChunks = wrapper.user().get(ClientWorld.class);
                    clientChunks.setEnvironment(wrapper.get(Types.INT, 1));
                    addTrackedEntity(wrapper, wrapper.get(Types.INT, 0), playerType);
                });
            }
        });
    }

    @Override
    public void registerMetadataRewriter(C packetType, Type<List<Metadata>> oldMetaType, Type<List<Metadata>> newMetaType) {
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity ID
                if (oldMetaType != null) {
                    map(oldMetaType, newMetaType);
                } else {
                    map(newMetaType);
                }
                handler(wrapper -> {
                    List<Metadata> metadata = wrapper.get(newMetaType, 0);
                    handleMetadata(wrapper.get(Types.VAR_INT, 0), metadata, wrapper.user());
                });
            }
        });
    }

    @Override
    public void registerMetadataRewriter(C packetType, Type<List<Metadata>> metaType) {
        registerMetadataRewriter(packetType, null, metaType);
    }

    protected PacketHandler getMobSpawnRewriter(Type<List<Metadata>> metaType, IdSetter idSetter) {
        return wrapper -> {
            int entityId = wrapper.get(Types.VAR_INT, 0);
            EntityType type = tracker(wrapper.user()).entityType(entityId);

            List<Metadata> metadata = wrapper.get(metaType, 0);
            handleMetadata(entityId, metadata, wrapper.user());

            EntityData entityData = entityDataForType(type);
            if (entityData != null) {
                idSetter.setId(wrapper, entityData.replacementId());
                if (entityData.hasBaseMeta()) {
                    entityData.defaultMeta().createMeta(new WrappedMetadata(metadata));
                }
            }
        };
    }

    public PacketHandler getMobSpawnRewriter(Type<List<Metadata>> metaType) {
        return getMobSpawnRewriter(metaType, (wrapper, id) -> wrapper.set(Types.UNSIGNED_BYTE, 0, (short) id));
    }

    public PacketHandler getMobSpawnRewriter1_11(Type<List<Metadata>> metaType) {
        return getMobSpawnRewriter(metaType, (wrapper, id) -> wrapper.set(Types.VAR_INT, 1, id));
    }

    protected PacketHandler getObjectTrackerHandler() {
        return wrapper -> addTrackedEntity(wrapper, wrapper.get(Types.VAR_INT, 0), objectTypeFromId(wrapper.get(Types.BYTE, 0)));
    }

    protected PacketHandler getTrackerAndMetaHandler(Type<List<Metadata>> metaType, EntityType entityType) {
        return wrapper -> {
            addTrackedEntity(wrapper, wrapper.get(Types.VAR_INT, 0), entityType);
            List<Metadata> metadata = wrapper.get(metaType, 0);
            handleMetadata(wrapper.get(Types.VAR_INT, 0), metadata, wrapper.user());
        };
    }

    protected PacketHandler getObjectRewriter(Function<Byte, ObjectType> objectGetter) {
        return wrapper -> {
            ObjectType type = objectGetter.apply(wrapper.get(Types.BYTE, 0));
            if (type == null) {
                protocol.getLogger().warning("Could not find Entity Type" + wrapper.get(Types.BYTE, 0));
                return;
            }

            EntityData data = getObjectData(type);
            if (data != null) {
                wrapper.set(Types.BYTE, 0, (byte) data.replacementId());
                if (data.objectData() != -1) {
                    wrapper.set(Types.INT, 0, data.objectData());
                }
            }
        };
    }

    @Deprecated
    protected void addTrackedEntity(PacketWrapper wrapper, int entityId, EntityType type) {
        tracker(wrapper.user()).addEntity(entityId, type);
    }

    @FunctionalInterface
    protected interface IdSetter {

        void setId(PacketWrapper wrapper, int id);
    }
}
