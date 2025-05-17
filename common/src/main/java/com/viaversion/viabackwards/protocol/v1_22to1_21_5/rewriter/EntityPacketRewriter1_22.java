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
package com.viaversion.viabackwards.protocol.v1_22to1_21_5.rewriter;

import com.viaversion.viabackwards.api.rewriters.BackwardsRegistryRewriter;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_22to1_21_5.Protocol1_22To1_21_5;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_22;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_21_5;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ServerboundPackets1_21_5;
import com.viaversion.viaversion.protocols.v1_21_5to1_22.packet.ClientboundConfigurationPackets1_22;
import com.viaversion.viaversion.protocols.v1_21_5to1_22.packet.ClientboundPacket1_22;
import com.viaversion.viaversion.protocols.v1_21_5to1_22.packet.ClientboundPackets1_22;
import com.viaversion.viaversion.rewriter.RegistryDataRewriter;

public final class EntityPacketRewriter1_22 extends EntityRewriter<ClientboundPacket1_22, Protocol1_22To1_21_5> {

    public EntityPacketRewriter1_22(final Protocol1_22To1_21_5 protocol) {
        super(protocol, VersionedTypes.V1_22.entityDataTypes.optionalComponentType, VersionedTypes.V1_22.entityDataTypes.booleanType);
    }

    @Override
    public void registerPackets() {
        registerTrackerWithData1_19(ClientboundPackets1_22.ADD_ENTITY, EntityTypes1_22.FALLING_BLOCK);
        registerSetEntityData(ClientboundPackets1_22.SET_ENTITY_DATA);
        registerRemoveEntities(ClientboundPackets1_22.REMOVE_ENTITIES);
        registerPlayerAbilities(ClientboundPackets1_22.PLAYER_ABILITIES);
        registerGameEvent(ClientboundPackets1_22.GAME_EVENT);
        registerLogin1_20_5(ClientboundPackets1_22.LOGIN);
        registerRespawn1_20_5(ClientboundPackets1_22.RESPAWN);

        final RegistryDataRewriter registryDataRewriter = new BackwardsRegistryRewriter(protocol);
        registryDataRewriter.remove("dialog");
        protocol.registerClientbound(ClientboundConfigurationPackets1_22.REGISTRY_DATA, registryDataRewriter::handle);

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

        filter().type(EntityTypes1_22.HAPPY_GHAST).cancel(17); // Leash holder
        filter().type(EntityTypes1_22.HAPPY_GHAST).cancel(18); // Stays still
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();
        mapEntityTypeWithData(EntityTypes1_22.HAPPY_GHAST, EntityTypes1_22.GHAST).tagName();
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_22.getTypeFromId(type);
    }
}
