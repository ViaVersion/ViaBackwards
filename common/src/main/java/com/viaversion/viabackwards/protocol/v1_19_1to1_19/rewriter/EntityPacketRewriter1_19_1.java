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
package com.viaversion.viabackwards.protocol.v1_19_1to1_19.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_19_1to1_19.Protocol1_19_1To1_19;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19;
import com.viaversion.viaversion.api.type.types.version.Types1_19;
import com.viaversion.viaversion.protocols.v1_19to1_19_1.packet.ClientboundPackets1_19_1;

public final class EntityPacketRewriter1_19_1 extends EntityRewriter<ClientboundPackets1_19_1, Protocol1_19_1To1_19> {

    public EntityPacketRewriter1_19_1(final Protocol1_19_1To1_19 protocol) {
        super(protocol, Types1_19.ENTITY_DATA_TYPES.optionalComponentType, Types1_19.ENTITY_DATA_TYPES.booleanType);
    }

    @Override
    protected void registerPackets() {
        registerSetEntityData(ClientboundPackets1_19_1.SET_ENTITY_DATA, Types1_19.ENTITY_DATA_LIST);
        registerRemoveEntities(ClientboundPackets1_19_1.REMOVE_ENTITIES);
        registerSpawnTracker(ClientboundPackets1_19_1.ADD_ENTITY);
    }

    @Override
    public void registerRewrites() {
        filter().type(EntityTypes1_19.ALLAY).cancel(16); // Dancing
        filter().type(EntityTypes1_19.ALLAY).cancel(17); // Can duplicate
    }

    @Override
    public EntityType typeFromId(final int typeId) {
        return EntityTypes1_19.getTypeFromId(typeId);
    }

}
