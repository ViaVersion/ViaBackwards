/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
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
import com.viaversion.viaversion.api.minecraft.entitydata.EntityDataType;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_14;

public abstract class EntityRewriter<C extends ClientboundPacketType, T extends BackwardsProtocol<C, ?, ?, ?>> extends EntityRewriterBase<C, T> {

    protected EntityRewriter(T protocol) {
        this(protocol, Types1_14.ENTITY_DATA_TYPES.optionalComponentType, Types1_14.ENTITY_DATA_TYPES.booleanType);
    }

    protected EntityRewriter(T protocol, EntityDataType displayType, EntityDataType displayVisibilityType) {
        super(protocol, displayType, 2, displayVisibilityType, 3);
    }

    @Override
    public void registerTrackerWithData(C packetType, EntityType fallingBlockType) {
        protocol.registerClientbound(packetType, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity ID
            wrapper.passthrough(Types.UUID); // Entity UUID
            wrapper.passthrough(Types.VAR_INT); // Entity Type
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z
            wrapper.passthrough(Types.BYTE); // Pitch
            wrapper.passthrough(Types.BYTE); // Yaw
            wrapper.passthrough(Types.INT); // Data
            getSpawnTrackerWithDataHandler(fallingBlockType).handle(wrapper);
        });
    }

    @Override
    public void registerTrackerWithData1_19(C packetType, EntityType fallingBlockType) {
        protocol.registerClientbound(packetType, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity id
            wrapper.passthrough(Types.UUID); // Entity UUID
            wrapper.passthrough(Types.VAR_INT); // Entity type
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z
            wrapper.passthrough(Types.BYTE); // Pitch
            wrapper.passthrough(Types.BYTE); // Yaw
            wrapper.passthrough(Types.BYTE); // Head yaw
            wrapper.passthrough(Types.VAR_INT); // Data
            getSpawnTrackerWithDataHandler1_19(fallingBlockType).handle(wrapper);
        });
    }

    public PacketHandler getSpawnTrackerWithDataHandler(EntityType fallingBlockType) {
        return wrapper -> {
            // Check against the UNMAPPED entity type
            EntityType entityType = trackAndMapEntity(wrapper);
            if (entityType == fallingBlockType) {
                int blockState = wrapper.get(Types.INT, 0);
                wrapper.set(Types.INT, 0, protocol.getMappingData().getNewBlockStateId(blockState));
            }
        };
    }

    public PacketHandler getSpawnTrackerWithDataHandler1_19(EntityType fallingBlockType) {
        return wrapper -> {
            if (protocol.getMappingData() == null) {
                return;
            }

            // Check against the UNMAPPED entity type
            EntityType entityType = trackAndMapEntity(wrapper);
            if (entityType == fallingBlockType) {
                int blockState = wrapper.get(Types.VAR_INT, 2);
                wrapper.set(Types.VAR_INT, 2, protocol.getMappingData().getNewBlockStateId(blockState));
            }
        };
    }

    public void registerSpawnTracker(C packetType) {
        protocol.registerClientbound(packetType, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity ID
            wrapper.passthrough(Types.UUID); // Entity UUID
            wrapper.passthrough(Types.VAR_INT); // Entity Type
            trackAndMapEntity(wrapper);
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
            String world = wrapper.get(Types.STRING, 1);
            if (tracker.currentWorld() != null && !tracker.currentWorld().equals(world)) {
                tracker.clearEntities();
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
    protected EntityType trackAndMapEntity(PacketWrapper wrapper) {
        int typeId = wrapper.get(Types.VAR_INT, 1);
        EntityType entityType = typeFromId(typeId);
        if (entityType == null) {
            return null;
        }

        tracker(wrapper.user()).addEntity(wrapper.get(Types.VAR_INT, 0), entityType);

        int mappedTypeId = newEntityId(entityType.getId());
        if (typeId != mappedTypeId) {
            wrapper.set(Types.VAR_INT, 1, mappedTypeId);
        }

        return entityType;
    }
}
