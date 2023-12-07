/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2023 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_20_2to1_20_3.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_20_2to1_20_3.Protocol1_20_2To1_20_3;
import com.viaversion.viabackwards.protocol.protocol1_20_2to1_20_3.storage.SpawnPositionStorage;
import com.viaversion.viaversion.api.data.ParticleMappings;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_3;
import com.viaversion.viaversion.api.minecraft.metadata.MetaType;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_20_2;
import com.viaversion.viaversion.api.type.types.version.Types1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ClientboundConfigurationPackets1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.Protocol1_20_3To1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundPackets1_20_3;

public final class EntityPacketRewriter1_20_3 extends EntityRewriter<ClientboundPackets1_20_3, Protocol1_20_2To1_20_3> {

    public EntityPacketRewriter1_20_3(final Protocol1_20_2To1_20_3 protocol) {
        super(protocol, Types1_20_2.META_TYPES.optionalComponentType, Types1_20_2.META_TYPES.booleanType);
    }

    @Override
    public void registerPackets() {
        registerSpawnTracker(ClientboundPackets1_20_3.SPAWN_ENTITY);
        registerMetadataRewriter(ClientboundPackets1_20_3.ENTITY_METADATA, Types1_20_3.METADATA_LIST, Types1_20_2.METADATA_LIST);
        registerRemoveEntities(ClientboundPackets1_20_3.REMOVE_ENTITIES);

        protocol.registerClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_2.REGISTRY_DATA, new PacketHandlers() {
            @Override
            protected void register() {
                map(Type.COMPOUND_TAG); // Registry data
                handler(configurationDimensionDataHandler());
                handler(configurationBiomeSizeTracker());
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_3.JOIN_GAME, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // Entity id
                map(Type.BOOLEAN); // Hardcore
                map(Type.STRING_ARRAY); // World List
                map(Type.VAR_INT); // Max players
                map(Type.VAR_INT); // View distance
                map(Type.VAR_INT); // Simulation distance
                map(Type.BOOLEAN); // Reduced debug info
                map(Type.BOOLEAN); // Show death screen
                map(Type.BOOLEAN); // Limited crafting
                map(Type.STRING); // Dimension key
                map(Type.STRING); // World

                handler(spawnPositionHandler());
                handler(worldDataTrackerHandlerByKey());
            }
        });
        protocol.registerClientbound(ClientboundPackets1_20_3.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Dimension
                map(Type.STRING); // World

                handler(spawnPositionHandler());
                handler(worldDataTrackerHandlerByKey());
            }
        });
    }

    private PacketHandler spawnPositionHandler() {
        return wrapper -> {
            final String world = wrapper.get(Type.STRING, 1);
            wrapper.user().get(SpawnPositionStorage.class).setDimension(world);
        };
    }

    @Override
    protected void registerRewrites() {
        filter().handler((event, meta) -> {
            final MetaType type = meta.metaType();
            if (type == Types1_20_3.META_TYPES.componentType) {
                meta.setTypeAndValue(Types1_20_2.META_TYPES.componentType, Protocol1_20_3To1_20_2.tagComponentToJson(meta.value()));
                return;
            } else if (type == Types1_20_3.META_TYPES.optionalComponentType) {
                meta.setTypeAndValue(Types1_20_2.META_TYPES.optionalComponentType, Protocol1_20_3To1_20_2.tagComponentToJson(meta.value()));
                return;
            } else if (type == Types1_20_3.META_TYPES.particleType) {
                final Particle particle = (Particle) meta.getValue();
                final ParticleMappings particleMappings = protocol.getMappingData().getParticleMappings();
                if (particle.getId() == particleMappings.id("vibration")) {
                    // Change the type of the position source type argument
                    final int positionSourceType = particle.<Integer>removeArgument(0).getValue();
                    if (positionSourceType == 0) {
                        particle.add(0, Type.STRING, "minecraft:block");
                    } else { // Entity
                        particle.add(0, Type.STRING, "minecraft:entity");
                    }
                }

                rewriteParticle(particle);
            } else if (type == Types1_20_3.META_TYPES.poseType) {
                final int pose = meta.value();
                if (pose >= 15) {
                    event.cancel();
                }
            }

            meta.setMetaType(Types1_20_2.META_TYPES.byId(type.typeId()));
        });

        registerMetaTypeHandler(
                Types1_20_2.META_TYPES.itemType,
                Types1_20_2.META_TYPES.blockStateType,
                Types1_20_2.META_TYPES.optionalBlockStateType,
                Types1_20_2.META_TYPES.particleType,
                Types1_20_2.META_TYPES.componentType,
                Types1_20_2.META_TYPES.optionalComponentType
        );

        filter().filterFamily(EntityTypes1_20_3.MINECART_ABSTRACT).index(11).handler((event, meta) -> {
            final int blockState = meta.value();
            meta.setValue(protocol.getMappingData().getNewBlockStateId(blockState));
        });

        filter().type(EntityTypes1_20_3.TNT).removeIndex(9); // Block state
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();

        mapEntityTypeWithData(EntityTypes1_20_3.BREEZE, EntityTypes1_20_3.BLAZE).jsonName();
        mapEntityTypeWithData(EntityTypes1_20_3.WIND_CHARGE, EntityTypes1_20_3.LLAMA_SPIT).jsonName();
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_20_3.getTypeFromId(type);
    }
}