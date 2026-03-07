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
package com.viaversion.viabackwards.protocol.v26_1to1_21_11.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v26_1to1_21_11.Protocol26_1To1_21_11;
import com.viaversion.viaversion.api.minecraft.Vector3d;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_11;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_21_11;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes26_1;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPacket26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPackets26_1;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ServerboundPackets1_21_6;

public final class EntityPacketRewriter26_1 extends EntityRewriter<ClientboundPacket26_1, Protocol26_1To1_21_11> {

    private static final int INTERACT_ACTION = 0;
    private static final int ATTACK_ACTION = 1;
    private static final int INTERACT_AT_ACTION = 2;

    public EntityPacketRewriter26_1(final Protocol26_1To1_21_11 protocol) {
        super(protocol, VersionedTypes.V1_21_11.entityDataTypes.optionalComponentType, VersionedTypes.V1_21_11.entityDataTypes.booleanType);
    }

    @Override
    public void registerPackets() {
        registerTrackerWithData1_21_9(ClientboundPackets26_1.ADD_ENTITY, EntityTypes1_21_11.FALLING_BLOCK);
        registerSetEntityData(ClientboundPackets26_1.SET_ENTITY_DATA);
        registerRemoveEntities(ClientboundPackets26_1.REMOVE_ENTITIES);
        registerPlayerAbilities(ClientboundPackets26_1.PLAYER_ABILITIES);
        registerGameEvent(ClientboundPackets26_1.GAME_EVENT);
        registerLogin1_20_5(ClientboundPackets26_1.LOGIN);
        registerRespawn1_20_5(ClientboundPackets26_1.RESPAWN);

        protocol.registerServerbound(ServerboundPackets1_21_6.INTERACT, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity ID
            final int action = wrapper.read(Types.VAR_INT);
            switch (action) {
                case INTERACT_ACTION -> {
                    wrapper.passthrough(Types.VAR_INT); // Hand
                    wrapper.write(Types.LOW_PRECISION_VECTOR, Vector3d.ZERO); // Unused
                    // Keep secondary action
                }
                case ATTACK_ACTION -> {
                    wrapper.setPacketType(ServerboundPackets26_1.ATTACK);
                    wrapper.read(Types.BOOLEAN); // Using secondary action
                }
                case INTERACT_AT_ACTION -> {
                    final float x = wrapper.read(Types.FLOAT);
                    final float y = wrapper.read(Types.FLOAT);
                    final float z = wrapper.read(Types.FLOAT);
                    wrapper.passthrough(Types.VAR_INT); // Hand
                    wrapper.write(Types.LOW_PRECISION_VECTOR, new Vector3d(x, y, z));
                    // Keep secondary action
                }
                default -> throw new IllegalArgumentException("Invalid interact action");
            }
        });
    }

    @Override
    protected void registerRewrites() {
        final EntityDataTypes26_1 entityDataTypes = VersionedTypes.V26_1.entityDataTypes;
        final EntityDataTypes1_21_11 mappedEntityDataTypes = VersionedTypes.V1_21_11.entityDataTypes;
        filter().handler((event, data) -> {
            final int id = data.dataType().typeId();
            int mappedId = id;
            if (id == entityDataTypes.catSoundVariant.typeId()
                || id == entityDataTypes.cowSoundVariant.typeId()
                || id == entityDataTypes.pigSoundVariant.typeId()
                || id == entityDataTypes.chickenSoundVariant.typeId()) {
                event.cancel();
                return;
            } else if (id > entityDataTypes.chickenSoundVariant.typeId()) {
                mappedId -= 4;
            } else if (id > entityDataTypes.pigSoundVariant.typeId()) {
                mappedId -= 3;
            } else if (id > entityDataTypes.cowSoundVariant.typeId()) {
                mappedId -= 2;
            } else if (id > entityDataTypes.catSoundVariant.typeId()) {
                mappedId -= 1;
            }
            data.setDataType(mappedEntityDataTypes.byId(mappedId));
        });
        registerEntityDataTypeHandler1_20_3(
            mappedEntityDataTypes.itemType,
            mappedEntityDataTypes.blockStateType,
            mappedEntityDataTypes.optionalBlockStateType,
            mappedEntityDataTypes.particleType,
            mappedEntityDataTypes.particlesType,
            mappedEntityDataTypes.componentType,
            mappedEntityDataTypes.optionalComponentType
        );

        filter().type(EntityTypes1_21_11.ZOMBIE_VILLAGER).removeIndex(21); // Is villager data finalized
        filter().type(EntityTypes1_21_11.VILLAGER).removeIndex(20); // Is villager data finalized
        filter().type(EntityTypes1_21_11.CAT).removeIndex(24); // Sound variant
        filter().type(EntityTypes1_21_11.CHICKEN).removeIndex(19); // Sound variant
        filter().type(EntityTypes1_21_11.PIG).removeIndex(20); // Sound variant
        filter().type(EntityTypes1_21_11.COW).removeIndex(19); // Sound variant
        filter().type(EntityTypes1_21_11.TADPOLE).removeIndex(17); // Age locked
        filter().type(EntityTypes1_21_11.ABSTRACT_AGEABLE).removeIndex(17); // Age locked
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_21_11.getTypeFromId(type);
    }
}
