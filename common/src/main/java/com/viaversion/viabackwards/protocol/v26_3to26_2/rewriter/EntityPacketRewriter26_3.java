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
package com.viaversion.viabackwards.protocol.v26_3to26_2.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v26_3to26_2.Protocol26_3To26_2;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes26_3;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes26_1;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPacket26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;

public final class EntityPacketRewriter26_3 extends EntityRewriter<ClientboundPacket26_3, Protocol26_3To26_2> {

    private static final EntityDataTypes26_1 MAPPED_DATA_TYPES = VersionedTypes.V26_2.entityDataTypes;

    public EntityPacketRewriter26_3(final Protocol26_3To26_2 protocol) {
        super(protocol, MAPPED_DATA_TYPES.optionalComponentType, MAPPED_DATA_TYPES.booleanType);
    }

    @Override
    public void registerPackets() {
        protocol.registerClientbound(ClientboundPackets26_3.MOVE_ENTITY_POS, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity ID

            final int properties = wrapper.read(Types.VAR_INT);
            final boolean onGround = (properties & 1) != 0;
            handleMovePos(wrapper, properties);
            wrapper.write(Types.BOOLEAN, onGround);
        });

        protocol.registerClientbound(ClientboundPackets26_3.MOVE_ENTITY_POS_ROT, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity ID

            final int properties = wrapper.read(Types.VAR_INT);
            final boolean onGround = (properties & 1) != 0;
            handleMovePos(wrapper, properties);

            wrapper.passthrough(Types.BYTE); // Y rot
            wrapper.passthrough(Types.BYTE); // X rot

            wrapper.write(Types.BOOLEAN, onGround);
        });

        protocol.registerClientbound(ClientboundPackets26_3.MOVE_ENTITY_ROT, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity ID
            final boolean onGround = wrapper.read(Types.BOOLEAN); // on ground stays as boolean; moved down
            wrapper.passthrough(Types.BYTE); // Y rot
            wrapper.passthrough(Types.BYTE); // X rot
            wrapper.write(Types.BOOLEAN, onGround);
        });

        protocol.registerClientbound(ClientboundPackets26_3.ENTITY_POSITION_SYNC, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity ID

            // TODO Calculate delta movement?
            final int stepType = wrapper.read(Types.VAR_INT);
            if (stepType == 0) {
                wrapper.passthrough(Types.DOUBLE); // X
                wrapper.passthrough(Types.DOUBLE); // Y
                wrapper.passthrough(Types.DOUBLE); // Z
                wrapper.write(Types.DOUBLE, 0D); // Delta x
                wrapper.write(Types.DOUBLE, 0D); // Delta y
                wrapper.write(Types.DOUBLE, 0D); // Delta z
            } else {
                // Only keep the last
                final int steps = wrapper.read(Types.VAR_INT);
                for (int i = 0; i < steps - 1; i++) {
                    wrapper.read(Types.DOUBLE); // X
                    wrapper.read(Types.DOUBLE); // Y
                    wrapper.read(Types.DOUBLE); // Z
                    wrapper.read(Types.VAR_INT); // Tick offset
                }

                wrapper.passthrough(Types.DOUBLE); // X
                wrapper.passthrough(Types.DOUBLE); // Y
                wrapper.passthrough(Types.DOUBLE); // Z
                wrapper.read(Types.VAR_INT); // Tick offset
                wrapper.write(Types.DOUBLE, 0D); // Delta x
                wrapper.write(Types.DOUBLE, 0D); // Delta y
                wrapper.write(Types.DOUBLE, 0D); // Delta z
            }
        });
    }

    private void handleMovePos(final PacketWrapper wrapper, final int properties) {
        // Only keep the last step, ignore the tick delay // TODO
        final int steps = properties >>> 1;
        if (steps == 0) {
            wrapper.passthrough(Types.SHORT); // Delta x
            wrapper.passthrough(Types.SHORT); // Delta y
            wrapper.passthrough(Types.SHORT); // Delta z
            return;
        }

        for (int i = 0; i < steps - 1; i++) {
            wrapper.read(Types.VAR_INT); // Tick delay
            wrapper.read(Types.SHORT); // Delta x
            wrapper.read(Types.SHORT); // Delta y
            wrapper.read(Types.SHORT); // Delta z
        }

        wrapper.read(Types.VAR_INT); // Tick delay
        wrapper.passthrough(Types.SHORT); // Delta x
        wrapper.passthrough(Types.SHORT); // Delta y
        wrapper.passthrough(Types.SHORT); // Delta z
    }

    @Override
    protected void registerRewrites() {
        dataTypeMapper().removed(VersionedTypes.V26_3.entityDataTypes.dyeColorType).register();
        registerEntityDataTypeHandler1_20_3(
            MAPPED_DATA_TYPES.itemType,
            MAPPED_DATA_TYPES.blockStateType,
            MAPPED_DATA_TYPES.optionalBlockStateType,
            MAPPED_DATA_TYPES.particleType,
            MAPPED_DATA_TYPES.particlesType,
            MAPPED_DATA_TYPES.componentType,
            MAPPED_DATA_TYPES.optionalComponentType
        );
        filter().type(EntityTypes26_3.CUSHION).handler((event, data) -> {
            if (event.index() > 7) {
                event.cancel(); // Cancel all non-entity data
            }
        });
    }

    @Override
    public void onMappingDataLoaded() {
        super.onMappingDataLoaded();
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes26_3.getTypeFromId(type);
    }
}
