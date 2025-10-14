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
package com.viaversion.viabackwards.protocol.v1_21_7to1_21_6.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_21_7to1_21_6.Protocol1_21_7To1_21_6;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_4;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_6;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_21_5;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ClientboundPacket1_21_6;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ClientboundPackets1_21_6;

public final class EntityPacketRewriter1_21_7 extends EntityRewriter<ClientboundPacket1_21_6, Protocol1_21_7To1_21_6> {

    public EntityPacketRewriter1_21_7(final Protocol1_21_7To1_21_6 protocol) {
        super(protocol, VersionedTypes.V1_21_6.entityDataTypes.optionalComponentType, VersionedTypes.V1_21_6.entityDataTypes.booleanType);
    }

    @Override
    public void registerPackets() {
        registerTrackerWithData1_19(ClientboundPackets1_21_6.ADD_ENTITY, EntityTypes1_21_4.FALLING_BLOCK);
        registerSetEntityData(ClientboundPackets1_21_6.SET_ENTITY_DATA);
        registerRemoveEntities(ClientboundPackets1_21_6.REMOVE_ENTITIES);
        registerPlayerAbilities(ClientboundPackets1_21_6.PLAYER_ABILITIES);
        registerGameEvent(ClientboundPackets1_21_6.GAME_EVENT);
        registerLogin1_20_5(ClientboundPackets1_21_6.LOGIN);
        registerRespawn1_20_5(ClientboundPackets1_21_6.RESPAWN);
    }

    @Override
    protected void registerRewrites() {
        final EntityDataTypes1_21_5 mappedEntityDataTypes = VersionedTypes.V1_21_6.entityDataTypes;
        registerEntityDataTypeHandler1_20_3(
            mappedEntityDataTypes.itemType,
            mappedEntityDataTypes.blockStateType,
            mappedEntityDataTypes.optionalBlockStateType,
            mappedEntityDataTypes.particleType,
            mappedEntityDataTypes.particlesType,
            mappedEntityDataTypes.componentType,
            mappedEntityDataTypes.optionalComponentType
        );
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_21_6.getTypeFromId(type);
    }
}
