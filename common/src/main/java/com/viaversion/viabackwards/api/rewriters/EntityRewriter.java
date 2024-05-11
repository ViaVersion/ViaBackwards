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
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.metadata.MetaType;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
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
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity id
                map(Types.UUID); // 1 - Entity UUID
                map(Types.VAR_INT); // 2 - Entity Type
                map(Types.DOUBLE); // 3 - X
                map(Types.DOUBLE); // 4 - Y
                map(Types.DOUBLE); // 5 - Z
                map(Types.BYTE); // 6 - Pitch
                map(Types.BYTE); // 7 - Yaw
                map(Types.INT); // 8 - Data
                handler(getSpawnTrackerWithDataHandler(fallingBlockType));
            }
        });
    }

    @Override
    public void registerTrackerWithData1_19(C packetType, EntityType fallingBlockType) {
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Entity id
                map(Types.UUID); // Entity UUID
                map(Types.VAR_INT); // Entity type
                map(Types.DOUBLE); // X
                map(Types.DOUBLE); // Y
                map(Types.DOUBLE); // Z
                map(Types.BYTE); // Pitch
                map(Types.BYTE); // Yaw
                map(Types.BYTE); // Head yaw
                map(Types.VAR_INT); // Data
                handler(getSpawnTrackerWithDataHandler1_19(fallingBlockType));
            }
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
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity ID
                map(Types.UUID); // 1 - Entity UUID
                map(Types.VAR_INT); // 2 - Entity Type
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
            String world = wrapper.get(Types.STRING, 1);
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
    protected EntityType trackAndMapEntity(PacketWrapper wrapper) {
        int typeId = wrapper.get(Types.VAR_INT, 1);
        EntityType entityType = typeFromId(typeId);
        tracker(wrapper.user()).addEntity(wrapper.get(Types.VAR_INT, 0), entityType);

        int mappedTypeId = newEntityId(entityType.getId());
        if (typeId != mappedTypeId) {
            wrapper.set(Types.VAR_INT, 1, mappedTypeId);
        }

        return entityType;
    }
}
