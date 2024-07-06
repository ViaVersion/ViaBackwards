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
package com.viaversion.viabackwards.protocol.v1_19to1_18_2.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_19to1_18_2.Protocol1_19To1_18_2;
import com.viaversion.viabackwards.protocol.v1_19to1_18_2.storage.DimensionRegistryStorage;
import com.viaversion.viabackwards.protocol.v1_19to1_18_2.storage.LastDeathPosition;
import com.viaversion.viabackwards.protocol.v1_19to1_18_2.storage.StoredPainting;
import com.viaversion.viaversion.api.data.ParticleMappings;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.minecraft.GlobalBlockPosition;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityDataType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_18;
import com.viaversion.viaversion.api.type.types.version.Types1_19;
import com.viaversion.viaversion.protocols.v1_17_1to1_18.packet.ClientboundPackets1_18;
import com.viaversion.viaversion.protocols.v1_18_2to1_19.packet.ClientboundPackets1_19;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.TagUtil;

public final class EntityPacketRewriter1_19 extends EntityRewriter<ClientboundPackets1_19, Protocol1_19To1_18_2> {

    public EntityPacketRewriter1_19(final Protocol1_19To1_18_2 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerTracker(ClientboundPackets1_19.ADD_EXPERIENCE_ORB, EntityTypes1_19.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_19.ADD_PLAYER, EntityTypes1_19.PLAYER);
        registerSetEntityData(ClientboundPackets1_19.SET_ENTITY_DATA, Types1_19.ENTITY_DATA_LIST, Types1_18.ENTITY_DATA_LIST);
        registerRemoveEntities(ClientboundPackets1_19.REMOVE_ENTITIES);

        protocol.registerClientbound(ClientboundPackets1_19.ADD_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Entity id
                map(Types.UUID); // Entity UUID
                map(Types.VAR_INT); // Entity Type
                map(Types.DOUBLE); // X
                map(Types.DOUBLE); // Y
                map(Types.DOUBLE); // Z
                map(Types.BYTE); // Pitch
                map(Types.BYTE); // Yaw
                handler(wrapper -> {
                    final byte headYaw = wrapper.read(Types.BYTE);
                    int data = wrapper.read(Types.VAR_INT);
                    final EntityType entityType = trackAndMapEntity(wrapper);
                    if (entityType.isOrHasParent(EntityTypes1_19.LIVING_ENTITY)) {
                        wrapper.write(Types.BYTE, headYaw);

                        // Switch pitch and yaw position
                        final byte pitch = wrapper.get(Types.BYTE, 0);
                        final byte yaw = wrapper.get(Types.BYTE, 1);
                        wrapper.set(Types.BYTE, 0, yaw);
                        wrapper.set(Types.BYTE, 1, pitch);

                        wrapper.setPacketType(ClientboundPackets1_18.ADD_MOB);
                        return;
                    } else if (entityType == EntityTypes1_19.PAINTING) {
                        wrapper.cancel();
                        // The entity has been tracked, now we wait for the entity data packet
                        final int entityId = wrapper.get(Types.VAR_INT, 0);
                        final StoredEntityData entityData = tracker(wrapper.user()).entityData(entityId);
                        final BlockPosition position = new BlockPosition(wrapper.get(Types.DOUBLE, 0).intValue(), wrapper.get(Types.DOUBLE, 1).intValue(), wrapper.get(Types.DOUBLE, 2).intValue());
                        entityData.put(new StoredPainting(entityId, wrapper.get(Types.UUID, 0), position, data));
                        return;
                    }

                    if (entityType == EntityTypes1_19.FALLING_BLOCK) {
                        data = protocol.getMappingData().getNewBlockStateId(data);
                    }
                    wrapper.write(Types.INT, data);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19.UPDATE_MOB_EFFECT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Entity id
                map(Types.VAR_INT); // Effect id
                map(Types.BYTE); // Amplifier
                map(Types.VAR_INT); // Duration
                map(Types.BYTE); // Flags
                handler(wrapper -> {
                    // Remove factor data
                    wrapper.read(Types.OPTIONAL_NAMED_COMPOUND_TAG);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // Entity ID
                map(Types.BOOLEAN); // Hardcore
                map(Types.BYTE); // Gamemode
                map(Types.BYTE); // Previous Gamemode
                map(Types.STRING_ARRAY); // Worlds
                map(Types.NAMED_COMPOUND_TAG); // Dimension registry
                handler(wrapper -> {
                    final DimensionRegistryStorage dimensionRegistryStorage = wrapper.user().get(DimensionRegistryStorage.class);
                    dimensionRegistryStorage.clear();

                    // Cache dimensions and find current dimension
                    final String dimensionKey = Key.stripMinecraftNamespace(wrapper.read(Types.STRING));
                    final CompoundTag registry = wrapper.get(Types.NAMED_COMPOUND_TAG, 0);
                    final ListTag<CompoundTag> dimensions = TagUtil.getRegistryEntries(registry, "dimension_type");
                    boolean found = false;
                    for (final CompoundTag dimension : dimensions) {
                        final String name = Key.stripMinecraftNamespace(dimension.getString("name"));
                        final CompoundTag dimensionData = dimension.getCompoundTag("element");
                        dimensionRegistryStorage.addDimension(name, dimensionData.copy());

                        if (!found && name.equals(dimensionKey)) {
                            wrapper.write(Types.NAMED_COMPOUND_TAG, dimensionData);
                            found = true;
                        }
                    }
                    if (!found) {
                        throw new IllegalStateException("Could not find dimension " + dimensionKey + " in dimension registry");
                    }

                    // Add biome category and track biomes
                    final ListTag<CompoundTag> biomes = TagUtil.getRegistryEntries(registry, "worldgen/biome");
                    for (final CompoundTag biome : biomes) {
                        final CompoundTag biomeCompound = biome.getCompoundTag("element");
                        biomeCompound.putString("category", "none");
                    }
                    tracker(wrapper.user()).setBiomesSent(biomes.size());

                    // Cache and remove chat types
                    final ListTag<CompoundTag> chatTypes = TagUtil.removeRegistryEntries(registry, "chat_type");
                    for (final CompoundTag chatType : chatTypes) {
                        final NumberTag idTag = chatType.getNumberTag("id");
                        dimensionRegistryStorage.addChatType(idTag.asInt(), chatType);
                    }
                });
                map(Types.STRING); // World
                map(Types.LONG); // Seed
                map(Types.VAR_INT); // Max players
                map(Types.VAR_INT); // Chunk radius
                map(Types.VAR_INT); // Simulation distance
                map(Types.BOOLEAN); // Reduced debug info
                map(Types.BOOLEAN); // Show death screen
                map(Types.BOOLEAN); // Debug
                map(Types.BOOLEAN); // Flat
                handler(wrapper -> {
                    final GlobalBlockPosition lastDeathPosition = wrapper.read(Types.OPTIONAL_GLOBAL_POSITION);
                    if (lastDeathPosition != null) {
                        wrapper.user().put(new LastDeathPosition(lastDeathPosition));
                    } else {
                        wrapper.user().remove(LastDeathPosition.class);
                    }
                });
                handler(worldDataTrackerHandler(1));
                handler(playerTrackerHandler());
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                handler(wrapper -> {
                    final String dimensionKey = wrapper.read(Types.STRING);
                    final CompoundTag dimension = wrapper.user().get(DimensionRegistryStorage.class).dimension(dimensionKey);
                    if (dimension == null) {
                        throw new IllegalArgumentException("Could not find dimension " + dimensionKey + " in dimension registry");
                    }

                    wrapper.write(Types.NAMED_COMPOUND_TAG, dimension);
                });
                map(Types.STRING); // World
                map(Types.LONG); // Seed
                map(Types.UNSIGNED_BYTE); // Gamemode
                map(Types.BYTE); // Previous gamemode
                map(Types.BOOLEAN); // Debug
                map(Types.BOOLEAN); // Flat
                map(Types.BOOLEAN); // Keep player data
                handler(wrapper -> {
                    final GlobalBlockPosition lastDeathPosition = wrapper.read(Types.OPTIONAL_GLOBAL_POSITION);
                    if (lastDeathPosition != null) {
                        wrapper.user().put(new LastDeathPosition(lastDeathPosition));
                    } else {
                        wrapper.user().remove(LastDeathPosition.class);
                    }
                });
                handler(worldDataTrackerHandler(0));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19.PLAYER_INFO, wrapper -> {
            final int action = wrapper.passthrough(Types.VAR_INT);
            final int entries = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < entries; i++) {
                wrapper.passthrough(Types.UUID); // UUID
                if (action == 0) { // Add player
                    wrapper.passthrough(Types.STRING); // Player Name

                    final int properties = wrapper.passthrough(Types.VAR_INT);
                    for (int j = 0; j < properties; j++) {
                        wrapper.passthrough(Types.STRING); // Name
                        wrapper.passthrough(Types.STRING); // Value
                        wrapper.passthrough(Types.OPTIONAL_STRING); // Signature
                    }

                    wrapper.passthrough(Types.VAR_INT); // Gamemode
                    wrapper.passthrough(Types.VAR_INT); // Ping
                    wrapper.passthrough(Types.OPTIONAL_COMPONENT); // Display name

                    // Remove public profile signature
                    wrapper.read(Types.OPTIONAL_PROFILE_KEY);
                } else if (action == 1 || action == 2) { // Update gamemode/update latency
                    wrapper.passthrough(Types.VAR_INT);
                } else if (action == 3) { // Update display name
                    wrapper.passthrough(Types.OPTIONAL_COMPONENT);
                }
            }
        });
    }

    @Override
    protected void registerRewrites() {
        filter().handler((event, data) -> {
            if (data.dataType().typeId() <= Types1_18.ENTITY_DATA_TYPES.poseType.typeId()) {
                data.setDataType(Types1_18.ENTITY_DATA_TYPES.byId(data.dataType().typeId()));
            }

            final EntityDataType type = data.dataType();
            if (type == Types1_18.ENTITY_DATA_TYPES.particleType) {
                final Particle particle = (Particle) data.getValue();
                final ParticleMappings particleMappings = protocol.getMappingData().getParticleMappings();
                if (particle.id() == particleMappings.id("sculk_charge")) {
                    //TODO
                    event.cancel();
                    return;
                } else if (particle.id() == particleMappings.id("shriek")) {
                    //TODO
                    event.cancel();
                    return;
                } else if (particle.id() == particleMappings.id("vibration")) {
                    // Can't do without the position
                    event.cancel();
                    return;
                }

                rewriteParticle(event.user(), particle);
            } else if (type == Types1_18.ENTITY_DATA_TYPES.poseType) {
                final int pose = data.value();
                if (pose >= 8) {
                    // Croaking, using_tongue, roaring, sniffing, emerging, digging -> standing -> standing
                    data.setValue(0);
                }
            }
        });

        registerEntityDataTypeHandler(Types1_18.ENTITY_DATA_TYPES.itemType, null, Types1_18.ENTITY_DATA_TYPES.optionalBlockStateType, null,
            Types1_18.ENTITY_DATA_TYPES.componentType, Types1_18.ENTITY_DATA_TYPES.optionalComponentType);
        registerBlockStateHandler(EntityTypes1_19.ABSTRACT_MINECART, 11);

        filter().type(EntityTypes1_19.PAINTING).index(8).handler((event, data) -> {
            event.cancel();

            final StoredEntityData entityData = tracker(event.user()).entityDataIfPresent(event.entityId());
            final StoredPainting storedPainting = entityData.remove(StoredPainting.class);
            if (storedPainting != null) {
                final PacketWrapper packet = PacketWrapper.create(ClientboundPackets1_18.ADD_PAINTING, event.user());
                packet.write(Types.VAR_INT, storedPainting.entityId());
                packet.write(Types.UUID, storedPainting.uuid());
                packet.write(Types.VAR_INT, data.value());
                packet.write(Types.BLOCK_POSITION1_14, storedPainting.position());
                packet.write(Types.BYTE, storedPainting.direction());
                try {
                    // TODO Race condition
                    packet.send(Protocol1_19To1_18_2.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        filter().type(EntityTypes1_19.CAT).index(19).handler((event, data) -> data.setDataType(Types1_18.ENTITY_DATA_TYPES.varIntType));

        filter().type(EntityTypes1_19.FROG).cancel(16); // Age
        filter().type(EntityTypes1_19.FROG).cancel(17); // Variant
        filter().type(EntityTypes1_19.FROG).cancel(18); // Tongue target

        filter().type(EntityTypes1_19.WARDEN).cancel(16); // Anger

        filter().type(EntityTypes1_19.GOAT).cancel(18);
        filter().type(EntityTypes1_19.GOAT).cancel(19);
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();
        mapEntityTypeWithData(EntityTypes1_19.FROG, EntityTypes1_19.RABBIT).jsonName();
        mapEntityTypeWithData(EntityTypes1_19.TADPOLE, EntityTypes1_19.PUFFERFISH).jsonName();
        mapEntityTypeWithData(EntityTypes1_19.CHEST_BOAT, EntityTypes1_19.BOAT);
        mapEntityTypeWithData(EntityTypes1_19.WARDEN, EntityTypes1_19.IRON_GOLEM).jsonName();
        mapEntityTypeWithData(EntityTypes1_19.ALLAY, EntityTypes1_19.VEX).jsonName();
    }

    @Override
    public EntityType typeFromId(final int typeId) {
        return EntityTypes1_19.getTypeFromId(typeId);
    }
}
