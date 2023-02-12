/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_13to1_13_1.packets;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.rewriters.LegacyEntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_13to1_13_1.Protocol1_13To1_13_1;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_13Types;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.Particle;
import com.viaversion.viaversion.api.type.types.version.Types1_13;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;
import java.util.List;

public class EntityPackets1_13_1 extends LegacyEntityRewriter<ClientboundPackets1_13, Protocol1_13To1_13_1> {

    public EntityPackets1_13_1(Protocol1_13To1_13_1 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_13.SPAWN_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.UUID); // 1 - UUID
                map(Type.BYTE); // 2 - Type
                map(Type.DOUBLE); // 3 - X
                map(Type.DOUBLE); // 4 - Y
                map(Type.DOUBLE); // 5 - Z
                map(Type.BYTE); // 6 - Pitch
                map(Type.BYTE); // 7 - Yaw
                map(Type.INT); // 8 - Data

                handler(wrapper -> {
                    int entityId = wrapper.get(Type.VAR_INT, 0);
                    byte type = wrapper.get(Type.BYTE, 0);
                    Entity1_13Types.EntityType entType = Entity1_13Types.getTypeFromId(type, true);
                    if (entType == null) {
                        ViaBackwards.getPlatform().getLogger().warning("Could not find 1.13 entity type " + type);
                        return;
                    }

                    // Rewrite falling block
                    if (entType.is(Entity1_13Types.EntityType.FALLING_BLOCK)) {
                        int data = wrapper.get(Type.INT, 0);
                        wrapper.set(Type.INT, 0, protocol.getMappingData().getNewBlockStateId(data));
                    }

                    // Track Entity
                    tracker(wrapper.user()).addEntity(entityId, entType);
                });
            }
        });

        registerTracker(ClientboundPackets1_13.SPAWN_EXPERIENCE_ORB, Entity1_13Types.EntityType.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_13.SPAWN_GLOBAL_ENTITY, Entity1_13Types.EntityType.LIGHTNING_BOLT);

        protocol.registerClientbound(ClientboundPackets1_13.SPAWN_MOB, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.UUID); // 1 - Entity UUID
                map(Type.VAR_INT); // 2 - Entity Type
                map(Type.DOUBLE); // 3 - X
                map(Type.DOUBLE); // 4 - Y
                map(Type.DOUBLE); // 5 - Z
                map(Type.BYTE); // 6 - Yaw
                map(Type.BYTE); // 7 - Pitch
                map(Type.BYTE); // 8 - Head Pitch
                map(Type.SHORT); // 9 - Velocity X
                map(Type.SHORT); // 10 - Velocity Y
                map(Type.SHORT); // 11 - Velocity Z
                map(Types1_13.METADATA_LIST); // 12 - Metadata

                // Track Entity
                handler(getTrackerHandler());

                // Rewrite Metadata
                handler(wrapper -> {
                    List<Metadata> metadata = wrapper.get(Types1_13.METADATA_LIST, 0);
                    handleMetadata(wrapper.get(Type.VAR_INT, 0), metadata, wrapper.user());
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.SPAWN_PLAYER, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.UUID); // 1 - Player UUID
                map(Type.DOUBLE); // 2 - X
                map(Type.DOUBLE); // 3 - Y
                map(Type.DOUBLE); // 4 - Z
                map(Type.BYTE); // 5 - Yaw
                map(Type.BYTE); // 6 - Pitch
                map(Types1_13.METADATA_LIST); // 7 - Metadata

                handler(getTrackerAndMetaHandler(Types1_13.METADATA_LIST, Entity1_13Types.EntityType.PLAYER));
            }
        });

        registerTracker(ClientboundPackets1_13.SPAWN_PAINTING, Entity1_13Types.EntityType.PAINTING);
        registerJoinGame(ClientboundPackets1_13.JOIN_GAME, Entity1_13Types.EntityType.PLAYER);
        registerRespawn(ClientboundPackets1_13.RESPAWN);
        registerRemoveEntities(ClientboundPackets1_13.DESTROY_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_13.ENTITY_METADATA, Types1_13.METADATA_LIST);
    }

    @Override
    protected void registerRewrites() {
        // Rewrite items & blocks
        filter().handler((event, meta) -> {
            if (meta.metaType() == Types1_13.META_TYPES.itemType) {
                protocol.getItemRewriter().handleItemToClient((Item) meta.getValue());
            } else if (meta.metaType() == Types1_13.META_TYPES.blockStateType) {
                // Convert to new block id
                int data = (int) meta.getValue();
                meta.setValue(protocol.getMappingData().getNewBlockStateId(data));
            } else if (meta.metaType() == Types1_13.META_TYPES.particleType) {
                rewriteParticle((Particle) meta.getValue());
            }
        });

        // Remove shooter UUID
        filter().filterFamily(Entity1_13Types.EntityType.ABSTRACT_ARROW).cancel(7);

        // Move colors to old position
        filter().type(Entity1_13Types.EntityType.SPECTRAL_ARROW).index(8).toIndex(7);

        // Move loyalty level to old position
        filter().type(Entity1_13Types.EntityType.TRIDENT).index(8).toIndex(7);

        // Rewrite Minecart blocks
        filter().filterFamily(Entity1_13Types.EntityType.MINECART_ABSTRACT).index(9).handler((event, meta) -> {
            int data = (int) meta.getValue();
            meta.setValue(protocol.getMappingData().getNewBlockStateId(data));
        });
    }

    @Override
    public EntityType typeFromId(int typeId) {
        return Entity1_13Types.getTypeFromId(typeId, false);
    }

    @Override
    protected EntityType getObjectTypeFromId(final int typeId) {
        return Entity1_13Types.getTypeFromId(typeId, true);
    }
}
