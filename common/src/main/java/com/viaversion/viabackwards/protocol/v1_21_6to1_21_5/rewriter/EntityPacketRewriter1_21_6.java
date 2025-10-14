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
package com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.Protocol1_21_6To1_21_5;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_6;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_21_5;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ClientboundPackets1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ServerboundPackets1_21_5;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ClientboundPacket1_21_6;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ClientboundPackets1_21_6;

public final class EntityPacketRewriter1_21_6 extends EntityRewriter<ClientboundPacket1_21_6, Protocol1_21_6To1_21_5> {

    public EntityPacketRewriter1_21_6(final Protocol1_21_6To1_21_5 protocol) {
        super(protocol, VersionedTypes.V1_21_6.entityDataTypes.optionalComponentType, VersionedTypes.V1_21_6.entityDataTypes.booleanType);
    }

    @Override
    public void registerPackets() {
        registerSetEntityData(ClientboundPackets1_21_6.SET_ENTITY_DATA);
        registerRemoveEntities(ClientboundPackets1_21_6.REMOVE_ENTITIES);
        registerPlayerAbilities(ClientboundPackets1_21_6.PLAYER_ABILITIES);
        registerGameEvent(ClientboundPackets1_21_6.GAME_EVENT);
        registerLogin1_20_5(ClientboundPackets1_21_6.LOGIN);
        registerRespawn1_20_5(ClientboundPackets1_21_6.RESPAWN);

        protocol.appendClientbound(ClientboundPackets1_21_6.ADD_ENTITY, wrapper -> {
            final int entityId = wrapper.passthrough(Types.VAR_INT);
            wrapper.passthrough(Types.UUID); // Entity UUID
            final int entityType = wrapper.passthrough(Types.VAR_INT);
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z
            wrapper.passthrough(Types.BYTE); // Pitch
            wrapper.passthrough(Types.BYTE); // Yaw
            wrapper.passthrough(Types.BYTE); // Head yaw
            wrapper.passthrough(Types.VAR_INT); // Data
            final short velocityX = wrapper.passthrough(Types.SHORT);
            final short velocityY = wrapper.passthrough(Types.SHORT);
            final short velocityZ = wrapper.passthrough(Types.SHORT);
            getSpawnTrackerWithDataHandler1_19(EntityTypes1_21_6.FALLING_BLOCK).handle(wrapper);
            if (velocityX != 0 || velocityY != 0 || velocityZ != 0) {
                if (!typeFromId(entityType).isOrHasParent(EntityTypes1_21_6.LIVING_ENTITY)) {
                    // Send movement separately
                    final PacketWrapper motionPacket = wrapper.create(ClientboundPackets1_21_5.SET_ENTITY_MOTION);
                    motionPacket.write(Types.VAR_INT, entityId);
                    motionPacket.write(Types.SHORT, velocityX);
                    motionPacket.write(Types.SHORT, velocityY);
                    motionPacket.write(Types.SHORT, velocityZ);
                    wrapper.send(Protocol1_21_6To1_21_5.class);
                    motionPacket.send(Protocol1_21_6To1_21_5.class);
                    wrapper.cancel();
                }
            }
        });

        protocol.registerServerbound(ServerboundPackets1_21_5.PLAYER_COMMAND, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity ID

            // press_shift_key and release_shift_key gone. The server uses (the already sent) player input instead
            final int action = wrapper.read(Types.VAR_INT);
            if (action < 2) {
                wrapper.cancel();
            }

            wrapper.write(Types.VAR_INT, action - 2);
        });
    }

    @Override
    protected void registerRewrites() {
        final EntityDataTypes1_21_5 entityDataTypes = VersionedTypes.V1_21_5.entityDataTypes;
        filter().mapDataType(entityDataTypes::byId);
        registerEntityDataTypeHandler1_20_3(
            entityDataTypes.itemType,
            entityDataTypes.blockStateType,
            entityDataTypes.optionalBlockStateType,
            entityDataTypes.particleType,
            entityDataTypes.particlesType,
            entityDataTypes.componentType,
            entityDataTypes.optionalComponentType
        );

        filter().type(EntityTypes1_21_6.HANGING_ENTITY).removeIndex(8); // Direction
        filter().type(EntityTypes1_21_6.HAPPY_GHAST).cancel(17); // Leash holder
        filter().type(EntityTypes1_21_6.HAPPY_GHAST).cancel(18); // Stays still
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();
        mapEntityTypeWithData(EntityTypes1_21_6.HAPPY_GHAST, EntityTypes1_21_6.GHAST).tagName();
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_21_6.getTypeFromId(type);
    }
}
