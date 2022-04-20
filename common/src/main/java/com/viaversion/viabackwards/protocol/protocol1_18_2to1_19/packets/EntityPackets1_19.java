/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2022 ViaVersion and contributors
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
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.storage.StoredPainting;
import com.viaversion.viaversion.api.data.ParticleMappings;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_17Types;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_19Types;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.metadata.MetaType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.Particle;
import com.viaversion.viaversion.api.type.types.version.Types1_18;
import com.viaversion.viaversion.api.type.types.version.Types1_19;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_18to1_17_1.ClientboundPackets1_18;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.ClientboundPackets1_19;

public final class EntityPackets1_19 extends EntityRewriter<Protocol1_18_2To1_19> {

    public EntityPackets1_19(final Protocol1_18_2To1_19 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerTracker(ClientboundPackets1_19.SPAWN_EXPERIENCE_ORB, Entity1_19Types.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_19.SPAWN_PLAYER, Entity1_19Types.PLAYER);
        registerMetadataRewriter(ClientboundPackets1_19.ENTITY_METADATA, Types1_19.METADATA_LIST, Types1_18.METADATA_LIST);
        registerRemoveEntities(ClientboundPackets1_19.REMOVE_ENTITIES);

        protocol.registerClientbound(ClientboundPackets1_19.SPAWN_ENTITY, new PacketRemapper() {
            @Override
            public void registerMap() {
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
                    final EntityType entityType = setOldEntityId(wrapper);
                    // Hope this is right
                    if (entityType.isOrHasParent(Entity1_19Types.LIVINGENTITY)) {
                        wrapper.write(Type.BYTE, headYaw);
                        wrapper.setPacketType(ClientboundPackets1_18.SPAWN_MOB);
                        return;
                    } else if (entityType == Entity1_19Types.PAINTING) {
                        wrapper.cancel();
                        // The entity has been tracked, now we wait for the metadata packet
                        final int entityId = wrapper.get(Type.VAR_INT, 0);
                        final StoredEntityData entityData = tracker(wrapper.user()).entityData(entityId);
                        final Position position = new Position(wrapper.get(Type.DOUBLE, 0).intValue(), wrapper.get(Type.DOUBLE, 1).intValue(), wrapper.get(Type.DOUBLE, 2).intValue());
                        entityData.put(new StoredPainting(entityId, wrapper.get(Type.UUID, 0), position, data));
                        return;
                    }

                    if (entityType == Entity1_19Types.FALLING_BLOCK) {
                        data = protocol.getMappingData().getNewBlockStateId(data);
                    }
                    wrapper.write(Type.INT, data);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19.ENTITY_EFFECT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Entity id
                map(Type.VAR_INT); // Effect id
                map(Type.BYTE); // Amplifier
                map(Type.VAR_INT); // Duration
                map(Type.BYTE); // Flags
                handler(wrapper -> {
                    // Remove factor data
                    if (wrapper.read(Type.BOOLEAN)) {
                        wrapper.read(Type.NBT);
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19.JOIN_GAME, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // Entity ID
                map(Type.BOOLEAN); // Hardcore
                map(Type.UNSIGNED_BYTE); // Gamemode
                map(Type.BYTE); // Previous Gamemode
                map(Type.STRING_ARRAY); // Worlds
                map(Type.NBT); // Dimension registry
                map(Type.NBT); // Current dimension data
                map(Type.STRING); // World
                map(Type.LONG); // Seed
                map(Type.VAR_INT); // Max players
                map(Type.VAR_INT); // Chunk radius
                map(Type.VAR_INT); // Read simulation distance
                handler(worldDataTrackerHandler(1));
                handler(playerTrackerHandler());
                handler(wrapper -> {
                    final CompoundTag registry = wrapper.get(Type.NBT, 0);
                    final CompoundTag biomeRegistry = registry.get("minecraft:worldgen/biome");
                    final ListTag biomes = biomeRegistry.get("value");
                    for (final Tag biome : biomes.getValue()) {
                        final CompoundTag biomeCompound = ((CompoundTag) biome).get("element");
                        biomeCompound.put("category", new StringTag("none"));
                    }

                    // Track amount of biomes sent
                    tracker(wrapper.user()).setBiomesSent(biomes.size());
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19.RESPAWN, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.NBT); // Dimension data
                map(Type.STRING); // World
                handler(worldDataTrackerHandler(0));
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
                if (particle.getId() == particleMappings.id("sculk_charge")) {
                    //TODO
                    event.cancel();
                    return;
                } else if (particle.getId() == particleMappings.id("shriek")) {
                    //TODO
                    event.cancel();
                    return;
                } else if (particle.getId() == particleMappings.id("vibration")) {
                    // Can't do without the position
                    event.cancel();
                    return;
                }

                rewriteParticle(particle);
            } else if (type == Types1_18.META_TYPES.poseType) {
                final int pose = meta.value();
                if (pose >= 8) {
                    // Croaking, using_tongue, roaring, sniffing, emerging, digging -> standing -> standing
                    meta.setValue(0);
                }
            }
        });

        registerMetaTypeHandler(Types1_18.META_TYPES.itemType, Types1_18.META_TYPES.blockStateType, null, Types1_18.META_TYPES.optionalComponentType);

        mapTypes(Entity1_19Types.values(), Entity1_17Types.class);

        filter().filterFamily(Entity1_19Types.MINECART_ABSTRACT).index(11).handler((event, meta) -> {
            final int data = (int) meta.getValue();
            meta.setValue(protocol.getMappingData().getNewBlockStateId(data));
        });

        filter().type(Entity1_19Types.PAINTING).index(8).handler((event, meta) -> {
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

        filter().type(Entity1_19Types.PLAYER).removeIndex(19); // Last death location;
        filter().type(Entity1_19Types.CAT).index(19).handler((event, meta) -> meta.setMetaType(Types1_18.META_TYPES.varIntType));

        filter().type(Entity1_19Types.FROG).cancel(16); // Age
        filter().type(Entity1_19Types.FROG).cancel(17); // Variant
        filter().type(Entity1_19Types.FROG).cancel(18); // Tongue target
        mapEntityTypeWithData(Entity1_19Types.FROG, Entity1_19Types.RABBIT).jsonName();

        mapEntityTypeWithData(Entity1_19Types.TADPOLE, Entity1_19Types.PUFFERFISH).jsonName();
        mapEntityTypeWithData(Entity1_19Types.CHEST_BOAT, Entity1_19Types.BOAT);

        filter().type(Entity1_19Types.WARDEN).cancel(16); // Anger
        mapEntityTypeWithData(Entity1_19Types.WARDEN, Entity1_19Types.IRON_GOLEM).jsonName();

        mapEntityTypeWithData(Entity1_19Types.ALLAY, Entity1_19Types.VEX).jsonName();
    }

    @Override
    public EntityType typeFromId(final int typeId) {
        return Entity1_19Types.getTypeFromId(typeId);
    }
}
