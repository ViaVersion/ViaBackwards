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
package com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viabackwards.api.rewriters.BackwardsRegistryRewriter;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.Protocol1_21_9To1_21_7;
import com.viaversion.viaversion.api.minecraft.Vector3d;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_6;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_9;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_21_5;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundPacket1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundPackets1_21_9;
import com.viaversion.viaversion.rewriter.RegistryDataRewriter;
import com.viaversion.viaversion.rewriter.entitydata.EntityDataHandler;

public final class EntityPacketRewriter1_21_9 extends EntityRewriter<ClientboundPacket1_21_9, Protocol1_21_9To1_21_7> {

    public EntityPacketRewriter1_21_9(final Protocol1_21_9To1_21_7 protocol) {
        super(protocol, VersionedTypes.V1_21_9.entityDataTypes.optionalComponentType, VersionedTypes.V1_21_9.entityDataTypes.booleanType);
    }

    @Override
    public void registerPackets() {
        registerSetEntityData(ClientboundPackets1_21_9.SET_ENTITY_DATA);
        registerRemoveEntities(ClientboundPackets1_21_9.REMOVE_ENTITIES);
        registerPlayerAbilities(ClientboundPackets1_21_9.PLAYER_ABILITIES);
        registerGameEvent(ClientboundPackets1_21_9.GAME_EVENT);
        registerLogin1_20_5(ClientboundPackets1_21_9.LOGIN);
        registerRespawn1_20_5(ClientboundPackets1_21_9.RESPAWN);

        protocol.registerClientbound(ClientboundPackets1_21_9.ADD_ENTITY, wrapper -> {
            final int entityId = wrapper.passthrough(Types.VAR_INT);
            wrapper.passthrough(Types.UUID); // Entity UUID
            final int entityTypeId = wrapper.passthrough(Types.VAR_INT);
            if (EntityTypes1_21_9.getTypeFromId(entityTypeId) == EntityTypes1_21_9.MANNEQUIN) {
                // TODO WIP
                wrapper.cancel();
                return;
            }

            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z

            final Vector3d movement = wrapper.read(Types.MOVEMENT_VECTOR);

            wrapper.passthrough(Types.BYTE); // Pitch
            wrapper.passthrough(Types.BYTE); // Yaw
            wrapper.passthrough(Types.BYTE); // Head yaw
            final int data = wrapper.passthrough(Types.VAR_INT);
            final EntityType entityType = trackAndRewrite(wrapper, entityTypeId, entityId);
            if (protocol.getMappingData() != null && entityType == EntityTypes1_21_9.FALLING_BLOCK) {
                final int mappedBlockStateId = protocol.getMappingData().getNewBlockStateId(data);
                wrapper.set(Types.VAR_INT, 2, mappedBlockStateId);
            }

            writeMovementShorts(wrapper, movement);
        });

        protocol.registerClientbound(ClientboundPackets1_21_9.SET_ENTITY_MOTION, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity ID
            writeMovementShorts(wrapper, wrapper.read(Types.MOVEMENT_VECTOR));
        });

        protocol.registerClientbound(ClientboundPackets1_21_9.PLAYER_ROTATION, wrapper -> {
            // TODO track
            wrapper.passthrough(Types.FLOAT); // Y rotation
            final boolean relativeY = wrapper.read(Types.BOOLEAN);
            wrapper.passthrough(Types.FLOAT); // X rotation
            final boolean relativeX = wrapper.read(Types.BOOLEAN);
        });

        final RegistryDataRewriter registryDataRewriter = new BackwardsRegistryRewriter(protocol);
        protocol.registerClientbound(ClientboundConfigurationPackets1_21_9.REGISTRY_DATA, registryDataRewriter::handle);
    }

    private void writeMovementShorts(final PacketWrapper wrapper, final Vector3d movement) {
        wrapper.write(Types.SHORT, (short) (movement.x() * 8000));
        wrapper.write(Types.SHORT, (short) (movement.y() * 8000));
        wrapper.write(Types.SHORT, (short) (movement.z() * 8000));
    }

    @Override
    protected void registerRewrites() {
        final EntityDataTypes1_21_5 entityDataTypes = protocol.mappedTypes().entityDataTypes();
        filter().handler((event, data) -> {
            int id = data.dataType().typeId();
            if (id == VersionedTypes.V1_21_9.entityDataTypes.copperGolemState.typeId()
                || id == VersionedTypes.V1_21_9.entityDataTypes.weatheringCopperState.typeId()
                || id == VersionedTypes.V1_21_9.entityDataTypes.mannequinProfileType.typeId()) {
                event.cancel();
                return;
            }
            if (id > VersionedTypes.V1_21_9.entityDataTypes.armadilloState.typeId()) {
                id -= 2;
            }
            if (id >= entityDataTypes.compoundTagType.typeId()) {
                id++;
            }
            data.setDataType(entityDataTypes.byId(id));
        });

        registerEntityDataTypeHandler1_20_3(
            entityDataTypes.itemType,
            entityDataTypes.blockStateType,
            entityDataTypes.optionalBlockStateType,
            entityDataTypes.particleType,
            entityDataTypes.particlesType,
            entityDataTypes.componentType,
            entityDataTypes.optionalComponentType
        );

        final EntityDataHandler shoulderDataHandler = (event, data) -> {
            final Integer value = data.value();
            if (value == null) {
                data.setTypeAndValue(protocol.mappedTypes().entityDataTypes.compoundTagType, null);
                return;
            }

            final CompoundTag entityTag = new CompoundTag();
            entityTag.putInt("id", EntityTypes1_21_6.PARROT.getId());
            entityTag.putInt("Variant", value);
            data.setTypeAndValue(protocol.mappedTypes().entityDataTypes.compoundTagType, entityTag);
        };
        filter().type(EntityTypes1_21_9.PLAYER).index(19).handler(shoulderDataHandler);
        filter().type(EntityTypes1_21_9.PLAYER).index(20).handler(shoulderDataHandler);
        filter().type(EntityTypes1_21_9.PLAYER).handler((event, data) -> {
            if (event.index() == 15) {
                event.setIndex(18);
            } else if (event.index() == 16) {
                event.setIndex(17);
            } else if (event.index() == 17 || event.index() == 18) {
                event.setIndex(event.index() - 2); // Move hearts and score back down
            }
        });
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();
        mapEntityTypeWithData(EntityTypes1_21_9.COPPER_GOLEM, EntityTypes1_21_9.FROG).tagName();
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_21_9.getTypeFromId(type);
    }
}
