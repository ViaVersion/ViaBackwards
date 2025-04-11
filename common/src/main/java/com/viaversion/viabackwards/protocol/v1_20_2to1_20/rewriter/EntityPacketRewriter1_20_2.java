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
package com.viaversion.viabackwards.protocol.v1_20_2to1_20.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_20_2to1_20.Protocol1_20_2To1_20;
import com.viaversion.viabackwards.protocol.v1_20_2to1_20.storage.ConfigurationPacketStorage;
import com.viaversion.viaversion.api.minecraft.GlobalBlockPosition;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19_4;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_20;
import com.viaversion.viaversion.api.type.types.version.Types1_20_2;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ClientboundPackets1_19_4;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ClientboundPackets1_20_2;

public final class EntityPacketRewriter1_20_2 extends EntityRewriter<ClientboundPackets1_20_2, Protocol1_20_2To1_20> {

    public EntityPacketRewriter1_20_2(final Protocol1_20_2To1_20 protocol) {
        super(protocol, Types1_20.ENTITY_DATA_TYPES.optionalComponentType, Types1_20.ENTITY_DATA_TYPES.booleanType);
    }

    @Override
    public void registerPackets() {
        registerSetEntityData(ClientboundPackets1_20_2.SET_ENTITY_DATA, Types1_20_2.ENTITY_DATA_LIST, Types1_20.ENTITY_DATA_LIST);
        registerRemoveEntities(ClientboundPackets1_20_2.REMOVE_ENTITIES);

        protocol.registerClientbound(ClientboundPackets1_20_2.ADD_ENTITY, new PacketHandlers() {
            @Override
            protected void register() {
                handler(wrapper -> {
                    final int entityId = wrapper.passthrough(Types.VAR_INT);

                    wrapper.passthrough(Types.UUID); // UUID

                    final int entityType = wrapper.read(Types.VAR_INT);
                    tracker(wrapper.user()).addEntity(entityId, typeFromId(entityType));

                    if (entityType != EntityTypes1_19_4.PLAYER.getId()) {
                        wrapper.write(Types.VAR_INT, entityType);

                        if (entityType == EntityTypes1_19_4.FALLING_BLOCK.getId()) {
                            wrapper.passthrough(Types.DOUBLE); // X
                            wrapper.passthrough(Types.DOUBLE); // Y
                            wrapper.passthrough(Types.DOUBLE); // Z
                            wrapper.passthrough(Types.BYTE); // Pitch
                            wrapper.passthrough(Types.BYTE); // Yaw
                            wrapper.passthrough(Types.BYTE); // Head yaw
                            final int blockState = wrapper.read(Types.VAR_INT); // Data
                            wrapper.write(Types.VAR_INT, protocol.getMappingData().getNewBlockStateId(blockState));
                        }
                        return;
                    }

                    // Map to spawn player packet
                    wrapper.setPacketType(ClientboundPackets1_19_4.ADD_PLAYER);

                    wrapper.passthrough(Types.DOUBLE); // X
                    wrapper.passthrough(Types.DOUBLE); // Y
                    wrapper.passthrough(Types.DOUBLE); // Z

                    final byte pitch = wrapper.read(Types.BYTE);
                    wrapper.passthrough(Types.BYTE); // Yaw
                    wrapper.write(Types.BYTE, pitch);
                    wrapper.read(Types.BYTE); // Head yaw
                    wrapper.read(Types.VAR_INT); // Data

                    final short velocityX = wrapper.read(Types.SHORT);
                    final short velocityY = wrapper.read(Types.SHORT);
                    final short velocityZ = wrapper.read(Types.SHORT);
                    if (velocityX == 0 && velocityY == 0 && velocityZ == 0) {
                        return;
                    }

                    // Follow up with velocity packet
                    wrapper.send(Protocol1_20_2To1_20.class);
                    wrapper.cancel();

                    final PacketWrapper velocityPacket = wrapper.create(ClientboundPackets1_19_4.SET_ENTITY_MOTION);
                    velocityPacket.write(Types.VAR_INT, entityId);
                    velocityPacket.write(Types.SHORT, velocityX);
                    velocityPacket.write(Types.SHORT, velocityY);
                    velocityPacket.write(Types.SHORT, velocityZ);
                    velocityPacket.send(Protocol1_20_2To1_20.class);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_2.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                handler(wrapper -> {
                    final ConfigurationPacketStorage configurationPacketStorage = wrapper.user().get(ConfigurationPacketStorage.class);
                    wrapper.passthrough(Types.INT); // Entity id
                    wrapper.passthrough(Types.BOOLEAN); // Hardcore

                    final String[] worlds = wrapper.read(Types.STRING_ARRAY);
                    final int maxPlayers = wrapper.read(Types.VAR_INT);
                    final int viewDistance = wrapper.read(Types.VAR_INT);
                    final int simulationDistance = wrapper.read(Types.VAR_INT);
                    final boolean reducedDebugInfo = wrapper.read(Types.BOOLEAN);
                    final boolean showRespawnScreen = wrapper.read(Types.BOOLEAN);

                    wrapper.read(Types.BOOLEAN); // Limited crafting

                    final String dimensionType = wrapper.read(Types.STRING);
                    final String world = wrapper.read(Types.STRING);
                    final long seed = wrapper.read(Types.LONG);

                    wrapper.passthrough(Types.BYTE); // Gamemode
                    wrapper.passthrough(Types.BYTE); // Previous gamemode

                    wrapper.write(Types.STRING_ARRAY, worlds);
                    wrapper.write(Types.NAMED_COMPOUND_TAG, configurationPacketStorage.registry());
                    wrapper.write(Types.STRING, dimensionType);
                    wrapper.write(Types.STRING, world);
                    wrapper.write(Types.LONG, seed);
                    wrapper.write(Types.VAR_INT, maxPlayers);
                    wrapper.write(Types.VAR_INT, viewDistance);
                    wrapper.write(Types.VAR_INT, simulationDistance);
                    wrapper.write(Types.BOOLEAN, reducedDebugInfo);
                    wrapper.write(Types.BOOLEAN, showRespawnScreen);

                    worldDataTrackerHandlerByKey().handle(wrapper);

                    wrapper.send(Protocol1_20_2To1_20.class);
                    wrapper.cancel();

                    if (configurationPacketStorage.enabledFeatures() != null) {
                        final PacketWrapper featuresPacket = wrapper.create(ClientboundPackets1_19_4.UPDATE_ENABLED_FEATURES);
                        featuresPacket.write(Types.STRING_ARRAY, configurationPacketStorage.enabledFeatures());
                        featuresPacket.send(Protocol1_20_2To1_20.class);
                    }

                    configurationPacketStorage.sendQueuedPackets(wrapper.user());
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_2.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                handler(wrapper -> {
                    wrapper.passthrough(Types.STRING); // Dimension type
                    wrapper.passthrough(Types.STRING); // World
                    wrapper.passthrough(Types.LONG); // Seed
                    wrapper.write(Types.UNSIGNED_BYTE, wrapper.read(Types.BYTE).shortValue()); // Gamemode
                    wrapper.passthrough(Types.BYTE); // Previous gamemode
                    wrapper.passthrough(Types.BOOLEAN); // Debug
                    wrapper.passthrough(Types.BOOLEAN); // Flat

                    final GlobalBlockPosition lastDeathPosition = wrapper.read(Types.OPTIONAL_GLOBAL_POSITION);
                    final int portalCooldown = wrapper.read(Types.VAR_INT);

                    wrapper.passthrough(Types.BYTE); // Data to keep

                    wrapper.write(Types.OPTIONAL_GLOBAL_POSITION, lastDeathPosition);
                    wrapper.write(Types.VAR_INT, portalCooldown);
                });
                handler(worldDataTrackerHandlerByKey()); // Tracks world height and name for chunk data and entity (un)tracking
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_2.UPDATE_MOB_EFFECT, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity id
            wrapper.write(Types.VAR_INT, wrapper.read(Types.VAR_INT) + 1); // Effect id
            wrapper.passthrough(Types.BYTE); // Amplifier
            wrapper.passthrough(Types.VAR_INT); // Duration
            wrapper.passthrough(Types.BYTE); // Flags

            final CompoundTag factorData = wrapper.read(Types.OPTIONAL_COMPOUND_TAG);
            wrapper.write(Types.OPTIONAL_NAMED_COMPOUND_TAG, factorData); // Factor data
        });

        protocol.registerClientbound(ClientboundPackets1_20_2.REMOVE_MOB_EFFECT, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity id
            wrapper.write(Types.VAR_INT, wrapper.read(Types.VAR_INT) + 1); // Effect id
        });
    }

    @Override
    protected void registerRewrites() {
        filter().mapDataType(Types1_20.ENTITY_DATA_TYPES::byId);
        registerEntityDataTypeHandler(Types1_20.ENTITY_DATA_TYPES.itemType, Types1_20.ENTITY_DATA_TYPES.blockStateType, Types1_20.ENTITY_DATA_TYPES.optionalBlockStateType, Types1_20.ENTITY_DATA_TYPES.particleType, null, null);
        registerBlockStateHandler(EntityTypes1_19_4.ABSTRACT_MINECART, 11);

        filter().type(EntityTypes1_19_4.DISPLAY).removeIndex(10);
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_19_4.getTypeFromId(type);
    }
}
