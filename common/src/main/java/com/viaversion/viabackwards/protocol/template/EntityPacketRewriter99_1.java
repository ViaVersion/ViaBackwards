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
package com.viaversion.viabackwards.protocol.template;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_11;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_21_11;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPacket26_1;

// Replace if needed
//  VersionedTypes
final class EntityPacketRewriter99_1 extends EntityRewriter<ClientboundPacket26_1, Protocol99_1To98_1> {

    private static final EntityDataTypes1_21_11 MAPPED_DATA_TYPES = VersionedTypes.V1_21_11.entityDataTypes;

    public EntityPacketRewriter99_1(final Protocol99_1To98_1 protocol) {
        super(protocol, MAPPED_DATA_TYPES.optionalComponentType, MAPPED_DATA_TYPES.booleanType);
    }

    @Override
    public void registerPackets() {
    }

    @Override
    protected void registerRewrites() {
        /*filter().handler((event, data) -> {
            int id = data.dataType().typeId();
            if (id >= ac) {
                return;
            } else if (id >= ab) {
                id--;
            }

            data.setDataType(MAPPED_DATA_TYPES.byId(id));
        });*/

        registerEntityDataTypeHandler1_20_3(
            MAPPED_DATA_TYPES.itemType,
            MAPPED_DATA_TYPES.blockStateType,
            MAPPED_DATA_TYPES.optionalBlockStateType,
            MAPPED_DATA_TYPES.particleType,
            MAPPED_DATA_TYPES.particlesType,
            MAPPED_DATA_TYPES.componentType,
            MAPPED_DATA_TYPES.optionalComponentType
        );

        // Remove entity data of new entity type
        // filter().type(EntityTypes1_21_11.SNIFFER).removeIndex(newIndex);
    }

    @Override
    public void onMappingDataLoaded() {
        // If types changed, uncomment to map them
        // mapTypes();

        // mapEntityTypeWithData(EntityTypes1_21_11.SNIFFER, EntityTypes1_21_11.RAVAGER).tagName();
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_21_11.getTypeFromId(type);
    }
}
