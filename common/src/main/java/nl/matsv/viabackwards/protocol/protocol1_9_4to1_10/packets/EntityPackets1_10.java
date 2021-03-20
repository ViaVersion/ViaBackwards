/*
 * ViaBackwards - https://github.com/ViaVersion/ViaBackwards
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

package nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.packets;

import nl.matsv.viabackwards.api.entities.storage.EntityData;
import nl.matsv.viabackwards.api.entities.storage.MetaStorage;
import nl.matsv.viabackwards.api.exceptions.RemovedValueException;
import nl.matsv.viabackwards.api.rewriters.LegacyEntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.Protocol1_9_4To1_10;
import nl.matsv.viabackwards.utils.Block;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.entities.Entity1_10Types;
import us.myles.ViaVersion.api.entities.Entity1_11Types;
import us.myles.ViaVersion.api.entities.Entity1_12Types;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_9;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.version.Types1_9;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;

import java.util.Optional;

public class EntityPackets1_10 extends LegacyEntityRewriter<Protocol1_9_4To1_10> {

    public EntityPackets1_10(Protocol1_9_4To1_10 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerOutgoing(ClientboundPackets1_9_3.SPAWN_ENTITY, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.UUID); // 1 - UUID
                map(Type.BYTE); // 2 - Type
                map(Type.DOUBLE); // 3 - x
                map(Type.DOUBLE); // 4 - y
                map(Type.DOUBLE); // 5 - z
                map(Type.BYTE); // 6 - Pitch
                map(Type.BYTE); // 7 - Yaw
                map(Type.INT); // 8 - data

                // Track Entity
                handler(getObjectTrackerHandler());
                handler(getObjectRewriter(id -> Entity1_11Types.ObjectType.findById(id).orElse(null)));

                // Handle FallingBlock blocks
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Optional<Entity1_12Types.ObjectType> type = Entity1_12Types.ObjectType.findById(wrapper.get(Type.BYTE, 0));
                        if (type.isPresent() && type.get() == Entity1_12Types.ObjectType.FALLING_BLOCK) {
                            int objectData = wrapper.get(Type.INT, 0);
                            int objType = objectData & 4095;
                            int data = objectData >> 12 & 15;

                            Block block = getProtocol().getBlockItemPackets().handleBlock(objType, data);
                            if (block == null)
                                return;

                            wrapper.set(Type.INT, 0, block.getId() | block.getData() << 12);
                        }
                    }
                });
            }
        });

        registerExtraTracker(ClientboundPackets1_9_3.SPAWN_EXPERIENCE_ORB, Entity1_10Types.EntityType.EXPERIENCE_ORB);
        registerExtraTracker(ClientboundPackets1_9_3.SPAWN_GLOBAL_ENTITY, Entity1_10Types.EntityType.WEATHER);

        protocol.registerOutgoing(ClientboundPackets1_9_3.SPAWN_MOB, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.UUID); // 1 - UUID
                map(Type.UNSIGNED_BYTE); // 2 - Entity Type
                map(Type.DOUBLE); // 3 - X
                map(Type.DOUBLE); // 4 - Y
                map(Type.DOUBLE); // 5 - Z
                map(Type.BYTE); // 6 - Yaw
                map(Type.BYTE); // 7 - Pitch
                map(Type.BYTE); // 8 - Head Pitch
                map(Type.SHORT); // 9 - Velocity X
                map(Type.SHORT); // 10 - Velocity Y
                map(Type.SHORT); // 11 - Velocity Z
                map(Types1_9.METADATA_LIST); // 12 - Metadata

                // Track entity
                handler(getTrackerHandler(Type.UNSIGNED_BYTE, 0));

                // Rewrite entity type / metadata
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int entityId = wrapper.get(Type.VAR_INT, 0);
                        EntityType type = getEntityType(wrapper.user(), entityId);

                        MetaStorage storage = new MetaStorage(wrapper.get(Types1_9.METADATA_LIST, 0));
                        handleMeta(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                storage
                        );

                        EntityData entityData = getEntityData(type);
                        if (entityData != null) {
                            wrapper.set(Type.UNSIGNED_BYTE, 0, (short) entityData.getReplacementId());
                            if (entityData.hasBaseMeta())
                                entityData.getDefaultMeta().createMeta(storage);
                        }

                        // Rewrite Metadata
                        wrapper.set(
                                Types1_9.METADATA_LIST,
                                0,
                                storage.getMetaDataList()
                        );
                    }
                });

            }
        });

        registerExtraTracker(ClientboundPackets1_9_3.SPAWN_PAINTING, Entity1_10Types.EntityType.PAINTING);
        registerJoinGame(ClientboundPackets1_9_3.JOIN_GAME, Entity1_10Types.EntityType.PLAYER);
        registerRespawn(ClientboundPackets1_9_3.RESPAWN);

        protocol.registerOutgoing(ClientboundPackets1_9_3.SPAWN_PLAYER, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.UUID); // 1 - Player UUID
                map(Type.DOUBLE); // 2 - X
                map(Type.DOUBLE); // 3 - Y
                map(Type.DOUBLE); // 4 - Z
                map(Type.BYTE); // 5 - Yaw
                map(Type.BYTE); // 6 - Pitch
                map(Types1_9.METADATA_LIST); // 7 - Metadata list

                handler(getTrackerAndMetaHandler(Types1_9.METADATA_LIST, Entity1_11Types.EntityType.PLAYER));
            }
        });

        registerEntityDestroy(ClientboundPackets1_9_3.DESTROY_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_9_3.ENTITY_METADATA, Types1_9.METADATA_LIST);
    }

    @Override
    protected void registerRewrites() {
        mapEntity(Entity1_10Types.EntityType.POLAR_BEAR, Entity1_10Types.EntityType.SHEEP).mobName("Polar Bear");

        // Change the sheep color when the polar bear is standing up (index 13 -> Standing up)
        registerMetaHandler().filter(Entity1_10Types.EntityType.POLAR_BEAR, 13).handle((e -> {
            Metadata data = e.getData();
            boolean b = (boolean) data.getValue();

            data.setMetaType(MetaType1_9.Byte);
            data.setValue(b ? (byte) (14 & 0x0F) : (byte) (0));

            return data;
        }));


        // Handle husk (index 13 -> Zombie Type)
        registerMetaHandler().filter(Entity1_10Types.EntityType.ZOMBIE, 13).handle(e -> {
            Metadata data = e.getData();

            if ((int) data.getValue() == 6) // Is type Husk
                data.setValue(0);

            return data;
        });

        // Handle Stray (index 12 -> Skeleton Type)
        registerMetaHandler().filter(Entity1_10Types.EntityType.SKELETON, 12).handle(e -> {
            Metadata data = e.getData();

            if ((int) data.getValue() == 2)
                data.setValue(0); // Change to default skeleton

            return data;
        });

        // Handle the missing NoGravity tag for every metadata
        registerMetaHandler().handle(e -> {
            Metadata data = e.getData();

            if (data.getId() == 5)
                throw RemovedValueException.EX;
            else if (data.getId() >= 5)
                data.setId(data.getId() - 1);

            return data;
        });
    }

    @Override
    protected EntityType getTypeFromId(int typeId) {
        return Entity1_10Types.getTypeFromId(typeId, false);
    }

    @Override
    protected EntityType getObjectTypeFromId(int typeId) {
        return Entity1_10Types.getTypeFromId(typeId, true);
    }
}
