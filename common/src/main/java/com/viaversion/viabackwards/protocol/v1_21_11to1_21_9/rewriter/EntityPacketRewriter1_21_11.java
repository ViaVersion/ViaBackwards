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
package com.viaversion.viabackwards.protocol.v1_21_11to1_21_9.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_21_11to1_21_9.Protocol1_21_11To1_21_9;
import com.viaversion.viabackwards.protocol.v1_21_11to1_21_9.storage.GameTimeStorage;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_11;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_21_9;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundPacket1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundPackets1_21_9;
import com.viaversion.viaversion.rewriter.entitydata.EntityDataHandlerEvent;

public final class EntityPacketRewriter1_21_11 extends EntityRewriter<ClientboundPacket1_21_9, Protocol1_21_11To1_21_9> {

    public EntityPacketRewriter1_21_11(final Protocol1_21_11To1_21_9 protocol) {
        super(protocol, VersionedTypes.V1_21_9.entityDataTypes.optionalComponentType, VersionedTypes.V1_21_9.entityDataTypes.booleanType);
    }

    @Override
    public void registerPackets() {
        registerTrackerWithData1_21_9(ClientboundPackets1_21_9.ADD_ENTITY, EntityTypes1_21_11.FALLING_BLOCK);
        registerSetEntityData(ClientboundPackets1_21_9.SET_ENTITY_DATA);
        registerRemoveEntities(ClientboundPackets1_21_9.REMOVE_ENTITIES);
        registerPlayerAbilities(ClientboundPackets1_21_9.PLAYER_ABILITIES);
        registerGameEvent(ClientboundPackets1_21_9.GAME_EVENT);
        registerLogin1_20_5(ClientboundPackets1_21_9.LOGIN);
        registerRespawn1_20_5(ClientboundPackets1_21_9.RESPAWN);
    }

    @Override
    protected void registerRewrites() {
        final EntityDataTypes1_21_9 entityDataTypes = VersionedTypes.V1_21_9.entityDataTypes;
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

        filter().type(EntityTypes1_21_11.WOLF).index(20).handler(this::absoluteToRelativeTicks);
        filter().type(EntityTypes1_21_11.BEE).index(18).handler(this::absoluteToRelativeTicks);

        filter().type(EntityTypes1_21_11.ABSTRACT_NAUTILUS).removeIndex(18); // Dashing
    }

    private void absoluteToRelativeTicks(final EntityDataHandlerEvent event, final EntityData data) {
        final long currentGameTime = event.user().get(GameTimeStorage.class).gameTime();
        final long angerEndTime = data.value();
        final int angerEndIn = Math.toIntExact(angerEndTime - currentGameTime);
        data.setTypeAndValue(VersionedTypes.V1_21_9.entityDataTypes.varIntType, Math.max(angerEndIn, 0));
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();
        mapEntityTypeWithData(EntityTypes1_21_11.NAUTILUS, EntityTypes1_21_11.SQUID).tagName();
        mapEntityTypeWithData(EntityTypes1_21_11.ZOMBIE_NAUTILUS, EntityTypes1_21_11.GLOW_SQUID).tagName();
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_21_11.getTypeFromId(type);
    }
}
