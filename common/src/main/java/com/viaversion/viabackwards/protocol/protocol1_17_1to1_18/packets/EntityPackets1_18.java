/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
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
import com.viaversion.viaversion.api.minecraft.entities.Entity1_17Types;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.FloatTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_18to1_17_1.ClientboundPackets1_18;

import java.util.ArrayList;

public final class EntityPackets1_18 extends EntityRewriter<Protocol1_17_1To1_18> {

    public EntityPackets1_18(final Protocol1_17_1To1_18 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        /*registerTrackerWithData(ClientboundPackets1_18.SPAWN_ENTITY, Entity1_18Types.FALLING_BLOCK);
        registerSpawnTracker(ClientboundPackets1_18.SPAWN_MOB);
        registerTracker(ClientboundPackets1_18.SPAWN_EXPERIENCE_ORB, Entity1_18Types.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_18.SPAWN_PAINTING, Entity1_18Types.PAINTING);
        registerTracker(ClientboundPackets1_18.SPAWN_PLAYER, Entity1_18Types.PLAYER);
        registerMetadataRewriter(ClientboundPackets1_18.ENTITY_METADATA, Types1_17.METADATA_LIST);
        registerRemoveEntities(ClientboundPackets1_18.REMOVE_ENTITIES);*/

        protocol.registerClientbound(ClientboundPackets1_18.JOIN_GAME, new PacketRemapper() {
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
                read(Type.VAR_INT); // Read simulation distance
                //handler(getTrackerHandler(Entity1_17Types.PLAYER, Type.INT)); //TODO
                handler(worldDataTrackerHandler(1));
                handler(wrapper -> {
                    final CompoundTag registry = wrapper.get(Type.NBT, 0);
                    final CompoundTag biomeRegistry = registry.get("minecraft:worldgen/biome");
                    final ListTag biomes = biomeRegistry.get("value");
                    for (final Tag biome : new ArrayList<>(biomes.getValue())) {
                        final CompoundTag biomeCompound = ((CompoundTag) biome).get("element");
                        final StringTag category = biomeCompound.get("category");
                        if (category.getValue().equals("mountain")) {
                            biomes.remove(biome);
                            continue;
                        }

                        // The client just needs something
                        biomeCompound.put("depth", new FloatTag(0.125F));
                        biomeCompound.put("scale", new FloatTag(0.05F));
                    }

                    // Track amount of biomes sent
                    tracker(wrapper.user()).setBiomesSent(biomes.size());
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_18.RESPAWN, new PacketRemapper() {
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
        // Particles have already been handled
        //registerMetaTypeHandler(MetaType1_17.ITEM, MetaType1_17.BLOCK_STATE, null, MetaType1_17.OPT_COMPONENT); //TODO correct types
    }

    @Override
    public EntityType typeFromId(final int typeId) {
        return Entity1_17Types.getTypeFromId(typeId); //TODO
    }
}
