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

package com.viaversion.viabackwards.protocol.v1_11to1_10.rewriter;

import com.viaversion.viabackwards.api.entities.storage.WrappedMetadata;
import com.viaversion.viabackwards.api.rewriters.LegacyEntityRewriter;
import com.viaversion.viabackwards.protocol.v1_11to1_10.Protocol1_11To1_10;
import com.viaversion.viabackwards.protocol.v1_11to1_10.data.SplashPotionMappings1_10;
import com.viaversion.viabackwards.protocol.v1_11to1_10.storage.ChestedHorseStorage;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_11;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_9;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_9;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;
import java.util.List;

public class EntityPacketRewriter1_11 extends LegacyEntityRewriter<ClientboundPackets1_9_3, Protocol1_11To1_10> {

    public EntityPacketRewriter1_11(Protocol1_11To1_10 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_9_3.LEVEL_EVENT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT);
                map(Types.BLOCK_POSITION1_8);
                map(Types.INT);
                handler(wrapper -> {
                    int type = wrapper.get(Types.INT, 0);
                    if (type == 2002 || type == 2007) {
                        // 2007 potion id doesn't exist in 1.10
                        if (type == 2007) {
                            wrapper.set(Types.INT, 0, 2002);
                        }

                        int mappedData = SplashPotionMappings1_10.getOldData(wrapper.get(Types.INT, 1));
                        if (mappedData != -1) {
                            wrapper.set(Types.INT, 1, mappedData);
                        }
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_9_3.ADD_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity id
                map(Types.UUID); // 1 - UUID
                map(Types.BYTE); // 2 - Type
                map(Types.DOUBLE); // 3 - x
                map(Types.DOUBLE); // 4 - y
                map(Types.DOUBLE); // 5 - z
                map(Types.BYTE); // 6 - Pitch
                map(Types.BYTE); // 7 - Yaw
                map(Types.INT); // 8 - data

                // Track Entity
                handler(getObjectTrackerHandler());
                handler(getObjectRewriter(EntityTypes1_11.ObjectType::findById));

                handler(protocol.getItemRewriter().getFallingBlockHandler());
            }
        });

        registerTracker(ClientboundPackets1_9_3.ADD_EXPERIENCE_ORB, EntityTypes1_11.EntityType.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_9_3.ADD_GLOBAL_ENTITY, EntityTypes1_11.EntityType.LIGHTNING_BOLT);

        protocol.registerClientbound(ClientboundPackets1_9_3.ADD_MOB, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity id
                map(Types.UUID); // 1 - UUID
                map(Types.VAR_INT, Types.UNSIGNED_BYTE); // 2 - Entity Type
                map(Types.DOUBLE); // 3 - X
                map(Types.DOUBLE); // 4 - Y
                map(Types.DOUBLE); // 5 - Z
                map(Types.BYTE); // 6 - Yaw
                map(Types.BYTE); // 7 - Pitch
                map(Types.BYTE); // 8 - Head Pitch
                map(Types.SHORT); // 9 - Velocity X
                map(Types.SHORT); // 10 - Velocity Y
                map(Types.SHORT); // 11 - Velocity Z
                map(Types1_9.ENTITY_DATA_LIST); // 12 - Metadata

                // Track entity
                handler(getTrackerHandler(Types.UNSIGNED_BYTE, 0));

                // Rewrite entity type / metadata
                handler(getMobSpawnRewriter(Types1_9.ENTITY_DATA_LIST));

                // Sub 1.11 clients will error if the list is empty
                handler(wrapper -> {
                    List<EntityData> metadata = wrapper.get(Types1_9.ENTITY_DATA_LIST, 0);
                    if (metadata.isEmpty()) {
                        metadata.add(new EntityData(0, EntityDataTypes1_9.BYTE, (byte) 0));
                    }
                });
            }
        });

        registerTracker(ClientboundPackets1_9_3.ADD_PAINTING, EntityTypes1_11.EntityType.PAINTING);
        registerJoinGame(ClientboundPackets1_9_3.LOGIN, EntityTypes1_11.EntityType.PLAYER);
        registerRespawn(ClientboundPackets1_9_3.RESPAWN);

        protocol.registerClientbound(ClientboundPackets1_9_3.ADD_PLAYER, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity ID
                map(Types.UUID); // 1 - Player UUID
                map(Types.DOUBLE); // 2 - X
                map(Types.DOUBLE); // 3 - Y
                map(Types.DOUBLE); // 4 - Z
                map(Types.BYTE); // 5 - Yaw
                map(Types.BYTE); // 6 - Pitch
                map(Types1_9.ENTITY_DATA_LIST); // 7 - Metadata list

                handler(getTrackerAndMetaHandler(Types1_9.ENTITY_DATA_LIST, EntityTypes1_11.EntityType.PLAYER));
                handler(wrapper -> {
                    // Sub 1.11 clients will cry if the list is empty
                    List<EntityData> metadata = wrapper.get(Types1_9.ENTITY_DATA_LIST, 0);
                    if (metadata.isEmpty()) {
                        metadata.add(new EntityData(0, EntityDataTypes1_9.BYTE, (byte) 0));
                    }
                });
            }
        });

        registerRemoveEntities(ClientboundPackets1_9_3.REMOVE_ENTITIES);
        registerSetEntityData(ClientboundPackets1_9_3.SET_ENTITY_DATA, Types1_9.ENTITY_DATA_LIST);

        protocol.registerClientbound(ClientboundPackets1_9_3.ENTITY_EVENT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // 0 - Entity ID
                map(Types.BYTE); // 1 - Entity Status

                handler(wrapper -> {
                    byte b = wrapper.get(Types.BYTE, 0);

                    if (b == 35) {
                        wrapper.clearPacket();
                        wrapper.setPacketType(ClientboundPackets1_9_3.GAME_EVENT);
                        wrapper.write(Types.UNSIGNED_BYTE, (short) 10); // Play Elder Guardian animation
                        wrapper.write(Types.FLOAT, 0F);
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        // Guardian
        mapEntityTypeWithData(EntityTypes1_11.EntityType.ELDER_GUARDIAN, EntityTypes1_11.EntityType.GUARDIAN);
        // Skeletons
        mapEntityTypeWithData(EntityTypes1_11.EntityType.WITHER_SKELETON, EntityTypes1_11.EntityType.SKELETON).spawnMetadata(storage -> storage.add(getSkeletonTypeMeta(1)));
        mapEntityTypeWithData(EntityTypes1_11.EntityType.STRAY, EntityTypes1_11.EntityType.SKELETON).plainName().spawnMetadata(storage -> storage.add(getSkeletonTypeMeta(2)));
        // Zombies
        mapEntityTypeWithData(EntityTypes1_11.EntityType.HUSK, EntityTypes1_11.EntityType.ZOMBIE).plainName().spawnMetadata(storage -> handleZombieType(storage, 6));
        mapEntityTypeWithData(EntityTypes1_11.EntityType.ZOMBIE_VILLAGER, EntityTypes1_11.EntityType.ZOMBIE).spawnMetadata(storage -> handleZombieType(storage, 1));
        // Horses
        mapEntityTypeWithData(EntityTypes1_11.EntityType.HORSE, EntityTypes1_11.EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(0))); // Nob able to ride the horse without having the MetaType sent.
        mapEntityTypeWithData(EntityTypes1_11.EntityType.DONKEY, EntityTypes1_11.EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(1)));
        mapEntityTypeWithData(EntityTypes1_11.EntityType.MULE, EntityTypes1_11.EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(2)));
        mapEntityTypeWithData(EntityTypes1_11.EntityType.SKELETON_HORSE, EntityTypes1_11.EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(4)));
        mapEntityTypeWithData(EntityTypes1_11.EntityType.ZOMBIE_HORSE, EntityTypes1_11.EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(3)));
        // New mobs
        mapEntityTypeWithData(EntityTypes1_11.EntityType.EVOKER_FANGS, EntityTypes1_11.EntityType.SHULKER);
        mapEntityTypeWithData(EntityTypes1_11.EntityType.EVOKER, EntityTypes1_11.EntityType.VILLAGER).plainName();
        mapEntityTypeWithData(EntityTypes1_11.EntityType.VEX, EntityTypes1_11.EntityType.BAT).plainName();
        mapEntityTypeWithData(EntityTypes1_11.EntityType.VINDICATOR, EntityTypes1_11.EntityType.VILLAGER).plainName().spawnMetadata(storage -> storage.add(new EntityData(13, EntityDataTypes1_9.VAR_INT, 4))); // Base Profession
        mapEntityTypeWithData(EntityTypes1_11.EntityType.LLAMA, EntityTypes1_11.EntityType.HORSE).plainName().spawnMetadata(storage -> storage.add(getHorseMetaType(1)));
        mapEntityTypeWithData(EntityTypes1_11.EntityType.LLAMA_SPIT, EntityTypes1_11.EntityType.SNOWBALL);

        mapObjectType(EntityTypes1_11.ObjectType.LLAMA_SPIT, EntityTypes1_11.ObjectType.SNOWBALL, -1);
        // Replace with endertorchthingies
        mapObjectType(EntityTypes1_11.ObjectType.EVOKER_FANGS, EntityTypes1_11.ObjectType.FALLING_BLOCK, 198 | 1 << 12);

        // Handle ElderGuardian & target metadata
        filter().type(EntityTypes1_11.EntityType.GUARDIAN).index(12).handler((event, meta) -> {
            boolean b = (boolean) meta.getValue();
            int bitmask = b ? 0x02 : 0;

            if (event.entityType() == EntityTypes1_11.EntityType.ELDER_GUARDIAN) {
                bitmask |= 0x04;
            }

            meta.setTypeAndValue(EntityDataTypes1_9.BYTE, (byte) bitmask);
        });

        // Handle skeleton swing
        filter().type(EntityTypes1_11.EntityType.ABSTRACT_SKELETON).index(12).toIndex(13);

        /*
            ZOMBIE CHANGES
         */
        filter().type(EntityTypes1_11.EntityType.ZOMBIE).handler((event, meta) -> {
            switch (meta.id()) {
                case 13 -> event.cancel();
                case 14 -> event.setIndex(15);
                case 15 -> event.setIndex(14);
                case 16 -> {
                    // Profession
                    event.setIndex(13);
                    meta.setValue(1 + (int) meta.getValue());
                }
            }
        });

        // Handle Evocation Illager
        filter().type(EntityTypes1_11.EntityType.EVOKER).index(12).handler((event, meta) -> {
            event.setIndex(13);
            meta.setTypeAndValue(EntityDataTypes1_9.VAR_INT, ((Byte) meta.getValue()).intValue()); // Change the profession for the states
        });

        // Handle Vex (Remove this field completely since the position is not updated correctly when idling for bats. Sad ):
        filter().type(EntityTypes1_11.EntityType.VEX).index(12).handler((event, meta) -> {
            meta.setValue((byte) 0x00);
        });

        // Handle VindicationIllager
        filter().type(EntityTypes1_11.EntityType.VINDICATOR).index(12).handler((event, meta) -> {
            event.setIndex(13);
            meta.setTypeAndValue(EntityDataTypes1_9.VAR_INT, ((Number) meta.getValue()).intValue() == 1 ? 2 : 4);
        });

        /*
            HORSES
         */

        // Handle horse flags
        filter().type(EntityTypes1_11.EntityType.ABSTRACT_HORSE).index(13).handler((event, meta) -> {
            StoredEntityData data = storedEntityData(event);
            byte b = (byte) meta.getValue();
            if (data.has(ChestedHorseStorage.class) && data.get(ChestedHorseStorage.class).isChested()) {
                b |= 0x08; // Chested
                meta.setValue(b);
            }
        });

        // Create chested horse storage
        filter().type(EntityTypes1_11.EntityType.CHESTED_HORSE).handler((event, meta) -> {
            StoredEntityData data = storedEntityData(event);
            if (!data.has(ChestedHorseStorage.class)) {
                data.put(new ChestedHorseStorage());
            }
        });

        // Handle horse armor
        filter().type(EntityTypes1_11.EntityType.HORSE).index(16).toIndex(17);

        // Handle chested horse
        filter().type(EntityTypes1_11.EntityType.CHESTED_HORSE).index(15).handler((event, meta) -> {
            StoredEntityData data = storedEntityData(event);
            ChestedHorseStorage storage = data.get(ChestedHorseStorage.class);
            boolean b = (boolean) meta.getValue();
            storage.setChested(b);
            event.cancel();
        });

        // Get rid of Liama metadata
        filter().type(EntityTypes1_11.EntityType.LLAMA).handler((event, meta) -> {
            StoredEntityData data = storedEntityData(event);
            ChestedHorseStorage storage = data.get(ChestedHorseStorage.class);

            int index = event.index();
            // Store them for later (:
            switch (index) {
                case 16 -> {
                    storage.setLiamaStrength((int) meta.getValue());
                    event.cancel();
                }
                case 17 -> {
                    storage.setLiamaCarpetColor((int) meta.getValue());
                    event.cancel();
                }
                case 18 -> {
                    storage.setLiamaVariant((int) meta.getValue());
                    event.cancel();
                }
            }
        });

        // Handle Horse (Correct owner)
        filter().type(EntityTypes1_11.EntityType.ABSTRACT_HORSE).index(14).toIndex(16);

        // Handle villager - Change non-existing profession
        filter().type(EntityTypes1_11.EntityType.VILLAGER).index(13).handler((event, meta) -> {
            if ((int) meta.getValue() == 5) {
                meta.setValue(0);
            }
        });

        // handle new Shulker color meta
        filter().type(EntityTypes1_11.EntityType.SHULKER).cancel(15);
    }

    /*
        0 - Skeleton
        1 - Wither Skeleton
        2 - Stray
     */

    private EntityData getSkeletonTypeMeta(int type) {
        return new EntityData(12, EntityDataTypes1_9.VAR_INT, type);
    }

    /*
        0 - Zombie
        1-5 - Villager with profession
        6 - Husk
     */
    private EntityData getZombieTypeMeta(int type) {
        return new EntityData(13, EntityDataTypes1_9.VAR_INT, type);
    }

    private void handleZombieType(WrappedMetadata storage, int type) {
        EntityData meta = storage.get(13);
        if (meta == null) {
            storage.add(getZombieTypeMeta(type));
        }
    }

    /*
        Horse 0
        Donkey 1
        Mule 2
        Zombie horse 3
        Skeleton horse 4
    */
    private EntityData getHorseMetaType(int type) {
        return new EntityData(14, EntityDataTypes1_9.VAR_INT, type);
    }

    @Override
    public EntityType typeFromId(int typeId) {
        return EntityTypes1_11.getTypeFromId(typeId, false);
    }

    @Override
    public EntityType objectTypeFromId(int typeId) {
        return EntityTypes1_11.getTypeFromId(typeId, true);
    }
}
