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
package com.viaversion.viabackwards.protocol.v1_21_2to1_21.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.FloatTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.rewriters.BackwardsRegistryRewriter;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.Protocol1_21_2To1_21;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage.PlayerStorage;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.api.minecraft.SoundEvent;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_2;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundConfigurationPackets1_21;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPackets1_21;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPackets1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPackets1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.storage.ClientVehicleStorage;
import com.viaversion.viaversion.rewriter.RegistryDataRewriter;
import com.viaversion.viaversion.util.Key;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class EntityPacketRewriter1_21_2 extends EntityRewriter<ClientboundPacket1_21_2, Protocol1_21_2To1_21> {

    private static final int REL_X = 0;
    private static final int REL_Y = 1;
    private static final int REL_Z = 2;
    private static final int REL_Y_ROT = 3;
    private static final int REL_X_ROT = 4;
    private static final int REL_DELTA_X = 5;
    private static final int REL_DELTA_Y = 6;
    private static final int REL_DELTA_Z = 7;
    private static final int REL_ROTATE_DELTA = 8;
    private boolean warned = ViaBackwards.getConfig().suppressEmulationWarnings();

    public EntityPacketRewriter1_21_2(final Protocol1_21_2To1_21 protocol) {
        super(protocol, VersionedTypes.V1_21.entityDataTypes.optionalComponentType, VersionedTypes.V1_21.entityDataTypes.booleanType);
    }

    @Override
    public void registerPackets() {
        registerSetEntityData(ClientboundPackets1_21_2.SET_ENTITY_DATA);
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
                data.add(new EntityData(11, VersionedTypes.V1_21.entityDataTypes.varIntType, boatType));

                final PacketWrapper entityDataPacket = wrapper.create(ClientboundPackets1_21.SET_ENTITY_DATA);
                entityDataPacket.write(Types.VAR_INT, entityId);
                entityDataPacket.write(VersionedTypes.V1_21.entityDataList, data);
                entityDataPacket.send(Protocol1_21_2To1_21.class);
            }
        });

        final RegistryDataRewriter registryDataRewriter = new BackwardsRegistryRewriter(protocol);
        registryDataRewriter.addEnchantmentEffectRewriter("change_item_damage", tag -> tag.putString("type", "damage_item"));
        protocol.registerClientbound(ClientboundConfigurationPackets1_21.REGISTRY_DATA, wrapper -> {
            final String registryKey = Key.stripMinecraftNamespace(wrapper.passthrough(Types.STRING));
            final RegistryEntry[] entries = wrapper.read(Types.REGISTRY_ENTRY_ARRAY);
            if (registryKey.equals("instrument")) {
                wrapper.cancel();
                return;
            }

            if (registryKey.equals("worldgen/biome")) {
                for (final RegistryEntry entry : entries) {
                    if (entry.tag() == null) {
                        continue;
                    }

                    final CompoundTag effects = ((CompoundTag) entry.tag()).getCompoundTag("effects");
                    final CompoundTag particle = effects.getCompoundTag("particle");
                    if (particle == null) {
                        continue;
                    }

                    final CompoundTag particleOptions = particle.getCompoundTag("options");
                    final String particleType = particleOptions.getString("type");
                    updateParticleFormat(particleOptions, Key.stripMinecraftNamespace(particleType));
                }
            }

            wrapper.write(Types.REGISTRY_ENTRY_ARRAY, registryDataRewriter.handle(wrapper.user(), registryKey, entries));
        });

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

            final float yaw = wrapper.read(Types.FLOAT);
            final float pitch = wrapper.read(Types.FLOAT);
            writePackedRotation(wrapper, yaw, pitch);
        });

        protocol.registerClientbound(ClientboundPackets1_21_2.PLAYER_ROTATION, ClientboundPackets1_21.PLAYER_LOOK_AT, wrapper -> {
            final float yaw = wrapper.read(Types.FLOAT);
            final float pitch = wrapper.read(Types.FLOAT);

            final double yRadians = Math.toRadians(yaw);
            final double xRadians = Math.toRadians(pitch);

            final double factor = -Math.cos(-xRadians);
            final double deltaX = Math.sin(-yRadians - (float) Math.PI) * factor;
            final double deltaY = Math.sin(-xRadians);
            final double deltaZ = Math.cos(-yRadians - (float) Math.PI) * factor;

            final PlayerStorage storage = wrapper.user().get(PlayerStorage.class);
            wrapper.write(Types.VAR_INT, 0); // From anchor
            wrapper.write(Types.DOUBLE, storage.x() + deltaX); // X
            wrapper.write(Types.DOUBLE, storage.y() + deltaY); // Y
            wrapper.write(Types.DOUBLE, storage.z() + deltaZ); // Z
            wrapper.write(Types.BOOLEAN, false); // At entity

            final PacketWrapper entityMotionPacket = PacketWrapper.create(ServerboundPackets1_21_2.MOVE_PLAYER_ROT, wrapper.user());
            entityMotionPacket.write(Types.FLOAT, yaw);
            entityMotionPacket.write(Types.FLOAT, pitch);
            entityMotionPacket.write(Types.UNSIGNED_BYTE, (short) 0); // On ground and horizontal collision
            entityMotionPacket.sendToServer(Protocol1_21_2To1_21.class);
        });

        protocol.registerClientbound(ClientboundPackets1_21_2.TELEPORT_ENTITY, wrapper -> {
            final int entityId = wrapper.passthrough(Types.VAR_INT);
            final double x = wrapper.passthrough(Types.DOUBLE);
            final double y = wrapper.passthrough(Types.DOUBLE);
            final double z = wrapper.passthrough(Types.DOUBLE);

            final double movementX = wrapper.read(Types.DOUBLE);
            final double movementY = wrapper.read(Types.DOUBLE);
            final double movementZ = wrapper.read(Types.DOUBLE);

            final float yaw = wrapper.read(Types.FLOAT);
            final float pitch = wrapper.read(Types.FLOAT);
            writePackedRotation(wrapper, yaw, pitch);

            final int relativeArguments = wrapper.read(Types.INT);

            // Send alongside separate entity motion
            wrapper.send(Protocol1_21_2To1_21.class);
            wrapper.cancel();

            handleRelativeArguments(wrapper, x, y, z, yaw, pitch, relativeArguments, movementX, movementY, movementZ, entityId);
        });

        protocol.registerClientbound(ClientboundPackets1_21_2.PLAYER_POSITION, wrapper -> {
            final int teleportId = wrapper.read(Types.VAR_INT);

            final double x = wrapper.passthrough(Types.DOUBLE);
            final double y = wrapper.passthrough(Types.DOUBLE);
            final double z = wrapper.passthrough(Types.DOUBLE);

            final double movementX = wrapper.read(Types.DOUBLE);
            final double movementY = wrapper.read(Types.DOUBLE);
            final double movementZ = wrapper.read(Types.DOUBLE);

            final float yaw = wrapper.passthrough(Types.FLOAT);
            final float pitch = wrapper.passthrough(Types.FLOAT);

            // Just keep the new values in there
            final int relativeArguments = wrapper.read(Types.INT);
            wrapper.write(Types.BYTE, (byte) relativeArguments);
            wrapper.write(Types.VAR_INT, teleportId);

            // Send alongside separate entity motion
            wrapper.send(Protocol1_21_2To1_21.class);
            wrapper.cancel();

            handleRelativeArguments(wrapper, x, y, z, yaw, pitch, relativeArguments, movementX, movementY, movementZ, null);
        });

        protocol.registerServerbound(ServerboundPackets1_20_5.PLAYER_COMMAND, wrapper -> {
            wrapper.passthrough(Types.VAR_INT);
            final int action = wrapper.passthrough(Types.VAR_INT);

            final PlayerStorage storage = wrapper.user().get(PlayerStorage.class);
            if (action == 0) {
                storage.setPlayerCommandTrackedSneaking(true);
            } else if (action == 1) {
                storage.setPlayerCommandTrackedSneaking(false);
            } else if (action == 3) {
                storage.setPlayerCommandTrackedSprinting(true);
            } else if (action == 4) {
                storage.setPlayerCommandTrackedSprinting(false);
            }
        });

        // Now also sent by the player if not in a vehicle, but we can't emulate that here, and otherwise only used in predicates
        protocol.registerServerbound(ServerboundPackets1_20_5.PLAYER_INPUT, wrapper -> {
            final float sideways = wrapper.read(Types.FLOAT);
            final float forward = wrapper.read(Types.FLOAT);
            final byte flags = wrapper.read(Types.BYTE);

            byte updatedFlags = 0;
            if (forward > 0) {
                updatedFlags |= 1; // Forward
            } else if (forward < 0) {
                updatedFlags |= 1 << 1; // Backward
            }

            if (sideways > 0) {
                updatedFlags |= 1 << 2; // Left
            } else if (sideways < 0) {
                updatedFlags |= 1 << 3; // Right
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
            sendSneakingPlayerCommand(wrapper, sneaking);
        });

        protocol.registerServerbound(ServerboundPackets1_20_5.MOVE_PLAYER_POS, wrapper -> {
            final double x = wrapper.passthrough(Types.DOUBLE);
            final double y = wrapper.passthrough(Types.DOUBLE);
            final double z = wrapper.passthrough(Types.DOUBLE);
            fixOnGround(wrapper);

            final PlayerStorage storage = wrapper.user().get(PlayerStorage.class);
            storage.setPosition(x, y, z);
        });
        protocol.registerServerbound(ServerboundPackets1_20_5.MOVE_PLAYER_POS_ROT, wrapper -> {
            final double x = wrapper.passthrough(Types.DOUBLE);
            final double y = wrapper.passthrough(Types.DOUBLE);
            final double z = wrapper.passthrough(Types.DOUBLE);
            final float yaw = wrapper.passthrough(Types.FLOAT);
            final float pitch = wrapper.passthrough(Types.FLOAT);
            fixOnGround(wrapper);

            final PlayerStorage storage = wrapper.user().get(PlayerStorage.class);
            storage.setPosition(x, y, z);
            storage.setRotation(yaw, pitch);
        });
        protocol.registerServerbound(ServerboundPackets1_20_5.MOVE_PLAYER_ROT, wrapper -> {
            final float yaw = wrapper.passthrough(Types.FLOAT);
            final float pitch = wrapper.passthrough(Types.FLOAT);
            fixOnGround(wrapper);

            final PlayerStorage storage = wrapper.user().get(PlayerStorage.class);
            storage.setRotation(yaw, pitch);
        });
        protocol.registerServerbound(ServerboundPackets1_20_5.MOVE_PLAYER_STATUS_ONLY, this::fixOnGround);
        protocol.registerServerbound(ServerboundPackets1_20_5.MOVE_VEHICLE, wrapper -> {
            final double x = wrapper.passthrough(Types.DOUBLE);
            final double y = wrapper.passthrough(Types.DOUBLE);
            final double z = wrapper.passthrough(Types.DOUBLE);
            final float yaw = wrapper.passthrough(Types.FLOAT);
            final float pitch = wrapper.passthrough(Types.FLOAT);

            final PlayerStorage storage = wrapper.user().get(PlayerStorage.class);
            storage.setPosition(x, y, z);
            storage.setRotation(yaw, pitch);
        });

        protocol.registerClientbound(ClientboundPackets1_21_2.PLAYER_INFO_UPDATE, wrapper -> {
            final BitSet actions = wrapper.read(Types.PROFILE_ACTIONS_ENUM1_21_2);
            // We need to recreate the BitSet field itself to remove the new action
            final BitSet updatedActions = new BitSet(6);
            for (int i = 0; i < 6; i++) {
                if (actions.get(i)) {
                    updatedActions.set(i);
                }
            }
            wrapper.write(Types.PROFILE_ACTIONS_ENUM1_19_3, updatedActions);

            final int entries = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < entries; i++) {
                wrapper.passthrough(Types.UUID);
                if (actions.get(0)) {
                    wrapper.passthrough(Types.STRING); // Player Name
                    wrapper.passthrough(Types.PROFILE_PROPERTY_ARRAY);
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
                    final Tag displayName = wrapper.passthrough(Types.OPTIONAL_TAG);
                    protocol.getComponentRewriter().processTag(wrapper.user(), displayName);
                }

                // New one
                if (actions.get(6)) {
                    wrapper.read(Types.VAR_INT); // List order
                }
            }
        });
        protocol.registerClientbound(ClientboundPackets1_21_2.SET_PASSENGERS, wrapper -> {
            final int vehicleId = wrapper.passthrough(Types.VAR_INT);
            final int[] passengerIds = wrapper.passthrough(Types.VAR_INT_ARRAY_PRIMITIVE);
            final ClientVehicleStorage storage = wrapper.user().get(ClientVehicleStorage.class);
            if (storage != null && vehicleId == storage.vehicleId()) {
                wrapper.user().remove(ClientVehicleStorage.class);
                sendSneakingPlayerCommand(wrapper, false);
            }

            final int clientEntityId = tracker(wrapper.user()).clientEntityId();
            for (final int passenger : passengerIds) {
                if (passenger == clientEntityId) {
                    wrapper.user().put(new ClientVehicleStorage(vehicleId));
                    break;
                }
            }
        });
        protocol.appendClientbound(ClientboundPackets1_21_2.REMOVE_ENTITIES, wrapper -> {
            final ClientVehicleStorage vehicleStorage = wrapper.user().get(ClientVehicleStorage.class);
            if (vehicleStorage == null) {
                return;
            }

            final int[] entityIds = wrapper.get(Types.VAR_INT_ARRAY_PRIMITIVE, 0);
            for (final int entityId : entityIds) {
                if (entityId == vehicleStorage.vehicleId()) {
                    wrapper.user().remove(ClientVehicleStorage.class);
                    sendSneakingPlayerCommand(wrapper, false);
                    break;
                }
            }
        });
    }

    private void updateParticleFormat(final CompoundTag options, final String particleType) {
        // Have to be float lists in older versions
        if ("dust_color_transition".equals(particleType)) {
            replaceColor(options, "from_color");
            replaceColor(options, "to_color");
        } else if ("dust".equals(particleType)) {
            replaceColor(options, "color");
        }
    }

    private void replaceColor(final CompoundTag options, final String to_color) {
        final IntTag toColorTag = options.getIntTag(to_color);
        if (toColorTag == null) {
            return;
        }

        final int rgb = toColorTag.asInt();
        final float r = ((rgb >> 16) & 0xFF) / 255F;
        final float g = ((rgb >> 8) & 0xFF) / 255F;
        final float b = (rgb & 0xFF) / 255F;
        options.put(to_color, new ListTag<>(List.of(new FloatTag(r), new FloatTag(g), new FloatTag(b))));
    }

    private void writePackedRotation(final PacketWrapper wrapper, final float yaw, final float pitch) {
        // Pack y and x rot
        wrapper.write(Types.BYTE, (byte) Math.floor(yaw * 256F / 360F));
        wrapper.write(Types.BYTE, (byte) Math.floor(pitch * 256F / 360F));
    }

    private void handleRelativeArguments(
        final PacketWrapper wrapper,
        double x, double y, double z,
        float yaw, float pitch,
        final int relativeArguments,
        double movementX, double movementY, double movementZ,
        @Nullable final Integer entityId
    ) {
        // Position and rotation
        final PlayerStorage storage = wrapper.user().get(PlayerStorage.class);
        if ((relativeArguments & 1 << REL_X) != 0) {
            x += storage.x();
        }
        if ((relativeArguments & 1 << REL_Y) != 0) {
            y += storage.y();
        }
        if ((relativeArguments & 1 << REL_Z) != 0) {
            z += storage.z();
        }
        if ((relativeArguments & 1 << REL_Y_ROT) != 0) {
            yaw += storage.yaw();
        }
        if ((relativeArguments & 1 << REL_X_ROT) != 0) {
            pitch += storage.pitch();
        }

        // Movement rotation
        if ((relativeArguments & 1 << REL_ROTATE_DELTA) != 0) {
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

        final boolean relativeDeltaX = (relativeArguments & 1 << REL_DELTA_X) != 0;
        final boolean relativeDeltaY = (relativeArguments & 1 << REL_DELTA_Y) != 0;
        final boolean relativeDeltaZ = (relativeArguments & 1 << REL_DELTA_Z) != 0;

        // Update after having used its previous data
        storage.setPosition(x, y, z);
        storage.setRotation(yaw, pitch);

        // Movement
        if (relativeDeltaX && relativeDeltaY && relativeDeltaZ) {
            if (entityId != null && entityId != tracker(wrapper.user()).clientEntityId()) {
                // Can only handle the client entity
                return;
            }

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
            explosionPacket.write(VersionedTypes.V1_21.particle(), new Particle(0)); // Small explosion
            explosionPacket.write(VersionedTypes.V1_21.particle(), new Particle(0)); // Large explosion
            explosionPacket.write(Types.SOUND_EVENT, Holder.of(new SoundEvent("", null))); // Explosion sound

            explosionPacket.send(Protocol1_21_2To1_21.class);
        } else if (!relativeDeltaX && !relativeDeltaY && !relativeDeltaZ) {
            final PacketWrapper entityMotionPacket = wrapper.create(ClientboundPackets1_21.SET_ENTITY_MOTION);
            entityMotionPacket.write(Types.VAR_INT, entityId != null ? entityId : tracker(wrapper.user()).clientEntityId());
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
    }

    private void sendSneakingPlayerCommand(final PacketWrapper wrapper, final boolean sneaking) {
        final PlayerStorage sneakingStorage = wrapper.user().get(PlayerStorage.class);
        if (sneakingStorage.setSneaking(sneaking)) {
            final PacketWrapper playerCommandPacket = wrapper.create(ServerboundPackets1_21_2.PLAYER_COMMAND);
            playerCommandPacket.write(Types.VAR_INT, tracker(wrapper.user()).clientEntityId());
            playerCommandPacket.write(Types.VAR_INT, sneaking ? 0 : 1); // Start/stop sneaking
            playerCommandPacket.write(Types.VAR_INT, 0); // Data
            playerCommandPacket.sendToServer(Protocol1_21_2To1_21.class);
        }
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
        filter().mapDataType(VersionedTypes.V1_21.entityDataTypes::byId);
        registerEntityDataTypeHandler1_20_3(
            VersionedTypes.V1_21.entityDataTypes.itemType,
            VersionedTypes.V1_21.entityDataTypes.blockStateType,
            VersionedTypes.V1_21.entityDataTypes.optionalBlockStateType,
            VersionedTypes.V1_21.entityDataTypes.particleType,
            VersionedTypes.V1_21.entityDataTypes.particlesType,
            VersionedTypes.V1_21.entityDataTypes.componentType,
            VersionedTypes.V1_21.entityDataTypes.optionalComponentType
        );
        registerBlockStateHandler(EntityTypes1_21_2.ABSTRACT_MINECART, 11);

        filter().type(EntityTypes1_21_2.CREAKING).cancel(17); // Active
        filter().type(EntityTypes1_21_2.CREAKING).cancel(16); // Can move

        filter().type(EntityTypes1_21_2.ABSTRACT_BOAT).addIndex(11); // Boat type
        filter().type(EntityTypes1_21_2.SALMON).removeIndex(17); // Data type
        filter().type(EntityTypes1_21_2.AGEABLE_WATER_CREATURE).removeIndex(16); // Baby

        filter().type(EntityTypes1_21_2.ABSTRACT_ARROW).removeIndex(10); // In ground
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_21_2.getTypeFromId(type);
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();

        mapEntityTypeWithData(EntityTypes1_21_2.CREAKING, EntityTypes1_21_2.WARDEN).tagName();
        mapEntityTypeWithData(EntityTypes1_21_2.CREAKING_TRANSIENT, EntityTypes1_21_2.WARDEN).tagName();
    }
}
