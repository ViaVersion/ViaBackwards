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

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.entities.storage.EntityObjectData;
import com.viaversion.viabackwards.api.entities.storage.EntityReplacement;
import com.viaversion.viabackwards.api.entities.storage.WrappedEntityData;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.ObjectType;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityDataType;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_9;
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
    private final Map<ObjectType, EntityReplacement> objectTypes = new HashMap<>();

    protected LegacyEntityRewriter(T protocol) {
        this(protocol, EntityDataTypes1_9.STRING, EntityDataTypes1_9.BOOLEAN);
    }

    protected LegacyEntityRewriter(T protocol, EntityDataType displayType, EntityDataType displayVisibilityType) {
        super(protocol, displayType, 2, displayVisibilityType, 3);
    }

    protected EntityObjectData mapObjectType(ObjectType oldObjectType, ObjectType replacement, int data) {
        EntityObjectData entData = new EntityObjectData(protocol, oldObjectType.getType().name(), oldObjectType.getId(), replacement.getId(), data);
        objectTypes.put(oldObjectType, entData);
        return entData;
    }

    protected @Nullable EntityReplacement getObjectData(ObjectType type) {
        return objectTypes.get(type);
    }

    protected void registerRespawn(C packetType) {
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT);
                handler(wrapper -> {
                    ClientWorld clientWorld = wrapper.user().getClientWorld(protocol.getClass());
                    if (clientWorld.setEnvironment(wrapper.get(Types.INT, 0))) {
                        tracker(wrapper.user()).clearEntities();
                    }
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
                    ClientWorld clientWorld = wrapper.user().getClientWorld(protocol.getClass());
                    clientWorld.setEnvironment(wrapper.get(Types.INT, 1));

                    final int entityId = wrapper.get(Types.INT, 0);
                    addTrackedEntity(wrapper, entityId, playerType);
                    tracker(wrapper.user()).setClientEntityId(entityId);
                });
            }
        });
    }

    protected PacketHandler getMobSpawnRewriter(Type<List<EntityData>> dataType, IdSetter idSetter) {
        return wrapper -> {
            int entityId = wrapper.get(Types.VAR_INT, 0);
            EntityType type = tracker(wrapper.user()).entityType(entityId);
            if (type == null) {
                return;
            }

            List<EntityData> entityDataList = wrapper.get(dataType, 0);
            handleEntityData(entityId, entityDataList, wrapper.user());

            EntityReplacement entityReplacement = entityDataForType(type);
            if (entityReplacement != null) {
                idSetter.setId(wrapper, entityReplacement.replacementId());
                if (entityReplacement.hasBaseData()) {
                    entityReplacement.defaultData().createData(new WrappedEntityData(entityDataList));
                }
            }
        };
    }

    public PacketHandler getMobSpawnRewriter(Type<List<EntityData>> dataType) {
        return getMobSpawnRewriter(dataType, (wrapper, id) -> wrapper.set(Types.UNSIGNED_BYTE, 0, (short) id));
    }

    public PacketHandler getMobSpawnRewriter1_11(Type<List<EntityData>> dataType) {
        return getMobSpawnRewriter(dataType, (wrapper, id) -> wrapper.set(Types.VAR_INT, 1, id));
    }

    protected PacketHandler getObjectTrackerHandler() {
        return wrapper -> addTrackedEntity(wrapper, wrapper.get(Types.VAR_INT, 0), objectTypeFromId(wrapper.get(Types.BYTE, 0)));
    }

    protected PacketHandler getTrackerAndDataHandler(Type<List<EntityData>> dataType, EntityType entityType) {
        return wrapper -> {
            addTrackedEntity(wrapper, wrapper.get(Types.VAR_INT, 0), entityType);
            List<EntityData> entityDataList = wrapper.get(dataType, 0);
            handleEntityData(wrapper.get(Types.VAR_INT, 0), entityDataList, wrapper.user());
        };
    }

    protected PacketHandler getObjectRewriter(Function<Byte, ObjectType> objectGetter) {
        return wrapper -> {
            ObjectType type = objectGetter.apply(wrapper.get(Types.BYTE, 0));
            if (type == null) {
                return;
            }

            EntityReplacement data = getObjectData(type);
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
