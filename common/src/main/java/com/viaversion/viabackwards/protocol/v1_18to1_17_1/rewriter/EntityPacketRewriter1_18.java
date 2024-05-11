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
package com.viaversion.viabackwards.protocol.v1_18to1_17_1.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_18to1_17_1.Protocol1_18To1_17_1;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_17;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityDataType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_17;
import com.viaversion.viaversion.api.type.types.version.Types1_18;
import com.viaversion.viaversion.protocols.v1_17_1to1_18.packet.ClientboundPackets1_18;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.util.TagUtil;

public final class EntityPacketRewriter1_18 extends EntityRewriter<ClientboundPackets1_18, Protocol1_18To1_17_1> {

    public EntityPacketRewriter1_18(final Protocol1_18To1_17_1 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerSetEntityData(ClientboundPackets1_18.SET_ENTITY_DATA, Types1_18.ENTITY_DATA_LIST, Types1_17.ENTITY_DATA_LIST);

        protocol.registerClientbound(ClientboundPackets1_18.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // Entity ID
                map(Types.BOOLEAN); // Hardcore
                map(Types.BYTE); // Gamemode
                map(Types.BYTE); // Previous Gamemode
                map(Types.STRING_ARRAY); // Worlds
                map(Types.NAMED_COMPOUND_TAG); // Dimension registry
                map(Types.NAMED_COMPOUND_TAG); // Current dimension data
                map(Types.STRING); // World
                map(Types.LONG); // Seed
                map(Types.VAR_INT); // Max players
                map(Types.VAR_INT); // Chunk radius
                read(Types.VAR_INT); // Read simulation distance
                handler(worldDataTrackerHandler(1));
                handler(wrapper -> {
                    final CompoundTag registry = wrapper.get(Types.NAMED_COMPOUND_TAG, 0);
                    final ListTag<CompoundTag> biomes = TagUtil.getRegistryEntries(registry, "worldgen/biome");
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
                map(Types.NAMED_COMPOUND_TAG); // Dimension data
                map(Types.STRING); // World
                handler(worldDataTrackerHandler(0));
            }
        });
    }

    @Override
    protected void registerRewrites() {
        filter().handler((event, meta) -> {
            meta.setDataType(Types1_17.ENTITY_DATA_TYPES.byId(meta.dataType().typeId()));

            EntityDataType type = meta.dataType();
            if (type == Types1_17.ENTITY_DATA_TYPES.particleType) {
                Particle particle = meta.value();
                if (particle.id() == 3) { // Block marker
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

                rewriteParticle(event.user(), particle);
            }
        });

        // Particles have already been handled
        registerMetaTypeHandler(Types1_17.ENTITY_DATA_TYPES.itemType, null, null, null,
            Types1_17.ENTITY_DATA_TYPES.componentType, Types1_17.ENTITY_DATA_TYPES.optionalComponentType);
    }

    @Override
    public EntityType typeFromId(final int typeId) {
        return EntityTypes1_17.getTypeFromId(typeId);
    }
}
