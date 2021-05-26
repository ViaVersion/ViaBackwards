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

package com.viaversion.viabackwards.protocol.protocol1_10to1_11.packets;

import com.viaversion.viabackwards.api.entities.storage.EntityData;
import com.viaversion.viabackwards.api.entities.storage.WrappedMetadata;
import com.viaversion.viabackwards.api.rewriters.LegacyEntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_10to1_11.PotionSplashHandler;
import com.viaversion.viabackwards.protocol.protocol1_10to1_11.Protocol1_10To1_11;
import com.viaversion.viabackwards.protocol.protocol1_10to1_11.storage.ChestedHorseStorage;
import com.viaversion.viabackwards.utils.Block;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_11Types;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_12Types;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_9;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_9;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;

import java.util.List;
import java.util.Optional;

public class EntityPackets1_11 extends LegacyEntityRewriter<Protocol1_10To1_11> {

    public EntityPackets1_11(Protocol1_10To1_11 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_9_3.EFFECT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT);
                map(Type.POSITION);
                map(Type.INT);
                handler(wrapper -> {
                    int type = wrapper.get(Type.INT, 0);
                    if (type == 2002 || type == 2007) {
                        // 2007 potion id doesn't exist in 1.10
                        if (type == 2007) {
                            wrapper.set(Type.INT, 0, 2002);
                        }

                        int mappedData = PotionSplashHandler.getOldData(wrapper.get(Type.INT, 1));
                        if (mappedData != -1) {
                            wrapper.set(Type.INT, 1, mappedData);
                        }
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_9_3.SPAWN_ENTITY, new PacketRemapper() {
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

                            Block block = protocol.getBlockItemPackets().handleBlock(objType, data);
                            if (block == null)
                                return;

                            wrapper.set(Type.INT, 0, block.getId() | block.getData() << 12);
                        }
                    }
                });
            }
        });

        registerTracker(ClientboundPackets1_9_3.SPAWN_EXPERIENCE_ORB, Entity1_11Types.EntityType.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_9_3.SPAWN_GLOBAL_ENTITY, Entity1_11Types.EntityType.WEATHER);

        protocol.registerClientbound(ClientboundPackets1_9_3.SPAWN_MOB, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.UUID); // 1 - UUID
                map(Type.VAR_INT, Type.UNSIGNED_BYTE); // 2 - Entity Type
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
                        EntityType type = tracker(wrapper.user()).entityType(entityId);

                        List<Metadata> list = wrapper.get(Types1_9.METADATA_LIST, 0);
                        handleMetadata(wrapper.get(Type.VAR_INT, 0), list, wrapper.user());

                        EntityData entityData = entityDataForType(type);
                        if (entityData != null) {
                            wrapper.set(Type.UNSIGNED_BYTE, 0, (short) entityData.replacementId());
                            if (entityData.hasBaseMeta()) {
                                entityData.defaultMeta().createMeta(new WrappedMetadata(list));
                            }
                        }
                    }
                });
            }
        });

        registerTracker(ClientboundPackets1_9_3.SPAWN_PAINTING, Entity1_11Types.EntityType.PAINTING);
        registerJoinGame(ClientboundPackets1_9_3.JOIN_GAME, Entity1_11Types.EntityType.PLAYER);
        registerRespawn(ClientboundPackets1_9_3.RESPAWN);

        protocol.registerClientbound(ClientboundPackets1_9_3.SPAWN_PLAYER, new PacketRemapper() {
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

        registerRemoveEntities(ClientboundPackets1_9_3.DESTROY_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_9_3.ENTITY_METADATA, Types1_9.METADATA_LIST);

        protocol.registerClientbound(ClientboundPackets1_9_3.ENTITY_STATUS, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Entity ID
                map(Type.BYTE); // 1 - Entity Status

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        byte b = wrapper.get(Type.BYTE, 0);

                        if (b == 35) {
                            wrapper.clearPacket();
                            wrapper.setId(0x1E); // Change Game State
                            wrapper.write(Type.UNSIGNED_BYTE, (short) 10); // Play Elder Guardian animation
                            wrapper.write(Type.FLOAT, 0F);
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        // Guardian
        mapEntityTypeWithData(Entity1_11Types.EntityType.ELDER_GUARDIAN, Entity1_11Types.EntityType.GUARDIAN);
        // Skeletons
        mapEntityTypeWithData(Entity1_11Types.EntityType.WITHER_SKELETON, Entity1_11Types.EntityType.SKELETON).mobName("Wither Skeleton").spawnMetadata(storage -> storage.add(getSkeletonTypeMeta(1)));
        mapEntityTypeWithData(Entity1_11Types.EntityType.STRAY, Entity1_11Types.EntityType.SKELETON).mobName("Stray").spawnMetadata(storage -> storage.add(getSkeletonTypeMeta(2)));
        // Zombies
        mapEntityTypeWithData(Entity1_11Types.EntityType.HUSK, Entity1_11Types.EntityType.ZOMBIE).mobName("Husk").spawnMetadata(storage -> handleZombieType(storage, 6));
        mapEntityTypeWithData(Entity1_11Types.EntityType.ZOMBIE_VILLAGER, Entity1_11Types.EntityType.ZOMBIE).spawnMetadata(storage -> handleZombieType(storage, 1));
        // Horses
        mapEntityTypeWithData(Entity1_11Types.EntityType.HORSE, Entity1_11Types.EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(0))); // Nob able to ride the horse without having the MetaType sent.
        mapEntityTypeWithData(Entity1_11Types.EntityType.DONKEY, Entity1_11Types.EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(1)));
        mapEntityTypeWithData(Entity1_11Types.EntityType.MULE, Entity1_11Types.EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(2)));
        mapEntityTypeWithData(Entity1_11Types.EntityType.SKELETON_HORSE, Entity1_11Types.EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(4)));
        mapEntityTypeWithData(Entity1_11Types.EntityType.ZOMBIE_HORSE, Entity1_11Types.EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(3)));
        // New mobs
        mapEntityTypeWithData(Entity1_11Types.EntityType.EVOCATION_FANGS, Entity1_11Types.EntityType.SHULKER);
        mapEntityTypeWithData(Entity1_11Types.EntityType.EVOCATION_ILLAGER, Entity1_11Types.EntityType.VILLAGER).mobName("Evoker");
        mapEntityTypeWithData(Entity1_11Types.EntityType.VEX, Entity1_11Types.EntityType.BAT).mobName("Vex");
        mapEntityTypeWithData(Entity1_11Types.EntityType.VINDICATION_ILLAGER, Entity1_11Types.EntityType.VILLAGER).mobName("Vindicator").spawnMetadata(storage -> storage.add(new Metadata(13, MetaType1_9.VarInt, 4))); // Base Profession
        mapEntityTypeWithData(Entity1_11Types.EntityType.LIAMA, Entity1_11Types.EntityType.HORSE).mobName("Llama").spawnMetadata(storage -> storage.add(getHorseMetaType(1)));
        mapEntityTypeWithData(Entity1_11Types.EntityType.LIAMA_SPIT, Entity1_11Types.EntityType.SNOWBALL);

        mapObjectType(Entity1_11Types.ObjectType.LIAMA_SPIT, Entity1_11Types.ObjectType.SNOWBALL, -1);
        // Replace with endertorchthingies
        mapObjectType(Entity1_11Types.ObjectType.EVOCATION_FANGS, Entity1_11Types.ObjectType.FALLING_BLOCK, 198 | 1 << 12);

        // Handle ElderGuardian & target metadata
        filter().filterFamily(Entity1_11Types.EntityType.GUARDIAN).index(12).handler((event, meta) -> {
            boolean b = (boolean) meta.getValue();
            int bitmask = b ? 0x02 : 0;

            if (event.entityType() == Entity1_11Types.EntityType.ELDER_GUARDIAN) {
                bitmask |= 0x04;
            }

            meta.setTypeAndValue(MetaType1_9.Byte, (byte) bitmask);
        });

        // Handle skeleton swing
        filter().filterFamily(Entity1_11Types.EntityType.ABSTRACT_SKELETON).index(12).toIndex(13);

        /*
            ZOMBIE CHANGES
         */
        filter().filterFamily(Entity1_11Types.EntityType.ZOMBIE).handler((event, meta) -> {
            switch (meta.id()) {
                case 13:
                    event.cancel();
                    return;
                case 14:
                    event.setIndex(15);
                    break;
                case 15:
                    event.setIndex(14);
                    break;
                // Profession
                case 16:
                    event.setIndex(13);
                    meta.setValue(1 + (int) meta.getValue());
                    break;
            }
        });

        // Handle Evocation Illager
        filter().type(Entity1_11Types.EntityType.EVOCATION_ILLAGER).index(12).handler((event, meta) -> {
            event.setIndex(13);
            meta.setTypeAndValue(MetaType1_9.VarInt, ((Byte) meta.getValue()).intValue()); // Change the profession for the states
        });

        // Handle Vex (Remove this field completely since the position is not updated correctly when idling for bats. Sad ):
        filter().type(Entity1_11Types.EntityType.VEX).index(12).handler((event, meta) -> {
            meta.setValue((byte) 0x00);
        });

        // Handle VindicationIllager
        filter().type(Entity1_11Types.EntityType.VINDICATION_ILLAGER).index(12).handler((event, meta) -> {
            event.setIndex(13);
            meta.setTypeAndValue(MetaType1_9.VarInt, ((Number) meta.getValue()).intValue() == 1 ? 2 : 4);
        });

        /*
            HORSES
         */

        // Handle horse flags
        filter().filterFamily(Entity1_11Types.EntityType.ABSTRACT_HORSE).index(13).handler((event, meta) -> {
            StoredEntityData data = storedEntityData(event);
            byte b = (byte) meta.getValue();
            if (data.has(ChestedHorseStorage.class) && data.get(ChestedHorseStorage.class).isChested()) {
                b |= 0x08; // Chested
                meta.setValue(b);
            }
        });

        // Create chested horse storage
        filter().filterFamily(Entity1_11Types.EntityType.CHESTED_HORSE).handler((event, meta) -> {
            StoredEntityData data = storedEntityData(event);
            if (!data.has(ChestedHorseStorage.class)) {
                data.put(new ChestedHorseStorage());
            }
        });

        // Handle horse armor
        filter().type(Entity1_11Types.EntityType.HORSE).index(16).toIndex(17);

        // Handle chested horse
        filter().filterFamily(Entity1_11Types.EntityType.CHESTED_HORSE).index(15).handler((event, meta) -> {
            StoredEntityData data = storedEntityData(event);
            ChestedHorseStorage storage = data.get(ChestedHorseStorage.class);
            boolean b = (boolean) meta.getValue();
            storage.setChested(b);
            event.cancel();
        });

        // Get rid of Liama metadata
        filter().type(Entity1_11Types.EntityType.LIAMA).handler((event, meta) -> {
            StoredEntityData data = storedEntityData(event);
            ChestedHorseStorage storage = data.get(ChestedHorseStorage.class);

            int index = event.index();
            // Store them for later (:
            switch (index) {
                case 16:
                    storage.setLiamaStrength((int) meta.getValue());
                    event.cancel();
                    break;
                case 17:
                    storage.setLiamaCarpetColor((int) meta.getValue());
                    event.cancel();
                    break;
                case 18:
                    storage.setLiamaVariant((int) meta.getValue());
                    event.cancel();
                    break;
            }
        });

        // Handle Horse (Correct owner)
        filter().filterFamily(Entity1_11Types.EntityType.ABSTRACT_HORSE).index(14).toIndex(16);

        // Handle villager - Change non-existing profession
        filter().type(Entity1_11Types.EntityType.VILLAGER).index(13).handler((event, meta) -> {
            if ((int) meta.getValue() == 5) {
                meta.setValue(0);
            }
        });

        // handle new Shulker color meta
        filter().type(Entity1_11Types.EntityType.SHULKER).cancel(15);
    }

    /*
        0 - Skeleton
        1 - Wither Skeleton
        2 - Stray
     */

    private Metadata getSkeletonTypeMeta(int type) {
        return new Metadata(12, MetaType1_9.VarInt, type);
    }

    /*
        0 - Zombie
        1-5 - Villager with profession
        6 - Husk
     */
    private Metadata getZombieTypeMeta(int type) {
        return new Metadata(13, MetaType1_9.VarInt, type);
    }

    private void handleZombieType(WrappedMetadata storage, int type) {
        Metadata meta = storage.get(13);
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
    private Metadata getHorseMetaType(int type) {
        return new Metadata(14, MetaType1_9.VarInt, type);
    }

    @Override
    public EntityType typeFromId(int typeId) {
        return Entity1_11Types.getTypeFromId(typeId, false);
    }

    @Override
    protected EntityType getObjectTypeFromId(int typeId) {
        return Entity1_11Types.getTypeFromId(typeId, true);
    }
}
