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
package nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.entities.storage.MetaStorage;
import nl.matsv.viabackwards.api.rewriters.LegacyEntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.Protocol1_13To1_13_1;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_13Types;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_13;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.Particle;
import com.viaversion.viaversion.api.type.types.version.Types1_13;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;

public class EntityPackets1_13_1 extends LegacyEntityRewriter<Protocol1_13To1_13_1> {

    public EntityPackets1_13_1(Protocol1_13To1_13_1 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerOutgoing(ClientboundPackets1_13.SPAWN_ENTITY, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.UUID); // 1 - UUID
                map(Type.BYTE); // 2 - Type
                map(Type.DOUBLE); // 3 - X
                map(Type.DOUBLE); // 4 - Y
                map(Type.DOUBLE); // 5 - Z
                map(Type.BYTE); // 6 - Pitch
                map(Type.BYTE); // 7 - Yaw
                map(Type.INT); // 8 - Data

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
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
                        addTrackedEntity(wrapper, entityId, entType);
                    }
                });
            }
        });

        registerExtraTracker(ClientboundPackets1_13.SPAWN_EXPERIENCE_ORB, Entity1_13Types.EntityType.EXPERIENCE_ORB);
        registerExtraTracker(ClientboundPackets1_13.SPAWN_GLOBAL_ENTITY, Entity1_13Types.EntityType.LIGHTNING_BOLT);

        protocol.registerOutgoing(ClientboundPackets1_13.SPAWN_MOB, new PacketRemapper() {
            @Override
            public void registerMap() {
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
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        MetaStorage storage = new MetaStorage(wrapper.get(Types1_13.METADATA_LIST, 0));
                        handleMeta(wrapper.user(), wrapper.get(Type.VAR_INT, 0), storage);

                        // Don't handle new ids / base meta since it's not used for this version

                        // Rewrite Metadata
                        wrapper.set(Types1_13.METADATA_LIST, 0, storage.getMetaDataList());
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_13.SPAWN_PLAYER, new PacketRemapper() {
            @Override
            public void registerMap() {
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

        registerExtraTracker(ClientboundPackets1_13.SPAWN_PAINTING, Entity1_13Types.EntityType.PAINTING);
        registerJoinGame(ClientboundPackets1_13.JOIN_GAME, Entity1_13Types.EntityType.PLAYER);
        registerRespawn(ClientboundPackets1_13.RESPAWN);
        registerEntityDestroy(ClientboundPackets1_13.DESTROY_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_13.ENTITY_METADATA, Types1_13.METADATA_LIST);
    }

    @Override
    protected void registerRewrites() {
        // Rewrite items & blocks
        registerMetaHandler().handle(e -> {
            Metadata meta = e.getData();
            if (meta.getMetaType() == MetaType1_13.Slot) {
                InventoryPackets1_13_1.toClient((Item) meta.getValue());
            } else if (meta.getMetaType() == MetaType1_13.BlockID) {
                // Convert to new block id
                int data = (int) meta.getValue();
                meta.setValue(protocol.getMappingData().getNewBlockStateId(data));
            } else if (meta.getMetaType() == MetaType1_13.PARTICLE) {
                rewriteParticle((Particle) meta.getValue());
            }
            return meta;
        });

        // Remove shooter UUID
        registerMetaHandler().
                filter(Entity1_13Types.EntityType.ABSTRACT_ARROW, true, 7)
                .removed();

        // Move colors to old position
        registerMetaHandler().filter(Entity1_13Types.EntityType.SPECTRAL_ARROW, 8)
                .handleIndexChange(7);

        // Move loyalty level to old position
        registerMetaHandler().filter(Entity1_13Types.EntityType.TRIDENT, 8)
                .handleIndexChange(7);

        // Rewrite Minecart blocks
        registerMetaHandler()
                .filter(Entity1_13Types.EntityType.MINECART_ABSTRACT, true, 9)
                .handle(e -> {
                    Metadata meta = e.getData();

                    int data = (int) meta.getValue();
                    meta.setValue(protocol.getMappingData().getNewBlockStateId(data));

                    return meta;
                });
    }

    @Override
    protected EntityType getTypeFromId(int typeId) {
        return Entity1_13Types.getTypeFromId(typeId, false);
    }

    @Override
    protected EntityType getObjectTypeFromId(final int typeId) {
        return Entity1_13Types.getTypeFromId(typeId, true);
    }
}
