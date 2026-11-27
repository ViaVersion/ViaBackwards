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
import com.viaversion.viaversion.api.data.entity.TrackedEntity;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityDataType;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_14;

import java.util.ArrayList;
import java.util.List;

public abstract class EntityRewriter<C extends ClientboundPacketType, T extends BackwardsProtocol<C, ?, ?, ?>> extends EntityRewriterBase<C, T> {

    protected EntityRewriter(T protocol) {
        this(protocol, Types1_14.ENTITY_DATA_TYPES.optionalComponentType, Types1_14.ENTITY_DATA_TYPES.booleanType);
    }

    protected EntityRewriter(T protocol, EntityDataType displayType, EntityDataType displayVisibilityType) {
        super(protocol, displayType, 2, displayVisibilityType, 3);
    }

    public PacketHandler getSpawnTrackerWithDataHandler() {
        return wrapper -> {
            int entityId = wrapper.get(Types.VAR_INT, 0);
            int typeId = wrapper.get(Types.VAR_INT, 1);
            TrackedEntity entity = trackAndRewrite(wrapper, typeId, entityId);
            if (entity.entityType() == fallingBlockType()) {
                int blockState = wrapper.get(Types.INT, 0);
                wrapper.set(Types.INT, 0, protocol.getMappingData().getNewBlockStateId(blockState));
            }
        };
    }

    public void trackSpawnWithData1_19(final PacketWrapper wrapper, final int data) {
        if (protocol.getMappingData() == null) {
            return;
        }

        int entityId = wrapper.get(Types.VAR_INT, 0);
        int typeId = wrapper.get(Types.VAR_INT, 1);
        TrackedEntity entity = trackAndRewrite(wrapper, typeId, entityId);
        if (entity.entityType() == fallingBlockType()) {
            wrapper.set(Types.VAR_INT, 2, protocol.getMappingData().getNewBlockStateId(data));
        }
    }

    /**
     * Spawn handler that sends default entity data in 1.19.3+.
     *
     * <p>Starting from 1.19.3, default entity data is no longer sent by the server, so any entity replacement
     * with default data may never apply that data if no packet is sent.
     *
     * <p>Note: this should be the last handler in the chain.
     */
    protected <CM extends ClientboundPacketType> PacketHandler defaultSpawnDataHandler(CM setEntityData, Type<List<EntityData>> mappedType) {
        return wrapper -> {
            final int entityId = wrapper.get(Types.VAR_INT, 0);

            final List<EntityData> spawnData = new ArrayList<>();
            handleEntityData(entityId, spawnData, wrapper.user());
            if (!spawnData.isEmpty()) {
                // we also want to overwrite the first actual data sent by the server
                tracker(wrapper.user()).entity(entityId).sentEntityData(false);

                wrapper.send(protocol.getClass());
                wrapper.cancel();

                final PacketWrapper packet = wrapper.create(setEntityData);
                packet.write(Types.VAR_INT, entityId);
                packet.write(mappedType, spawnData);
                packet.send(protocol.getClass());
            }
        };
    }
}
