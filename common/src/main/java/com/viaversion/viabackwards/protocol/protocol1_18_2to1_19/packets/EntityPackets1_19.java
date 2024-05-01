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
package com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.packets;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.Protocol1_18_2To1_19;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.storage.DimensionRegistryStorage;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.storage.StoredPainting;
import com.viaversion.viaversion.api.data.ParticleMappings;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19;
import com.viaversion.viaversion.api.minecraft.metadata.MetaType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_18;
import com.viaversion.viaversion.api.type.types.version.Types1_19;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.protocols.protocol1_18to1_17_1.ClientboundPackets1_18;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.ClientboundPackets1_19;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.TagUtil;

public final class EntityPackets1_19 extends EntityRewriter<ClientboundPackets1_19, Protocol1_18_2To1_19> {

    public EntityPackets1_19(final Protocol1_18_2To1_19 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerTracker(ClientboundPackets1_19.SPAWN_EXPERIENCE_ORB, EntityTypes1_19.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_19.SPAWN_PLAYER, EntityTypes1_19.PLAYER);
        registerMetadataRewriter(ClientboundPackets1_19.ENTITY_METADATA, Types1_19.METADATA_LIST, Types1_18.METADATA_LIST);
        registerRemoveEntities(ClientboundPackets1_19.REMOVE_ENTITIES);

        protocol.registerClientbound(ClientboundPackets1_19.SPAWN_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // Entity id
                map(Type.UUID); // Entity UUID
                map(Type.VAR_INT); // Entity Type
                map(Type.DOUBLE); // X
                map(Type.DOUBLE); // Y
                map(Type.DOUBLE); // Z
                map(Type.BYTE); // Pitch
                map(Type.BYTE); // Yaw
                handler(wrapper -> {
                    final byte headYaw = wrapper.read(Type.BYTE);
                    int data = wrapper.read(Type.VAR_INT);
                    final EntityType entityType = trackAndMapEntity(wrapper);
                    if (entityType.isOrHasParent(EntityTypes1_19.LIVINGENTITY)) {
                        wrapper.write(Type.BYTE, headYaw);

                        // Switch pitch and yaw position
                        final byte pitch = wrapper.get(Type.BYTE, 0);
                        final byte yaw = wrapper.get(Type.BYTE, 1);
                        wrapper.set(Type.BYTE, 0, yaw);
                        wrapper.set(Type.BYTE, 1, pitch);

                        wrapper.setPacketType(ClientboundPackets1_18.SPAWN_MOB);
                        return;
                    } else if (entityType == EntityTypes1_19.PAINTING) {
                        wrapper.cancel();
                        // The entity has been tracked, now we wait for the metadata packet
                        final int entityId = wrapper.get(Type.VAR_INT, 0);
                        final StoredEntityData entityData = tracker(wrapper.user()).entityData(entityId);
                        final Position position = new Position(wrapper.get(Type.DOUBLE, 0).intValue(), wrapper.get(Type.DOUBLE, 1).intValue(), wrapper.get(Type.DOUBLE, 2).intValue());
                        entityData.put(new StoredPainting(entityId, wrapper.get(Type.UUID, 0), position, data));
                        return;
                    }

                    if (entityType == EntityTypes1_19.FALLING_BLOCK) {
                        data = protocol.getMappingData().getNewBlockStateId(data);
                    }
                    wrapper.write(Type.INT, data);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19.ENTITY_EFFECT, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // Entity id
                map(Type.VAR_INT); // Effect id
                map(Type.BYTE); // Amplifier
                map(Type.VAR_INT); // Duration
                map(Type.BYTE); // Flags
                handler(wrapper -> {
                    // Remove factor data
                    wrapper.read(Type.OPTIONAL_NAMED_COMPOUND_TAG);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19.JOIN_GAME, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // Entity ID
                map(Type.BOOLEAN); // Hardcore
                map(Type.BYTE); // Gamemode
                map(Type.BYTE); // Previous Gamemode
                map(Type.STRING_ARRAY); // Worlds
                map(Type.NAMED_COMPOUND_TAG); // Dimension registry
                handler(wrapper -> {
                    final DimensionRegistryStorage dimensionRegistryStorage = wrapper.user().get(DimensionRegistryStorage.class);
                    dimensionRegistryStorage.clear();

                    // Cache dimensions and find current dimension
                    final String dimensionKey = Key.stripMinecraftNamespace(wrapper.read(Type.STRING));
                    final CompoundTag registry = wrapper.get(Type.NAMED_COMPOUND_TAG, 0);
                    final ListTag<CompoundTag> dimensions = TagUtil.getRegistryEntries(registry, "dimension_type");
                    boolean found = false;
                    for (final CompoundTag dimension : dimensions) {
                        final StringTag nameTag = dimension.getStringTag("name");
                        final CompoundTag dimensionData = dimension.getCompoundTag("element");
                        dimensionRegistryStorage.addDimension(nameTag.getValue(), dimensionData.copy());

                        if (!found && Key.stripMinecraftNamespace(nameTag.getValue()).equals(dimensionKey)) {
                            wrapper.write(Type.NAMED_COMPOUND_TAG, dimensionData);
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
                map(Type.STRING); // World
                map(Type.LONG); // Seed
                map(Type.VAR_INT); // Max players
                map(Type.VAR_INT); // Chunk radius
                map(Type.VAR_INT); // Simulation distance
                map(Type.BOOLEAN); // Reduced debug info
                map(Type.BOOLEAN); // Show death screen
                map(Type.BOOLEAN); // Debug
                map(Type.BOOLEAN); // Flat
                read(Type.OPTIONAL_GLOBAL_POSITION); // Read last death location
                handler(worldDataTrackerHandler(1));
                handler(playerTrackerHandler());
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                handler(wrapper -> {
                    final String dimensionKey = wrapper.read(Type.STRING);
                    final CompoundTag dimension = wrapper.user().get(DimensionRegistryStorage.class).dimension(dimensionKey);
                    if (dimension == null) {
                        throw new IllegalArgumentException("Could not find dimension " + dimensionKey + " in dimension registry");
                    }

                    wrapper.write(Type.NAMED_COMPOUND_TAG, dimension);
                });
                map(Type.STRING); // World
                map(Type.LONG); // Seed
                map(Type.UNSIGNED_BYTE); // Gamemode
                map(Type.BYTE); // Previous gamemode
                map(Type.BOOLEAN); // Debug
                map(Type.BOOLEAN); // Flat
                map(Type.BOOLEAN); // Keep player data
                read(Type.OPTIONAL_GLOBAL_POSITION); // Read last death location
                handler(worldDataTrackerHandler(0));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19.PLAYER_INFO, wrapper -> {
            final int action = wrapper.passthrough(Type.VAR_INT);
            final int entries = wrapper.passthrough(Type.VAR_INT);
            for (int i = 0; i < entries; i++) {
                wrapper.passthrough(Type.UUID); // UUID
                if (action == 0) { // Add player
                    wrapper.passthrough(Type.STRING); // Player Name

                    final int properties = wrapper.passthrough(Type.VAR_INT);
                    for (int j = 0; j < properties; j++) {
                        wrapper.passthrough(Type.STRING); // Name
                        wrapper.passthrough(Type.STRING); // Value
                        wrapper.passthrough(Type.OPTIONAL_STRING); // Signature
                    }

                    wrapper.passthrough(Type.VAR_INT); // Gamemode
                    wrapper.passthrough(Type.VAR_INT); // Ping
                    wrapper.passthrough(Type.OPTIONAL_COMPONENT); // Display name

                    // Remove public profile signature
                    wrapper.read(Type.OPTIONAL_PROFILE_KEY);
                } else if (action == 1 || action == 2) { // Update gamemode/update latency
                    wrapper.passthrough(Type.VAR_INT);
                } else if (action == 3) { // Update display name
                    wrapper.passthrough(Type.OPTIONAL_COMPONENT);
                }
            }
        });
    }

    @Override
    protected void registerRewrites() {
        filter().handler((event, meta) -> {
            if (meta.metaType().typeId() <= Types1_18.META_TYPES.poseType.typeId()) {
                meta.setMetaType(Types1_18.META_TYPES.byId(meta.metaType().typeId()));
            }

            final MetaType type = meta.metaType();
            if (type == Types1_18.META_TYPES.particleType) {
                final Particle particle = (Particle) meta.getValue();
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
            } else if (type == Types1_18.META_TYPES.poseType) {
                final int pose = meta.value();
                if (pose >= 8) {
                    // Croaking, using_tongue, roaring, sniffing, emerging, digging -> standing -> standing
                    meta.setValue(0);
                }
            }
        });

        registerMetaTypeHandler(Types1_18.META_TYPES.itemType, Types1_18.META_TYPES.blockStateType, null, null,
            Types1_18.META_TYPES.componentType, Types1_18.META_TYPES.optionalComponentType);

        filter().type(EntityTypes1_19.MINECART_ABSTRACT).index(11).handler((event, meta) -> {
            final int data = (int) meta.getValue();
            meta.setValue(protocol.getMappingData().getNewBlockStateId(data));
        });

        filter().type(EntityTypes1_19.PAINTING).index(8).handler((event, meta) -> {
            event.cancel();

            final StoredEntityData entityData = tracker(event.user()).entityDataIfPresent(event.entityId());
            final StoredPainting storedPainting = entityData.remove(StoredPainting.class);
            if (storedPainting != null) {
                final PacketWrapper packet = PacketWrapper.create(ClientboundPackets1_18.SPAWN_PAINTING, event.user());
                packet.write(Type.VAR_INT, storedPainting.entityId());
                packet.write(Type.UUID, storedPainting.uuid());
                packet.write(Type.VAR_INT, meta.value());
                packet.write(Type.POSITION1_14, storedPainting.position());
                packet.write(Type.BYTE, storedPainting.direction());
                try {
                    // TODO Race condition
                    packet.send(Protocol1_18_2To1_19.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        filter().type(EntityTypes1_19.CAT).index(19).handler((event, meta) -> meta.setMetaType(Types1_18.META_TYPES.varIntType));

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
