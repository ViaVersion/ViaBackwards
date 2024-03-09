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
package com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.packets;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.Protocol1_17_1To1_18;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_17;
import com.viaversion.viaversion.api.minecraft.metadata.MetaType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_17;
import com.viaversion.viaversion.api.type.types.version.Types1_18;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_18to1_17_1.ClientboundPackets1_18;

public final class EntityPackets1_18 extends EntityRewriter<ClientboundPackets1_18, Protocol1_17_1To1_18> {

    public EntityPackets1_18(final Protocol1_17_1To1_18 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerMetadataRewriter(ClientboundPackets1_18.ENTITY_METADATA, Types1_18.METADATA_LIST, Types1_17.METADATA_LIST);

        protocol.registerClientbound(ClientboundPackets1_18.JOIN_GAME, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // Entity ID
                map(Type.BOOLEAN); // Hardcore
                map(Type.BYTE); // Gamemode
                map(Type.BYTE); // Previous Gamemode
                map(Type.STRING_ARRAY); // Worlds
                map(Type.NAMED_COMPOUND_TAG); // Dimension registry
                map(Type.NAMED_COMPOUND_TAG); // Current dimension data
                map(Type.STRING); // World
                map(Type.LONG); // Seed
                map(Type.VAR_INT); // Max players
                map(Type.VAR_INT); // Chunk radius
                read(Type.VAR_INT); // Read simulation distance
                handler(worldDataTrackerHandler(1));
                handler(wrapper -> {
                    final CompoundTag registry = wrapper.get(Type.NAMED_COMPOUND_TAG, 0);
                    final CompoundTag biomeRegistry = registry.getCompoundTag("minecraft:worldgen/biome");
                    final ListTag<CompoundTag> biomes = biomeRegistry.getListTag("value", CompoundTag.class);
                    for (final CompoundTag biome : biomes) {
                        final CompoundTag biomeCompound = biome.getCompoundTag("element");
                        final StringTag category = biomeCompound.getStringTag("category");
                        if (category.getValue().equals("mountain")) {
                            category.setValue("extreme_hills");
                        }

                        // The client just needs something
                        biomeCompound.putFloat("depth", 0.125F);
                        biomeCompound.putFloat("scale", 0.05F);
                    }

                    // Track amount of biomes sent
                    tracker(wrapper.user()).setBiomesSent(biomes.size());
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_18.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.NAMED_COMPOUND_TAG); // Dimension data
                map(Type.STRING); // World
                handler(worldDataTrackerHandler(0));
            }
        });
    }

    @Override
    protected void registerRewrites() {
        filter().handler((event, meta) -> {
            meta.setMetaType(Types1_17.META_TYPES.byId(meta.metaType().typeId()));

            MetaType type = meta.metaType();
            if (type == Types1_17.META_TYPES.particleType) {
                Particle particle = meta.value();
                if (particle.getId() == 3) { // Block marker
                    Particle.ParticleData<?> data = particle.getArguments().remove(0);
                    int blockState = (int) data.getValue();
                    if (blockState == 7786) { // Light block
                        particle.setId(3);
                    } else {
                        // Else assume barrier block
                        particle.setId(2);
                    }
                    return;
                }

                rewriteParticle(particle);
            }
        });

        // Particles have already been handled
        registerMetaTypeHandler(Types1_17.META_TYPES.itemType, null, null, null,
            Types1_17.META_TYPES.componentType, Types1_17.META_TYPES.optionalComponentType);
    }

    @Override
    public EntityType typeFromId(final int typeId) {
        return EntityTypes1_17.getTypeFromId(typeId);
    }
}
