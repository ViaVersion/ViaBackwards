/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2024 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_21_2to1_21.rewriter;

import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.Protocol1_21_2To1_21;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage.PlayerStorage;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage.SneakingStorage;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.SoundEvent;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_2;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_21;
import com.viaversion.viaversion.api.type.types.version.Types1_21_2;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundConfigurationPackets1_21;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPackets1_21;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPackets1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPackets1_21_2;
import com.viaversion.viaversion.rewriter.RegistryDataRewriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public final class EntityPacketRewriter1_21_2 extends EntityRewriter<ClientboundPacket1_21_2, Protocol1_21_2To1_21> {

    private boolean warned = ViaBackwards.getConfig().suppressEmulationWarnings();

    public EntityPacketRewriter1_21_2(final Protocol1_21_2To1_21 protocol) {
        super(protocol, Types1_21.ENTITY_DATA_TYPES.optionalComponentType, Types1_21.ENTITY_DATA_TYPES.booleanType);
    }

    @Override
    public void registerPackets() {
        registerSetEntityData(ClientboundPackets1_21_2.SET_ENTITY_DATA, Types1_21_2.ENTITY_DATA_LIST, Types1_21.ENTITY_DATA_LIST);
        registerRemoveEntities(ClientboundPackets1_21_2.REMOVE_ENTITIES);
        protocol.registerClientbound(ClientboundPackets1_21_2.ADD_ENTITY, wrapper -> {
            final int entityId = wrapper.passthrough(Types.VAR_INT);
            wrapper.passthrough(Types.UUID); // Entity UUID
            final int entityTypeId = wrapper.passthrough(Types.VAR_INT);
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z
            wrapper.passthrough(Types.BYTE); // Pitch
            wrapper.passthrough(Types.BYTE); // Yaw
            wrapper.passthrough(Types.BYTE); // Head yaw
            wrapper.passthrough(Types.VAR_INT); // Data
            getSpawnTrackerWithDataHandler1_19(EntityTypes1_21_2.FALLING_BLOCK).handle(wrapper);

            final EntityType type = EntityTypes1_21_2.getTypeFromId(entityTypeId);
            if (type.isOrHasParent(EntityTypes1_21_2.ABSTRACT_BOAT)) {
                wrapper.send(Protocol1_21_2To1_21.class);
                wrapper.cancel();

                // Add boat type to entity data
                final List<EntityData> data = new ArrayList<>();
                final int boatType = type.isOrHasParent(EntityTypes1_21_2.ABSTRACT_CHEST_BOAT) ? chestBoatTypeFromEntityType(type) : boatTypeFromEntityType(type);
                data.add(new EntityData(11, Types1_21.ENTITY_DATA_TYPES.varIntType, boatType));

                final PacketWrapper entityDataPacket = wrapper.create(ClientboundPackets1_21.SET_ENTITY_DATA);
                entityDataPacket.write(Types.VAR_INT, entityId);
                entityDataPacket.write(Types1_21.ENTITY_DATA_LIST, data);
                entityDataPacket.send(Protocol1_21_2To1_21.class);
            }
        });

        final RegistryDataRewriter registryDataRewriter = new RegistryDataRewriter(protocol);
        registryDataRewriter.addEnchantmentEffectRewriter("change_item_damage", tag -> tag.putString("type", "damage_item"));
        protocol.registerClientbound(ClientboundConfigurationPackets1_21.REGISTRY_DATA, registryDataRewriter::handle);

        protocol.registerClientbound(ClientboundPackets1_21_2.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // Entity id
                map(Types.BOOLEAN); // Hardcore
                map(Types.STRING_ARRAY); // World List
                map(Types.VAR_INT); // Max players
                map(Types.VAR_INT); // View distance
                map(Types.VAR_INT); // Simulation distance
                map(Types.BOOLEAN); // Reduced debug info
                map(Types.BOOLEAN); // Show death screen
                map(Types.BOOLEAN); // Limited crafting
                map(Types.VAR_INT); // Dimension key
                map(Types.STRING); // World
                map(Types.LONG); // Seed
                map(Types.BYTE); // Gamemode
                map(Types.BYTE); // Previous gamemode
                map(Types.BOOLEAN); // Debug
                map(Types.BOOLEAN); // Flat
                map(Types.OPTIONAL_GLOBAL_POSITION); // Last death location
                map(Types.VAR_INT); // Portal cooldown
                handler(worldDataTrackerHandlerByKey1_20_5(3));
                handler(playerTrackerHandler());
                read(Types.VAR_INT); // Sea level
            }
        });

        protocol.registerClientbound(ClientboundPackets1_21_2.RESPAWN, wrapper -> {
            final int dimensionId = wrapper.passthrough(Types.VAR_INT);
            final String world = wrapper.passthrough(Types.STRING);
            wrapper.passthrough(Types.LONG); // Seed
            wrapper.passthrough(Types.BYTE); // Gamemode
            wrapper.passthrough(Types.BYTE); // Previous gamemode
            wrapper.passthrough(Types.BOOLEAN); // Debug
            wrapper.passthrough(Types.BOOLEAN); // Flat
            wrapper.passthrough(Types.OPTIONAL_GLOBAL_POSITION); // Last death location
            wrapper.passthrough(Types.VAR_INT); // Portal cooldown

            wrapper.read(Types.VAR_INT); // Sea level
            trackWorldDataByKey1_20_5(wrapper.user(), dimensionId, world);
        });

        protocol.registerClientbound(ClientboundPackets1_21_2.ENTITY_POSITION_SYNC, ClientboundPackets1_21.TELEPORT_ENTITY, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity ID
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z

            // Unused
            wrapper.read(Types.DOUBLE); // Delta movement X
            wrapper.read(Types.DOUBLE); // Delta movement Y
            wrapper.read(Types.DOUBLE); // Delta movement Z

            updateRotation(wrapper);
        });

        protocol.registerClientbound(ClientboundPackets1_21_2.PLAYER_ROTATION, ClientboundPackets1_21.PLAYER_LOOK_AT, wrapper -> {
            wrapper.passthrough(Types.FLOAT); // Y rot
            wrapper.passthrough(Types.FLOAT); // X rot

            final double yaw = Math.toRadians(wrapper.get(Types.FLOAT, 0));
            final double pitch = Math.toRadians(wrapper.get(Types.FLOAT, 1));

            final double factor = -Math.cos(-pitch);
            final double deltaX = Math.sin(-yaw - (float) Math.PI) * factor;
            final double deltaY = Math.sin(-pitch);
            final double deltaZ = Math.cos(-yaw - (float) Math.PI) * factor;

            final PlayerStorage storage = wrapper.user().get(PlayerStorage.class);
            wrapper.write(Types.VAR_INT, 0); // From anchor
            wrapper.write(Types.DOUBLE, storage.x() + deltaX); // X
            wrapper.write(Types.DOUBLE, storage.y() + deltaY); // Y
            wrapper.write(Types.DOUBLE, storage.z() + deltaZ); // Z
            wrapper.write(Types.BOOLEAN, false); // At entity

            final PacketWrapper entityMotionPacket = PacketWrapper.create(ServerboundPackets1_21_2.MOVE_PLAYER_ROT, wrapper.user());
            entityMotionPacket.write(Types.FLOAT, wrapper.get(Types.FLOAT, 0));
            entityMotionPacket.write(Types.FLOAT, wrapper.get(Types.FLOAT, 1));
            entityMotionPacket.write(Types.UNSIGNED_BYTE, (short) 0); // On ground and horizontal collision
            entityMotionPacket.sendToServer(Protocol1_21_2To1_21.class);
        });

        protocol.registerClientbound(ClientboundPackets1_21_2.TELEPORT_ENTITY, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity ID
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z

            double movementX = wrapper.read(Types.DOUBLE);
            double movementY = wrapper.read(Types.DOUBLE);
            double movementZ = wrapper.read(Types.DOUBLE);

            // Pack y and x rot
            updateRotation(wrapper);

            final int relativeArguments = wrapper.read(Types.VAR_INT);

            // Send alongside separate entity motion
            wrapper.send(Protocol1_21_2To1_21.class);
            wrapper.cancel();
            handleRelativeArguments(wrapper, relativeArguments, movementX, movementY, movementZ);
        });

        protocol.registerClientbound(ClientboundPackets1_21_2.PLAYER_POSITION, wrapper -> {
            final int teleportId = wrapper.read(Types.VAR_INT);

            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z

            double movementX = wrapper.read(Types.DOUBLE);
            double movementY = wrapper.read(Types.DOUBLE);
            double movementZ = wrapper.read(Types.DOUBLE);

            wrapper.passthrough(Types.FLOAT); // Y rot
            wrapper.passthrough(Types.FLOAT); // X rot

            // Just keep the new values in there
            final int relativeArguments = wrapper.read(Types.INT);
            wrapper.write(Types.BYTE, (byte) relativeArguments);
            wrapper.write(Types.VAR_INT, teleportId);

            // Send alongside separate entity motion
            wrapper.send(Protocol1_21_2To1_21.class);
            wrapper.cancel();
            handleRelativeArguments(wrapper, relativeArguments, movementX, movementY, movementZ);
        });

        protocol.registerServerbound(ServerboundPackets1_20_5.PLAYER_COMMAND, wrapper -> {
            wrapper.passthrough(Types.VAR_INT);
            final int action = wrapper.passthrough(Types.VAR_INT);
            if (action == 0) {
                wrapper.user().get(SneakingStorage.class).setPlayerCommandTrackedSneaking(true);
            } else if (action == 1) {
                wrapper.user().get(SneakingStorage.class).setPlayerCommandTrackedSneaking(false);
            }
        });

        // Now also sent by the player if not in a vehicle, but we can't emulate that here, and otherwise only used in predicates
        protocol.registerServerbound(ServerboundPackets1_20_5.PLAYER_INPUT, wrapper -> {
            final float sideways = wrapper.read(Types.FLOAT);
            final float forward = wrapper.read(Types.FLOAT);
            final byte flags = wrapper.read(Types.BYTE);

            byte updatedFlags = 0;
            if (forward > 0) {
                updatedFlags |= 1;
            } else if (forward < 0) {
                updatedFlags |= 1 << 1;
            }

            if (sideways < 0) {
                updatedFlags |= 1 << 2;
            } else if (sideways > 0) {
                updatedFlags |= 1 << 3;
            }

            if ((flags & 1) != 0) { // Jumping
                updatedFlags |= 1 << 4;
            }

            final boolean sneaking = (flags & 2) != 0;
            if (sneaking) {
                updatedFlags |= 1 << 5;
            }

            // Sprinting we don't know...

            wrapper.write(Types.BYTE, updatedFlags);

            // Player input no longer sets the sneaking state on the server
            // Send the change separately if needed (= when in a vehicle and player commands aren't sent by the old client)
            final SneakingStorage sneakingStorage = wrapper.user().get(SneakingStorage.class);
            if (sneakingStorage.setSneaking(sneaking)) {
                final PacketWrapper playerCommandPacket = wrapper.create(ServerboundPackets1_21_2.PLAYER_COMMAND);
                playerCommandPacket.write(Types.VAR_INT, tracker(wrapper.user()).clientEntityId());
                playerCommandPacket.write(Types.VAR_INT, sneaking ? 0 : 1); // Start/stop sneaking
                playerCommandPacket.write(Types.VAR_INT, 0); // Data
                playerCommandPacket.sendToServer(Protocol1_21_2To1_21.class);
            }
        });

        protocol.registerServerbound(ServerboundPackets1_20_5.MOVE_PLAYER_POS, wrapper -> {
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z
            fixOnGround(wrapper);

            final PlayerStorage storage = wrapper.user().get(PlayerStorage.class);
            storage.setPosition(wrapper);
        });
        protocol.registerServerbound(ServerboundPackets1_20_5.MOVE_PLAYER_POS_ROT, wrapper -> {
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z
            wrapper.passthrough(Types.FLOAT); // Yaw
            wrapper.passthrough(Types.FLOAT); // Pitch
            fixOnGround(wrapper);

            final PlayerStorage storage = wrapper.user().get(PlayerStorage.class);
            storage.setPosition(wrapper);
            storage.setRotation(wrapper);
        });
        protocol.registerServerbound(ServerboundPackets1_20_5.MOVE_PLAYER_ROT, wrapper -> {
            wrapper.passthrough(Types.FLOAT); // Yaw
            wrapper.passthrough(Types.FLOAT); // Pitch
            fixOnGround(wrapper);

            final PlayerStorage storage = wrapper.user().get(PlayerStorage.class);
            storage.setRotation(wrapper);
        });
        protocol.registerServerbound(ServerboundPackets1_20_5.MOVE_PLAYER_STATUS_ONLY, this::fixOnGround);
        protocol.registerServerbound(ServerboundPackets1_20_5.MOVE_VEHICLE, wrapper -> {
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z
            wrapper.passthrough(Types.FLOAT); // Yaw
            wrapper.passthrough(Types.FLOAT); // Pitch

            final PlayerStorage storage = wrapper.user().get(PlayerStorage.class);
            storage.setPosition(wrapper);
            storage.setRotation(wrapper);
        });

        protocol.registerClientbound(ClientboundPackets1_21_2.PLAYER_INFO_UPDATE, wrapper -> {
            final BitSet actions = wrapper.passthrough(Types.PROFILE_ACTIONS_ENUM1_21_2);
            final int entries = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < entries; i++) {
                wrapper.passthrough(Types.UUID);
                if (actions.get(0)) {
                    wrapper.passthrough(Types.STRING); // Player Name

                    final int properties = wrapper.passthrough(Types.VAR_INT);
                    for (int j = 0; j < properties; j++) {
                        wrapper.passthrough(Types.STRING); // Name
                        wrapper.passthrough(Types.STRING); // Value
                        wrapper.passthrough(Types.OPTIONAL_STRING); // Signature
                    }
                }
                if (actions.get(1) && wrapper.passthrough(Types.BOOLEAN)) {
                    wrapper.passthrough(Types.UUID); // Session UUID
                    wrapper.passthrough(Types.PROFILE_KEY);
                }
                if (actions.get(2)) {
                    wrapper.passthrough(Types.VAR_INT); // Gamemode
                }
                if (actions.get(3)) {
                    wrapper.passthrough(Types.BOOLEAN); // Listed
                }
                if (actions.get(4)) {
                    wrapper.passthrough(Types.VAR_INT); // Latency
                }
                if (actions.get(5)) {
                    final Tag displayName = wrapper.passthrough(Types.TAG);
                    protocol.getComponentRewriter().processTag(wrapper.user(), displayName);
                }

                // New one
                if (actions.get(6)) {
                    actions.clear(6);
                    wrapper.read(Types.VAR_INT); // List order
                }
            }
        });
    }

    private void updateRotation(PacketWrapper wrapper) {
        // Pack y and x rot
        final float yaw = wrapper.read(Types.FLOAT);
        final float pitch = wrapper.read(Types.FLOAT);
        wrapper.write(Types.BYTE, (byte) Math.floor(yaw * 256F / 360F));
        wrapper.write(Types.BYTE, (byte) Math.floor(pitch * 256F / 360F));
    }

    private void handleRelativeArguments(final PacketWrapper wrapper, final int relativeArguments, double movementX, double movementY, double movementZ) {
        final PlayerStorage storage = wrapper.user().get(PlayerStorage.class);
        storage.setPosition(wrapper);

        // Rotate Delta
        if ((relativeArguments & 1 << 8) != 0) {
            float yaw = wrapper.get(Types.FLOAT, 4);
            if ((relativeArguments & 1 << 3) != 0) {
                yaw += storage.yaw();
            }

            float pitch = wrapper.get(Types.FLOAT, 5);
            if ((relativeArguments & 1 << 4) != 0) {
                pitch += storage.pitch();
            }

            final double deltaYaw = Math.toRadians(storage.yaw() - yaw);
            final double deltaYawCos = Math.cos(deltaYaw);
            final double deltaYawSin = Math.sin(deltaYaw);
            movementX = movementX * deltaYawCos + movementZ * deltaYawSin;
            movementZ = movementZ * deltaYawCos - movementX * deltaYawSin;

            final double deltaPitch = Math.toRadians(storage.pitch() - pitch);
            final double deltaPitchCos = Math.cos(deltaPitch);
            final double deltaPitchSin = Math.sin(deltaPitch);
            movementY = movementY * deltaPitchCos + movementZ * deltaPitchSin;
            movementZ = movementZ * deltaPitchCos - movementY * deltaPitchSin;
        }

        final boolean relativeDeltaX = (relativeArguments & 1 << 5) != 0;
        final boolean relativeDeltaY = (relativeArguments & 1 << 6) != 0;
        final boolean relativeDeltaZ = (relativeArguments & 1 << 7) != 0;

        // Delta x, y, z
        if (relativeDeltaX && relativeDeltaY && relativeDeltaZ) {
            final PacketWrapper explosionPacket = wrapper.create(ClientboundPackets1_21.EXPLODE);
            explosionPacket.write(Types.DOUBLE, 0.0); // Center X
            explosionPacket.write(Types.DOUBLE, 0.0); // Center Y
            explosionPacket.write(Types.DOUBLE, 0.0); // Center Z
            explosionPacket.write(Types.FLOAT, 0F); // Power
            explosionPacket.write(Types.VAR_INT, 0); // Blocks affected
            explosionPacket.write(Types.FLOAT, (float) movementX);
            explosionPacket.write(Types.FLOAT, (float) movementY);
            explosionPacket.write(Types.FLOAT, (float) movementZ);
            explosionPacket.write(Types.VAR_INT, 0); // Block interaction
            explosionPacket.write(Types1_21.PARTICLE, new Particle(0)); // Small explosion
            explosionPacket.write(Types1_21.PARTICLE, new Particle(0)); // Large explosion
            explosionPacket.write(Types.SOUND_EVENT, Holder.of(new SoundEvent("", null))); // Explosion sound

            explosionPacket.send(Protocol1_21_2To1_21.class);
        } else if (!relativeDeltaX && !relativeDeltaY && !relativeDeltaZ) {
            final PacketWrapper entityMotionPacket = wrapper.create(ClientboundPackets1_21.SET_ENTITY_MOTION);
            entityMotionPacket.write(Types.VAR_INT, tracker(wrapper.user()).clientEntityId());
            entityMotionPacket.write(Types.SHORT, (short) (movementX * 8000));
            entityMotionPacket.write(Types.SHORT, (short) (movementY * 8000));
            entityMotionPacket.write(Types.SHORT, (short) (movementZ * 8000));

            entityMotionPacket.send(Protocol1_21_2To1_21.class);
        } else if (!warned) {
            // Mixed combinations of relative and absolute would require tracking the previous delta movement
            // which is quite impossible without doing massive player simulation on protocol level.

            // This is bad but so is life.
            protocol.getLogger().warning("Mixed combinations of relative and absolute delta movements are not supported for 1.21.1 players. " +
                    "This will result in incorrect movement for the player. ");
            warned = true;
        }

        storage.setRotation(wrapper);
    }

    private int boatTypeFromEntityType(final EntityType type) {
        if (type == EntityTypes1_21_2.OAK_BOAT) {
            return 0;
        } else if (type == EntityTypes1_21_2.SPRUCE_BOAT) {
            return 1;
        } else if (type == EntityTypes1_21_2.BIRCH_BOAT) {
            return 2;
        } else if (type == EntityTypes1_21_2.JUNGLE_BOAT) {
            return 3;
        } else if (type == EntityTypes1_21_2.ACACIA_BOAT) {
            return 4;
        } else if (type == EntityTypes1_21_2.CHERRY_BOAT) {
            return 5;
        } else if (type == EntityTypes1_21_2.DARK_OAK_BOAT) {
            return 6;
        } else if (type == EntityTypes1_21_2.MANGROVE_BOAT) {
            return 7;
        } else if (type == EntityTypes1_21_2.BAMBOO_RAFT) {
            return 8;
        } else {
            return 0;
        }
    }

    private int chestBoatTypeFromEntityType(final EntityType type) {
        if (type == EntityTypes1_21_2.OAK_CHEST_BOAT) {
            return 0;
        } else if (type == EntityTypes1_21_2.SPRUCE_CHEST_BOAT) {
            return 1;
        } else if (type == EntityTypes1_21_2.BIRCH_CHEST_BOAT) {
            return 2;
        } else if (type == EntityTypes1_21_2.JUNGLE_CHEST_BOAT) {
            return 3;
        } else if (type == EntityTypes1_21_2.ACACIA_CHEST_BOAT) {
            return 4;
        } else if (type == EntityTypes1_21_2.CHERRY_CHEST_BOAT) {
            return 5;
        } else if (type == EntityTypes1_21_2.DARK_OAK_CHEST_BOAT) {
            return 6;
        } else if (type == EntityTypes1_21_2.MANGROVE_CHEST_BOAT) {
            return 7;
        } else if (type == EntityTypes1_21_2.BAMBOO_CHEST_RAFT) {
            return 8;
        } else {
            return 0;
        }
    }

    private void fixOnGround(final PacketWrapper wrapper) {
        final boolean data = wrapper.read(Types.BOOLEAN);
        wrapper.write(Types.UNSIGNED_BYTE, data ? (short) 1 : 0); // Carries more data now
    }

    @Override
    protected void registerRewrites() {
        filter().mapDataType(Types1_21.ENTITY_DATA_TYPES::byId);
        registerEntityDataTypeHandler1_20_3(
            Types1_21.ENTITY_DATA_TYPES.itemType,
            Types1_21.ENTITY_DATA_TYPES.blockStateType,
            Types1_21.ENTITY_DATA_TYPES.optionalBlockStateType,
            Types1_21.ENTITY_DATA_TYPES.particleType,
            Types1_21.ENTITY_DATA_TYPES.particlesType,
            Types1_21.ENTITY_DATA_TYPES.componentType,
            Types1_21.ENTITY_DATA_TYPES.optionalComponentType
        );
        registerBlockStateHandler(EntityTypes1_21_2.ABSTRACT_MINECART, 11);

        filter().type(EntityTypes1_21_2.CREAKING).cancel(17); // Active
        filter().type(EntityTypes1_21_2.CREAKING).cancel(16); // Can move

        filter().type(EntityTypes1_21_2.CREAKING_TRANSIENT).handler((event, data) -> {
            if (event.index() > 7) {
                event.cancel();
            }
        });

        filter().type(EntityTypes1_21_2.ABSTRACT_BOAT).addIndex(11); // Boat type
        filter().type(EntityTypes1_21_2.SALMON).removeIndex(17); // Data type
        filter().type(EntityTypes1_21_2.DOLPHIN).removeIndex(16); // Baby
        filter().type(EntityTypes1_21_2.GLOW_SQUID).removeIndex(16); // Baby
        filter().type(EntityTypes1_21_2.SQUID).removeIndex(16); // Baby

        filter().type(EntityTypes1_21_2.ABSTRACT_ARROW).removeIndex(10); // In ground
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_21_2.getTypeFromId(type);
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();

        mapEntityTypeWithData(EntityTypes1_21_2.CREAKING, EntityTypes1_21_2.WARDEN).jsonName();
        mapEntityTypeWithData(EntityTypes1_21_2.CREAKING_TRANSIENT, EntityTypes1_21_2.TEXT_DISPLAY);
    }
}
