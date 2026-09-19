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
import com.viaversion.viabackwards.protocol.v26_3to26_2.storage.ProtocolStorables26_3;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes26_3;
import com.viaversion.viaversion.api.data.entity.TrackedEntity;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes26_1;
import com.viaversion.viaversion.api.minecraft.item.data.SwingAnimation;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPackets26_1;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPacket26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ServerboundPackets26_3;
import java.util.ArrayList;
import java.util.List;

public final class EntityPacketRewriter26_3 extends EntityRewriter<ClientboundPacket26_3, Protocol26_3To26_2> {

    private static final EntityDataTypes26_1 MAPPED_DATA_TYPES = VersionedTypes.V26_2.entityDataTypes;
    private static final int NO_SWING_ANIMATION = 0;
    private static final int CHANGE_DESTROY_DIRECTION_ACTION = 1;
    private static final int WHITE_CARPET_BLOCK_STATE = 12896;

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

            final int pathType = wrapper.read(Types.VAR_INT);
            if (pathType == 0) { // Linear
                wrapper.passthrough(Types.DOUBLE); // X
                wrapper.passthrough(Types.DOUBLE); // Y
                wrapper.passthrough(Types.DOUBLE); // Z
                wrapper.write(Types.DOUBLE, 0D); // Delta x
                wrapper.write(Types.DOUBLE, 0D); // Delta y
                wrapper.write(Types.DOUBLE, 0D); // Delta z
                return;
            }

            // Only keep the last position; get the delta movement from the final step and hope that's right
            final int steps = wrapper.read(Types.VAR_INT);
            double x = 0;
            double y = 0;
            double z = 0;
            double previousX = 0;
            double previousY = 0;
            double previousZ = 0;
            int tickOffset = 0;
            for (int i = 0; i < steps; i++) {
                previousX = x;
                previousY = y;
                previousZ = z;
                x = wrapper.read(Types.DOUBLE);
                y = wrapper.read(Types.DOUBLE);
                z = wrapper.read(Types.DOUBLE);
                tickOffset = wrapper.read(Types.VAR_INT);
            }

            wrapper.write(Types.DOUBLE, x);
            wrapper.write(Types.DOUBLE, y);
            wrapper.write(Types.DOUBLE, z);
            if (steps > 1 && tickOffset > 0) {
                wrapper.write(Types.DOUBLE, (x - previousX) / tickOffset); // Delta x
                wrapper.write(Types.DOUBLE, (y - previousY) / tickOffset); // Delta y
                wrapper.write(Types.DOUBLE, (z - previousZ) / tickOffset); // Delta z
            } else {
                wrapper.write(Types.DOUBLE, 0D); // Delta x
                wrapper.write(Types.DOUBLE, 0D); // Delta y
                wrapper.write(Types.DOUBLE, 0D); // Delta z
            }
        });

        protocol.appendClientbound(ClientboundPackets26_3.LOGIN, wrapper -> {
            wrapper.rewindReader(1);
            handleGamemodes(wrapper);
        });
        protocol.appendClientbound(ClientboundPackets26_3.RESPAWN, wrapper -> {
            wrapper.rewindReader(1);
            handleGamemodes(wrapper);
        });

        protocol.registerServerbound(ServerboundPackets26_1.SWING, ServerboundPackets26_3.PUNCH, wrapper -> {
            final int hand = wrapper.read(Types.VAR_INT);
            if (hand == 1) { // Offhand, although not every arm swing is a punch either...
                wrapper.cancel();
            }
        });
        protocol.registerServerbound(ServerboundPackets26_1.PLAYER_ACTION, wrapper -> {
            final int action = wrapper.read(Types.VAR_INT);
            wrapper.write(Types.VAR_INT, action >= CHANGE_DESTROY_DIRECTION_ACTION ? action + 1 : action);
        });
        protocol.registerClientbound(ClientboundPackets26_3.SWING_ANIMATION, ClientboundPackets26_1.ANIMATE, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity ID

            final int hand = wrapper.read(Types.VAR_INT);
            final SwingAnimation animation = wrapper.read(SwingAnimation.TYPE);
            if (animation.type() == NO_SWING_ANIMATION) {
                wrapper.cancel();
                return;
            }

            // Type and duration go bye bye, just map to the correct arm swing given what the client is currently using
            wrapper.write(Types.UNSIGNED_BYTE, (short) (hand == 0 ? 0 : 3));
        });

        protocol.registerServerbound(ServerboundPackets26_1.ACCEPT_TELEPORTATION, wrapper -> {
            final int id = wrapper.passthrough(Types.VAR_INT);

            final ProtocolStorables26_3 storables = wrapper.user().storables(protocol);
            storables.setCurrentTeleportId(id);

            // Send once the pos_rot followup arrives, which the client should always send right after
            wrapper.cancel();
        });
        protocol.registerServerbound(ServerboundPackets26_1.MOVE_PLAYER_POS_ROT, wrapper -> {
            final ProtocolStorables26_3 storables = wrapper.user().storables(protocol);
            if (storables.currentTeleportId() != null) {
                wrapper.setPacketType(ServerboundPackets26_3.ACCEPT_TELEPORTATION);
                wrapper.write(Types.VAR_INT, storables.currentTeleportId());
                wrapper.passthrough(Types.DOUBLE); // X
                wrapper.passthrough(Types.DOUBLE); // Y
                wrapper.passthrough(Types.DOUBLE); // Z
                wrapper.passthrough(Types.FLOAT); // Y Rot
                wrapper.passthrough(Types.FLOAT); // X Rot
                wrapper.read(Types.UNSIGNED_BYTE); // Data
                storables.setCurrentTeleportId(null);
            }
        });

        protocol.registerClientbound(ClientboundPackets26_3.ANIMATE, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity ID
            final short action = wrapper.read(Types.UNSIGNED_BYTE);
            wrapper.write(Types.UNSIGNED_BYTE, switch (action) {
                case 0 -> (short) 2; // Wake up
                case 1 -> (short) 4; // Crit
                case 2 -> (short) 5; // Magic crit
                default -> action;
            });
        });

        protocol.registerServerbound(ServerboundPackets26_1.SPECTATE_ENTITY, ServerboundPackets26_3.SPECTATOR_ACTION);

        protocol.appendClientbound(ClientboundPackets26_3.ADD_ENTITY, wrapper -> {
            final int entityId = wrapper.get(Types.VAR_INT, 0);
            final TrackedEntity entity = tracker(wrapper.user()).entity(entityId);
            if (entity == null || entity.entityType() != EntityTypes26_3.CUSHION) {
                return;
            }

            // Set falling block data to white carpet block state
            wrapper.set(Types.VAR_INT, 2, WHITE_CARPET_BLOCK_STATE);

            wrapper.send(Protocol26_3To26_2.class);
            wrapper.cancel();

            // Send no gravity entity data
            final List<EntityData> entityDataList = new ArrayList<>();
            entityDataList.add(new EntityData(5, MAPPED_DATA_TYPES.booleanType, true)); // No gravity

            final PacketWrapper entityDataPacket = wrapper.create(ClientboundPackets26_1.SET_ENTITY_DATA);
            entityDataPacket.write(Types.VAR_INT, entityId);
            entityDataPacket.write(VersionedTypes.V26_2.entityDataList, entityDataList);
            entityDataPacket.send(Protocol26_3To26_2.class);
        });
    }

    private void handleGamemodes(final PacketWrapper wrapper) {
        final int gamemode = wrapper.read(Types.VAR_INT);
        wrapper.write(Types.BYTE, (byte) gamemode);

        final Integer previousGamemode = wrapper.read(Types.OPTIONAL_VAR_INT);
        wrapper.write(Types.BYTE, previousGamemode == null ? -1 : previousGamemode.byteValue());
    }

    private void handleMovePos(final PacketWrapper wrapper, final int properties) {
        final int steps = properties >>> 1;
        if (steps == 0) {
            wrapper.passthrough(Types.SHORT); // Delta x
            wrapper.passthrough(Types.SHORT); // Delta y
            wrapper.passthrough(Types.SHORT); // Delta z
            return;
        }

        // Sum each delta position. Old clients do their own fixed-length lerping
        int deltaX = 0;
        int deltaY = 0;
        int deltaZ = 0;
        for (int i = 0; i < steps; i++) {
            wrapper.read(Types.VAR_INT); // Tick delay
            deltaX += wrapper.read(Types.SHORT);
            deltaY += wrapper.read(Types.SHORT);
            deltaZ += wrapper.read(Types.SHORT);
        }

        wrapper.write(Types.SHORT, (short) deltaX);
        wrapper.write(Types.SHORT, (short) deltaY);
        wrapper.write(Types.SHORT, (short) deltaZ);
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
        mapEntityTypeWithData(EntityTypes26_3.CUSHION, EntityTypes26_3.FALLING_BLOCK).tagName();
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes26_3.getTypeFromId(type);
    }
}
