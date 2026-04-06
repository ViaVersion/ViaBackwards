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
import com.viaversion.viabackwards.protocol.v26_1to1_21_11.storage.GameModeStorage;
import com.viaversion.viaversion.api.minecraft.GameMode;
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
        protocol.appendClientbound(ClientboundPackets26_1.RESPAWN, wrapper -> {
            final byte gamemode = wrapper.get(Types.BYTE, 0);
            wrapper.user().get(GameModeStorage.class).setGameMode(gamemode);
        });
        protocol.appendClientbound(ClientboundPackets26_1.LOGIN, wrapper -> {
            final byte gamemode = wrapper.get(Types.BYTE, 0);
            wrapper.user().get(GameModeStorage.class).setGameMode(gamemode);
        });

        protocol.registerServerbound(ServerboundPackets1_21_6.INTERACT, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity ID
            final int action = wrapper.read(Types.VAR_INT);
            switch (action) {
                case INTERACT_ACTION -> wrapper.cancel(); // Drop "normal" interacts, as interact_at is always sent by Vanilla clients, and always sent first, with this following after
                case ATTACK_ACTION -> {
                    final boolean spectator = wrapper.user().get(GameModeStorage.class).gameMode() == GameMode.SPECTATOR.id();
                    wrapper.setPacketType(spectator ? ServerboundPackets26_1.SPECTATE_ENTITY : ServerboundPackets26_1.ATTACK);
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
        dataTypeMapper()
            .removed(entityDataTypes.catSoundVariant)
            .removed(entityDataTypes.cowSoundVariant)
            .removed(entityDataTypes.pigSoundVariant)
            .removed(entityDataTypes.chickenSoundVariant)
            .register();
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
    public EntityType typeFromId(final int type) {
        return EntityTypes1_21_11.getTypeFromId(type);
    }
}
