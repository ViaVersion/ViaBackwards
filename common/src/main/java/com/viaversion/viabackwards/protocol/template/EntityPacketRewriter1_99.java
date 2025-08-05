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
package com.viaversion.viabackwards.protocol.template;

import com.viaversion.viabackwards.api.rewriters.BackwardsRegistryRewriter;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_4;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_21_2;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundConfigurationPackets1_21;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPackets1_21_2;
import com.viaversion.viaversion.rewriter.RegistryDataRewriter;

// Replace if needed
//  VersionedTypes
final class EntityPacketRewriter1_99 extends EntityRewriter<ClientboundPacket1_21_2, Protocol1_99To1_98> {

    public EntityPacketRewriter1_99(final Protocol1_99To1_98 protocol) {
        super(protocol, VersionedTypes.V1_21_4.entityDataTypes.optionalComponentType, VersionedTypes.V1_21_4.entityDataTypes.booleanType);
    }

    @Override
    public void registerPackets() {
        registerTrackerWithData1_21_9(ClientboundPackets1_21_2.ADD_ENTITY, EntityTypes1_21_4.FALLING_BLOCK);
        registerSetEntityData(ClientboundPackets1_21_2.SET_ENTITY_DATA);
        registerRemoveEntities(ClientboundPackets1_21_2.REMOVE_ENTITIES);
        registerPlayerAbilities(ClientboundPackets1_21_2.PLAYER_ABILITIES);
        registerGameEvent(ClientboundPackets1_21_2.GAME_EVENT);
        registerLogin1_20_5(ClientboundPackets1_21_2.LOGIN);
        registerRespawn1_20_5(ClientboundPackets1_21_2.RESPAWN);

        final RegistryDataRewriter registryDataRewriter = new BackwardsRegistryRewriter(protocol);
        protocol.registerClientbound(ClientboundConfigurationPackets1_21.REGISTRY_DATA, registryDataRewriter::handle);
    }

    @Override
    protected void registerRewrites() {
        final EntityDataTypes1_21_2 mappedEntityDataTypes = VersionedTypes.V1_21_4.entityDataTypes;
        /*filter().handler((event, data) -> {
            int id = data.dataType().typeId();
            if (id >= ac) {
                return;
            } else if (id >= ab) {
                id--;
            }

            data.setDataType(mappedEntityDataTypess.byId(id));
        });*/

        registerEntityDataTypeHandler1_20_3(
            mappedEntityDataTypes.itemType,
            mappedEntityDataTypes.blockStateType,
            mappedEntityDataTypes.optionalBlockStateType,
            mappedEntityDataTypes.particleType,
            mappedEntityDataTypes.particlesType,
            mappedEntityDataTypes.componentType,
            mappedEntityDataTypes.optionalComponentType
        );

        // Remove entity data of new entity type
        // filter().type(EntityTypes1_21_4.SNIFFER).removeIndex(newIndex);
    }

    @Override
    public void onMappingDataLoaded() {
        // If types changed, uncomment to map them
        // mapTypes();

        // mapEntityTypeWithData(EntityTypes1_21_4.SNIFFER, EntityTypes1_21_4.RAVAGER).tagName();
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_21_4.getTypeFromId(type);
    }
}
