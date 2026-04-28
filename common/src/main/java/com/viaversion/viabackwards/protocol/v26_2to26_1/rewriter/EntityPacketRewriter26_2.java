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
package com.viaversion.viabackwards.protocol.v26_2to26_1.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v26_2to26_1.Protocol26_2To26_1;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes26_2;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes26_1;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPacket26_1;

public final class EntityPacketRewriter26_2 extends EntityRewriter<ClientboundPacket26_1, Protocol26_2To26_1> {

    private static final EntityDataTypes26_1 MAPPED_DATA_TYPES = VersionedTypes.V26_1.entityDataTypes;

    public EntityPacketRewriter26_2(final Protocol26_2To26_1 protocol) {
        super(protocol, MAPPED_DATA_TYPES.optionalComponentType, MAPPED_DATA_TYPES.booleanType);
    }

    @Override
    protected void registerRewrites() {
        dataTypeMapper().register();
        registerEntityDataTypeHandler1_20_3(
            MAPPED_DATA_TYPES.itemType,
            MAPPED_DATA_TYPES.blockStateType,
            MAPPED_DATA_TYPES.optionalBlockStateType,
            MAPPED_DATA_TYPES.particleType,
            MAPPED_DATA_TYPES.particlesType,
            MAPPED_DATA_TYPES.componentType,
            MAPPED_DATA_TYPES.optionalComponentType
        );

        filter().type(EntityTypes26_2.SULFUR_CUBE).cancel(19); // max fuse
        filter().type(EntityTypes26_2.SULFUR_CUBE).cancel(20); // from bucket
        filter().type(EntityTypes26_2.ABSTRACT_CUBE_MOB).removeIndex(17); // age locked
        filter().type(EntityTypes26_2.ABSTRACT_CUBE_MOB).removeIndex(16); // baby
    }

    @Override
    public void onMappingDataLoaded() {
        super.onMappingDataLoaded();
        mapEntityTypeWithData(EntityTypes26_2.SULFUR_CUBE, EntityTypes26_2.MAGMA_CUBE).tagName();
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes26_2.getTypeFromId(type);
    }
}
