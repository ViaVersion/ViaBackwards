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
import com.viaversion.viaversion.api.data.ParticleMappings;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_17Types;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_19Types;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.metadata.MetaType;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.Particle;
import com.viaversion.viaversion.api.type.types.version.Types1_14;
import com.viaversion.viaversion.api.type.types.version.Types1_18;
import com.viaversion.viaversion.api.type.types.version.Types1_19;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.ClientboundPackets1_19;

public final class EntityPackets1_19 extends EntityRewriter<Protocol1_18_2To1_19> {

    public EntityPackets1_19(final Protocol1_18_2To1_19 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerTrackerWithData(ClientboundPackets1_19.SPAWN_ENTITY, Entity1_19Types.FALLING_BLOCK);
        registerSpawnTracker(ClientboundPackets1_19.SPAWN_MOB);
        registerTracker(ClientboundPackets1_19.SPAWN_EXPERIENCE_ORB, Entity1_19Types.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_19.SPAWN_PAINTING, Entity1_19Types.PAINTING);
        registerTracker(ClientboundPackets1_19.SPAWN_PLAYER, Entity1_19Types.PLAYER);
        registerMetadataRewriter(ClientboundPackets1_19.ENTITY_METADATA, Types1_19.METADATA_LIST, Types1_18.METADATA_LIST);
        registerRemoveEntities(ClientboundPackets1_19.REMOVE_ENTITIES);

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
            meta.setMetaType(Types1_18.META_TYPES.byId(meta.metaType().typeId()));

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

        filter().type(Entity1_19Types.FROG).cancel(16); // Age
        filter().type(Entity1_19Types.FROG).cancel(17); // Anger
        mapEntityTypeWithData(Entity1_19Types.FROG, Entity1_19Types.PUFFERFISH).jsonName().spawnMetadata(storage -> {
            storage.add(new Metadata(17, Types1_14.META_TYPES.varIntType, 2)); // Puff state
        }).jsonName();

        mapEntityTypeWithData(Entity1_19Types.TADPOLE, Entity1_19Types.PUFFERFISH).jsonName();
        mapEntityTypeWithData(Entity1_19Types.CHEST_BOAT, Entity1_19Types.BOAT);

        filter().type(Entity1_19Types.WARDEN).cancel(16); // Anger
        mapEntityTypeWithData(Entity1_19Types.WARDEN, Entity1_19Types.IRON_GOLEM).jsonName();
    }

    @Override
    public EntityType typeFromId(final int typeId) {
        return Entity1_19Types.getTypeFromId(typeId);
    }
}
