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
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.Protocol1_21_9To1_21_7;
import com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.storage.MannequinData;
import com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.storage.PlayerRotationStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.GameProfile;
import com.viaversion.viaversion.api.minecraft.GlobalBlockPosition;
import com.viaversion.viaversion.api.minecraft.ResolvableProfile;
import com.viaversion.viaversion.api.minecraft.Vector3d;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_6;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_9;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_21_5;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ClientboundPackets1_21_6;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ServerboundPackets1_21_6;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundPacket1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundPackets1_21_9;
import com.viaversion.viaversion.rewriter.entitydata.EntityDataHandler;
import com.viaversion.viaversion.util.ChatColorUtil;
import java.util.BitSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.checkerframework.checker.nullness.qual.Nullable;

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
            final UUID uuid = wrapper.passthrough(Types.UUID);
            final int entityTypeId = wrapper.passthrough(Types.VAR_INT);

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

            if (EntityTypes1_21_9.getTypeFromId(entityTypeId) == EntityTypes1_21_9.MANNEQUIN) {
                final String name = randomHackyEmptyName();
                final MannequinData mannequinData = new MannequinData(uuid, name);
                tracker(wrapper.user()).entity(entityId).data().put(mannequinData);
                sendInitialPlayerInfoUpdate(wrapper, mannequinData);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_21_9.SET_ENTITY_MOTION, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity ID
            writeMovementShorts(wrapper, wrapper.read(Types.MOVEMENT_VECTOR));
        });

        protocol.registerClientbound(ClientboundPackets1_21_9.PLAYER_ROTATION, wrapper -> {
            final PlayerRotationStorage storage = wrapper.user().get(PlayerRotationStorage.class);

            float yRot = wrapper.read(Types.FLOAT);
            if (wrapper.read(Types.BOOLEAN)) {
                yRot = storage.yaw() + yRot;
            }

            float xRot = wrapper.read(Types.FLOAT);
            if (wrapper.read(Types.BOOLEAN)) {
                xRot = storage.pitch() + xRot;
            }

            wrapper.write(Types.FLOAT, yRot);
            wrapper.write(Types.FLOAT, xRot);
            storage.setRotation(yRot, xRot); // Update after having used its previous data
        });

        protocol.registerClientbound(ClientboundPackets1_21_9.SET_DEFAULT_SPAWN_POSITION, wrapper -> {
            final GlobalBlockPosition pos = wrapper.read(Types.GLOBAL_POSITION);
            wrapper.write(Types.BLOCK_POSITION1_14, new BlockPosition(pos.x(), pos.y(), pos.z()));
            wrapper.passthrough(Types.FLOAT); // Yaw
            wrapper.read(Types.FLOAT); // Pitch
        });

        protocol.registerServerbound(ServerboundPackets1_21_6.MOVE_PLAYER_POS_ROT, wrapper -> {
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z

            storePlayerRotation(wrapper);
        });

        protocol.registerServerbound(ServerboundPackets1_21_6.MOVE_PLAYER_ROT, this::storePlayerRotation);
    }

    private void sendInitialPlayerInfoUpdate(final PacketWrapper wrapper, final MannequinData mannequinData) {
        final PacketWrapper playerInfo = wrapper.create(ClientboundPackets1_21_6.PLAYER_INFO_UPDATE);

        final BitSet actions = new BitSet(8);
        for (int i = 0; i < 8; i++) {
            actions.set(i);
        }
        playerInfo.write(Types.PROFILE_ACTIONS_ENUM1_21_4, actions);
        playerInfo.write(Types.VAR_INT, 1); // One entry
        playerInfo.write(Types.UUID, mannequinData.uuid());
        playerInfo.write(Types.STRING, mannequinData.name());
        playerInfo.write(Types.PROFILE_PROPERTY_ARRAY, new GameProfile.Property[0]);
        playerInfo.write(Types.BOOLEAN, false); // Session info
        playerInfo.write(Types.VAR_INT, 0); // Gamemode
        playerInfo.write(Types.BOOLEAN, false); // Listed
        playerInfo.write(Types.VAR_INT, 0); // Latency
        playerInfo.write(Types.OPTIONAL_TAG, null);
        playerInfo.write(Types.VAR_INT, 1000); // List order
        playerInfo.write(Types.BOOLEAN, true); // Show hat
        playerInfo.send(Protocol1_21_9To1_21_7.class);

        sendPlayerTeamDisplayName(wrapper.user(), mannequinData, null);
    }

    private void sendPlayerInfoDisplayNameUpdate(final UserConnection connection, final MannequinData mannequinData, @Nullable final Tag displayName) {
        final PacketWrapper playerInfo = PacketWrapper.create(ClientboundPackets1_21_6.PLAYER_INFO_UPDATE, connection);
        final BitSet actions = new BitSet(8);
        actions.set(5);
        playerInfo.write(Types.PROFILE_ACTIONS_ENUM1_21_4, actions);
        playerInfo.write(Types.VAR_INT, 1);
        playerInfo.write(Types.UUID, mannequinData.uuid());
        playerInfo.write(Types.OPTIONAL_TAG, displayName);
        playerInfo.send(Protocol1_21_9To1_21_7.class);

        sendPlayerTeamDisplayName(connection, mannequinData, displayName);
    }

    private void sendPlayerTeamDisplayName(final UserConnection connection, final MannequinData mannequinData, final Tag displayName) {
        // Send the display name as a team prefix
        final Tag nonNullDisplayName = displayName != null ? displayName : new StringTag("Mannequin");
        final PacketWrapper addTeam = PacketWrapper.create(ClientboundPackets1_21_6.SET_PLAYER_TEAM, connection);
        addTeam.write(Types.STRING, mannequinData.name());
        addTeam.write(Types.BYTE, mannequinData.hasTeam() ? (byte) 2 : 0); // Mode
        addTeam.write(Types.TAG, nonNullDisplayName); // Display Name
        addTeam.write(Types.BYTE, (byte) 0); // Flags
        addTeam.write(Types.VAR_INT, 0); // Nametag visibility
        addTeam.write(Types.VAR_INT, 0); // Collision rule
        addTeam.write(Types.VAR_INT, 15); // Color
        addTeam.write(Types.TAG, nonNullDisplayName); // Prefix
        addTeam.write(Types.TAG, new StringTag("")); // Suffix
        if (!mannequinData.hasTeam()) {
            addTeam.write(Types.STRING_ARRAY, new String[]{mannequinData.name()});
        }
        addTeam.send(Protocol1_21_9To1_21_7.class);
    }

    private void sendPlayerInfoProfileUpdate(final UserConnection connection, final UUID uuid, @Nullable final String name, final GameProfile.Property[] properties) {
        final PacketWrapper playerInfo = PacketWrapper.create(ClientboundPackets1_21_6.PLAYER_INFO_UPDATE, connection);
        final BitSet actions = new BitSet(8);
        actions.set(0);
        playerInfo.write(Types.PROFILE_ACTIONS_ENUM1_21_4, actions);
        playerInfo.write(Types.VAR_INT, 1);
        playerInfo.write(Types.UUID, uuid);
        playerInfo.write(Types.STRING, name != null ? name : randomHackyEmptyName());
        playerInfo.write(Types.PROFILE_PROPERTY_ARRAY, properties);
        playerInfo.send(Protocol1_21_9To1_21_7.class);
    }

    private String randomHackyEmptyName() {
        final StringBuilder builder = new StringBuilder();
        // Player names cannot be updated after the initial add without fully respawning them;
        // Stack random color codes to appear as an empty name, later filled with a team prefix
        for (int i = 0; i < 8; i++) {
            final int random = ThreadLocalRandom.current().nextInt(ChatColorUtil.ALL_CODES.length());
            builder.append('§').append(ChatColorUtil.ALL_CODES.charAt(random));
        }
        return builder.toString();
    }

    private void writeMovementShorts(final PacketWrapper wrapper, final Vector3d movement) {
        wrapper.write(Types.SHORT, (short) (movement.x() * 8000));
        wrapper.write(Types.SHORT, (short) (movement.y() * 8000));
        wrapper.write(Types.SHORT, (short) (movement.z() * 8000));
    }

    private void storePlayerRotation(final PacketWrapper wrapper) {
        final float yaw = wrapper.passthrough(Types.FLOAT);
        final float pitch = wrapper.passthrough(Types.FLOAT);

        wrapper.user().get(PlayerRotationStorage.class).setRotation(yaw, pitch);
    }

    @Override
    protected void registerRewrites() {
        final EntityDataTypes1_21_5 entityDataTypes = protocol.mappedTypes().entityDataTypes();
        filter().handler((event, data) -> {
            int id = data.dataType().typeId();
            if (id == VersionedTypes.V1_21_9.entityDataTypes.copperGolemState.typeId()
                || id == VersionedTypes.V1_21_9.entityDataTypes.weatheringCopperState.typeId()) {
                event.cancel();
                return;
            }
            if (id == VersionedTypes.V1_21_9.entityDataTypes.mannequinProfileType.typeId()) {
                if (event.entityType() == null) {
                    event.cancel();
                }
                return; // Handled separately
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
            final CompoundTag entityTag = new CompoundTag();
            final Integer value = data.value();
            if (value != null) {
                entityTag.putInt("id", EntityTypes1_21_6.PARROT.getId());
                entityTag.putInt("Variant", value);
            }
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

        filter().type(EntityTypes1_21_9.MANNEQUIN).handler(((event, data) -> {
            if (event.index() == 2) { // Display name
                final Tag displayName = data.value();
                final MannequinData mannequinData = event.trackedEntity().data().get(MannequinData.class);
                sendPlayerInfoDisplayNameUpdate(event.user(), mannequinData, displayName);
            } else if (event.index() == 17) { // Profile
                final ResolvableProfile profile = data.value();
                final UUID uuid = event.trackedEntity().data().get(MannequinData.class).uuid();
                sendPlayerInfoProfileUpdate(event.user(), uuid, profile.profile().name(), profile.profile().properties());
                event.cancel();
            } else if (event.index() == 15) {
                event.setIndex(18);
            } else if (event.index() == 16) {
                event.setIndex(17);
            } else if (event.index() == 18) {
                // TODO Immovable?
                event.cancel();
            } else if (event.index() == 19) {
                event.cancel(); // Description
            }
        }));
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();
        mapEntityTypeWithData(EntityTypes1_21_9.COPPER_GOLEM, EntityTypes1_21_9.FROG).tagName();
        mapEntityTypeWithData(EntityTypes1_21_9.MANNEQUIN, EntityTypes1_21_9.PLAYER);
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_21_9.getTypeFromId(type);
    }
}
