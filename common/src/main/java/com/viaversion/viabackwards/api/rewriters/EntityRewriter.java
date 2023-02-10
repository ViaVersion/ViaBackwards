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
package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.metadata.MetaType;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_14;

public abstract class EntityRewriter<C extends ClientboundPacketType, T extends BackwardsProtocol<C, ?, ?, ?>> extends EntityRewriterBase<C, T> {

    protected EntityRewriter(T protocol) {
        this(protocol, Types1_14.META_TYPES.optionalComponentType, Types1_14.META_TYPES.booleanType);
    }

    protected EntityRewriter(T protocol, MetaType displayType, MetaType displayVisibilityType) {
        super(protocol, displayType, 2, displayVisibilityType, 3);
    }

    @Override
    public void registerTrackerWithData(C packetType, EntityType fallingBlockType) {
        protocol.registerClientbound(packetType, new PacketRemapper() {
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
                handler(getSpawnTrackerWithDataHandler(fallingBlockType));
            }
        });
    }

    @Override
    public void registerTrackerWithData1_19(C packetType, EntityType fallingBlockType) {
        protocol.registerClientbound(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Entity id
                map(Type.UUID); // Entity UUID
                map(Type.VAR_INT); // Entity type
                map(Type.DOUBLE); // X
                map(Type.DOUBLE); // Y
                map(Type.DOUBLE); // Z
                map(Type.BYTE); // Pitch
                map(Type.BYTE); // Yaw
                map(Type.BYTE); // Head yaw
                map(Type.VAR_INT); // Data
                handler(getSpawnTrackerWithDataHandler1_19(fallingBlockType));
            }
        });
    }

    public PacketHandler getSpawnTrackerWithDataHandler(EntityType fallingBlockType) {
        return wrapper -> {
            // Check against the UNMAPPED entity type
            EntityType entityType = trackAndMapEntity(wrapper);
            if (entityType == fallingBlockType) {
                int blockState = wrapper.get(Type.INT, 0);
                wrapper.set(Type.INT, 0, protocol.getMappingData().getNewBlockStateId(blockState));
            }
        };
    }

    public PacketHandler getSpawnTrackerWithDataHandler1_19(EntityType fallingBlockType) {
        return wrapper -> {
            // Check against the UNMAPPED entity type
            EntityType entityType = trackAndMapEntity(wrapper);
            if (entityType == fallingBlockType) {
                int blockState = wrapper.get(Type.VAR_INT, 2);
                wrapper.set(Type.VAR_INT, 2, protocol.getMappingData().getNewBlockStateId(blockState));
            }
        };
    }

    public void registerSpawnTracker(C packetType) {
        protocol.registerClientbound(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.UUID); // 1 - Entity UUID
                map(Type.VAR_INT); // 2 - Entity Type
                handler(wrapper -> trackAndMapEntity(wrapper));
            }
        });
    }

    /**
     * Returns a handler to track the current world and uncache entity data on world changes.
     *
     * @return handler to track the current world
     */
    public PacketHandler worldTrackerHandlerByKey() {
        return wrapper -> {
            EntityTracker tracker = tracker(wrapper.user());
            String world = wrapper.get(Type.STRING, 1);
            if (tracker.currentWorld() != null && !tracker.currentWorld().equals(world)) {
                tracker.clearEntities();
                tracker.trackClientEntity();
            }
            tracker.setCurrentWorld(world);
        };
    }

    /**
     * Sets the mapped entity id and returns the unmapped entity type.
     *
     * @param wrapper packet wrapper
     * @return unmapped (!) entity type
     */
    protected EntityType trackAndMapEntity(PacketWrapper wrapper) throws Exception {
        int typeId = wrapper.get(Type.VAR_INT, 1);
        EntityType entityType = typeFromId(typeId);
        tracker(wrapper.user()).addEntity(wrapper.get(Type.VAR_INT, 0), entityType);

        int mappedTypeId = newEntityId(entityType.getId());
        if (typeId != mappedTypeId) {
            wrapper.set(Type.VAR_INT, 1, mappedTypeId);
        }

        return entityType;
    }
}
