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
package com.viaversion.viabackwards.protocol.v26_1to1_21_11.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v26_1to1_21_11.Protocol26_1To1_21_11;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_11;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_21_11;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_21_9to1_21_11.packet.ClientboundPacket1_21_11;
import com.viaversion.viaversion.protocols.v1_21_9to1_21_11.packet.ClientboundPackets1_21_11;

public final class EntityPacketRewriter26_1 extends EntityRewriter<ClientboundPacket1_21_11, Protocol26_1To1_21_11> {

    public EntityPacketRewriter26_1(final Protocol26_1To1_21_11 protocol) {
        super(protocol, VersionedTypes.V1_21_11.entityDataTypes.optionalComponentType, VersionedTypes.V1_21_11.entityDataTypes.booleanType);
    }

    @Override
    public void registerPackets() {
        registerTrackerWithData1_21_9(ClientboundPackets1_21_11.ADD_ENTITY, EntityTypes1_21_11.FALLING_BLOCK);
        registerSetEntityData(ClientboundPackets1_21_11.SET_ENTITY_DATA);
        registerRemoveEntities(ClientboundPackets1_21_11.REMOVE_ENTITIES);
        registerPlayerAbilities(ClientboundPackets1_21_11.PLAYER_ABILITIES);
        registerGameEvent(ClientboundPackets1_21_11.GAME_EVENT);
        registerLogin1_20_5(ClientboundPackets1_21_11.LOGIN);
        registerRespawn1_20_5(ClientboundPackets1_21_11.RESPAWN);
    }

    @Override
    protected void registerRewrites() {
        final EntityDataTypes1_21_11 mappedEntityDataTypes = VersionedTypes.V1_21_11.entityDataTypes;
        registerEntityDataTypeHandler1_20_3(
            mappedEntityDataTypes.itemType,
            mappedEntityDataTypes.blockStateType,
            mappedEntityDataTypes.optionalBlockStateType,
            mappedEntityDataTypes.particleType,
            mappedEntityDataTypes.particlesType,
            mappedEntityDataTypes.componentType,
            mappedEntityDataTypes.optionalComponentType
        );

        filter().type(EntityTypes1_21_11.ABSTRACT_VILLAGER).removeIndex(18); // Is villager data finalized
        filter().type(EntityTypes1_21_11.ZOMBIE_VILLAGER).removeIndex(21); // Is villager data finalized
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_21_11.getTypeFromId(type);
    }
}
