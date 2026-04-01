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
package com.viaversion.viabackwards.protocol.v1_21_4to1_21_2.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_21_4to1_21_2.Protocol1_21_4To1_21_2;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_4;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.packet.ServerboundPackets1_21_4;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPackets1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPackets1_21_2;

public final class EntityPacketRewriter1_21_4 extends EntityRewriter<ClientboundPacket1_21_2, Protocol1_21_4To1_21_2> {

    public EntityPacketRewriter1_21_4(final Protocol1_21_4To1_21_2 protocol) {
        super(protocol, VersionedTypes.V1_21_4.entityDataTypes.optionalComponentType, VersionedTypes.V1_21_4.entityDataTypes.booleanType);
    }

    @Override
    public void registerPackets() {
        protocol.replaceClientbound(ClientboundPackets1_21_2.LOGIN, wrapper -> {
            final int entityId = wrapper.passthrough(Types.INT); // Entity id
            wrapper.passthrough(Types.BOOLEAN); // Hardcore
            wrapper.passthrough(Types.STRING_ARRAY); // World List
            wrapper.passthrough(Types.VAR_INT); // Max players
            wrapper.passthrough(Types.VAR_INT); // View distance
            wrapper.passthrough(Types.VAR_INT); // Simulation distance
            wrapper.passthrough(Types.BOOLEAN); // Reduced debug info
            wrapper.passthrough(Types.BOOLEAN); // Show death screen
            wrapper.passthrough(Types.BOOLEAN); // Limited crafting
            final int dimensionId = wrapper.passthrough(Types.VAR_INT);
            final String world = wrapper.passthrough(Types.STRING);
            trackWorldDataByKey1_20_5(wrapper.user(), dimensionId, world);
            trackPlayer(wrapper.user(), entityId);

            final PacketWrapper playerLoadedPacket = wrapper.create(ServerboundPackets1_21_4.PLAYER_LOADED);
            playerLoadedPacket.scheduleSendToServer(Protocol1_21_4To1_21_2.class);
        });

        protocol.replaceClientbound(ClientboundPackets1_21_2.RESPAWN, wrapper -> {
            final int dimensionId = wrapper.passthrough(Types.VAR_INT);
            final String world = wrapper.passthrough(Types.STRING);
            trackWorldDataByKey1_20_5(wrapper.user(), dimensionId, world);

            final PacketWrapper playerLoadedPacket = wrapper.create(ServerboundPackets1_21_4.PLAYER_LOADED);
            playerLoadedPacket.scheduleSendToServer(Protocol1_21_4To1_21_2.class);
        });

        protocol.registerServerbound(ServerboundPackets1_21_2.MOVE_VEHICLE, wrapper -> {
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z
            wrapper.passthrough(Types.FLOAT); // Yaw
            wrapper.passthrough(Types.FLOAT); // Pitch
            wrapper.write(Types.BOOLEAN, true); // On ground // TODO ...
        });
    }

    @Override
    protected void registerRewrites() {
        filter().mapDataType(VersionedTypes.V1_21_2.entityDataTypes::byId);
        registerEntityDataTypeHandler1_20_3(
            VersionedTypes.V1_21_2.entityDataTypes.itemType,
            VersionedTypes.V1_21_2.entityDataTypes.blockStateType,
            VersionedTypes.V1_21_2.entityDataTypes.optionalBlockStateType,
            VersionedTypes.V1_21_2.entityDataTypes.particleType,
            VersionedTypes.V1_21_2.entityDataTypes.particlesType,
            VersionedTypes.V1_21_2.entityDataTypes.componentType,
            VersionedTypes.V1_21_2.entityDataTypes.optionalComponentType
        );
        registerBlockStateHandler(EntityTypes1_21_4.ABSTRACT_MINECART, 11);

        filter().type(EntityTypes1_21_4.CREAKING).removeIndex(19); // Home pos
        filter().type(EntityTypes1_21_4.CREAKING).removeIndex(18); // Is tearing down
        filter().type(EntityTypes1_21_4.SALMON).index(17).handler((event, data) -> {
            final int typeId = data.value();
            final String type = switch (typeId) {
                case 0 -> "small";
                case 2 -> "large";
                default -> "medium";
            };
            data.setTypeAndValue(VersionedTypes.V1_21_4.entityDataTypes.stringType, type);
        });
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_21_4.getTypeFromId(type);
    }
}
