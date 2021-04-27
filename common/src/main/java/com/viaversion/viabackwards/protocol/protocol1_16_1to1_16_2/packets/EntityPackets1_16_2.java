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
package com.viaversion.viabackwards.protocol.protocol1_16_1to1_16_2.packets;

import com.google.common.collect.Sets;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_16_1to1_16_2.Protocol1_16_1To1_16_2;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_16Types;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_16_2Types;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.metadata.MetaType;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_14;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.Particle;
import com.viaversion.viaversion.api.type.types.version.Types1_14;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.protocol1_16to1_15_2.packets.EntityPackets;

import java.util.Set;

public class EntityPackets1_16_2 extends EntityRewriter<Protocol1_16_1To1_16_2> {

    private final Set<String> oldDimensions = Sets.newHashSet("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");

    public EntityPackets1_16_2(Protocol1_16_1To1_16_2 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerSpawnTrackerWithData(ClientboundPackets1_16_2.SPAWN_ENTITY, Entity1_16_2Types.FALLING_BLOCK);
        registerSpawnTracker(ClientboundPackets1_16_2.SPAWN_MOB);
        registerExtraTracker(ClientboundPackets1_16_2.SPAWN_EXPERIENCE_ORB, Entity1_16_2Types.EXPERIENCE_ORB);
        registerExtraTracker(ClientboundPackets1_16_2.SPAWN_PAINTING, Entity1_16_2Types.PAINTING);
        registerExtraTracker(ClientboundPackets1_16_2.SPAWN_PLAYER, Entity1_16_2Types.PLAYER);
        registerEntityDestroy(ClientboundPackets1_16_2.DESTROY_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_16_2.ENTITY_METADATA, Types1_14.METADATA_LIST);

        protocol.registerOutgoing(ClientboundPackets1_16_2.JOIN_GAME, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // Entity ID
                handler(wrapper -> {
                    boolean hardcore = wrapper.read(Type.BOOLEAN);
                    short gamemode = wrapper.read(Type.UNSIGNED_BYTE);
                    if (hardcore) {
                        gamemode |= 0x08;
                    }
                    wrapper.write(Type.UNSIGNED_BYTE, gamemode);
                });
                map(Type.BYTE); // Previous Gamemode
                map(Type.STRING_ARRAY); // World List
                handler(wrapper -> {
                    // Just screw the registry and write the defaults for 1.16 and 1.16.1 clients
                    wrapper.read(Type.NBT);
                    wrapper.write(Type.NBT, EntityPackets.DIMENSIONS_TAG);

                    CompoundTag dimensionData = wrapper.read(Type.NBT);
                    wrapper.write(Type.STRING, getDimensionFromData(dimensionData));
                });
                map(Type.STRING); // Dimension
                map(Type.LONG); // Seed
                handler(wrapper -> {
                    int maxPlayers = wrapper.read(Type.VAR_INT);
                    wrapper.write(Type.UNSIGNED_BYTE, (short) Math.max(maxPlayers, 255));
                });
                // ...
                handler(getTrackerHandler(Entity1_16_2Types.PLAYER, Type.INT));
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_16_2.RESPAWN, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    CompoundTag dimensionData = wrapper.read(Type.NBT);
                    wrapper.write(Type.STRING, getDimensionFromData(dimensionData));
                });
            }
        });
    }

    private String getDimensionFromData(CompoundTag dimensionData) {
        // This may technically break other custom dimension settings for 1.16/1.16.1 clients, so those cases are considered semi "unsupported" here
        StringTag effectsLocation = dimensionData.get("effects");
        return effectsLocation != null && oldDimensions.contains(effectsLocation.getValue()) ? effectsLocation.getValue() : "minecraft:overworld";
    }

    @Override
    protected void registerRewrites() {
        registerMetaHandler().handle(e -> {
            Metadata meta = e.getData();
            MetaType type = meta.getMetaType();
            if (type == MetaType1_14.Slot) {
                meta.setValue(protocol.getBlockItemPackets().handleItemToClient((Item) meta.getValue()));
            } else if (type == MetaType1_14.BlockID) {
                meta.setValue(protocol.getMappingData().getNewBlockStateId((int) meta.getValue()));
            } else if (type == MetaType1_14.OptChat) {
                JsonElement text = meta.getCastedValue();
                if (text != null) {
                    protocol.getTranslatableRewriter().processText(text);
                }
            } else if (type == MetaType1_14.PARTICLE) {
                rewriteParticle((Particle) meta.getValue());
            }
            return meta;
        });

        mapTypes(Entity1_16_2Types.values(), Entity1_16Types.class);
        mapEntity(Entity1_16_2Types.PIGLIN_BRUTE, Entity1_16_2Types.PIGLIN).jsonName("Piglin Brute");

        registerMetaHandler().filter(Entity1_16_2Types.ABSTRACT_PIGLIN, true).handle(meta -> {
            if (meta.getIndex() == 15) {
                meta.getData().setId(16);
            } else if (meta.getIndex() == 16) {
                meta.getData().setId(15);
            }
            return meta.getData();
        });
    }

    @Override
    protected EntityType getTypeFromId(int typeId) {
        return Entity1_16_2Types.getTypeFromId(typeId);
    }
}
