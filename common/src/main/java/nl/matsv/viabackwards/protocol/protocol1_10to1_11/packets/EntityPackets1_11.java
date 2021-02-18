/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_10to1_11.packets;

import nl.matsv.viabackwards.api.entities.storage.EntityData;
import nl.matsv.viabackwards.api.entities.storage.MetaStorage;
import nl.matsv.viabackwards.api.exceptions.RemovedValueException;
import nl.matsv.viabackwards.api.rewriters.LegacyEntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_10to1_11.PotionSplashHandler;
import nl.matsv.viabackwards.protocol.protocol1_10to1_11.Protocol1_10To1_11;
import nl.matsv.viabackwards.protocol.protocol1_10to1_11.storage.ChestedHorseStorage;
import nl.matsv.viabackwards.utils.Block;
import us.myles.ViaVersion.api.PacketWrapper;
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

public class EntityPackets1_11 extends LegacyEntityRewriter<Protocol1_10To1_11> {

    public EntityPackets1_11(Protocol1_10To1_11 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerOutgoing(ClientboundPackets1_9_3.EFFECT, new PacketRemapper() {
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

        registerExtraTracker(ClientboundPackets1_9_3.SPAWN_EXPERIENCE_ORB, Entity1_11Types.EntityType.EXPERIENCE_ORB);
        registerExtraTracker(ClientboundPackets1_9_3.SPAWN_GLOBAL_ENTITY, Entity1_11Types.EntityType.WEATHER);

        protocol.registerOutgoing(ClientboundPackets1_9_3.SPAWN_MOB, new PacketRemapper() {
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
                            if (entityData.hasBaseMeta()) {
                                entityData.getDefaultMeta().createMeta(storage);
                            }
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

        registerExtraTracker(ClientboundPackets1_9_3.SPAWN_PAINTING, Entity1_11Types.EntityType.PAINTING);
        registerJoinGame(ClientboundPackets1_9_3.JOIN_GAME, Entity1_11Types.EntityType.PLAYER);
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

        protocol.registerOutgoing(ClientboundPackets1_9_3.ENTITY_STATUS, new PacketRemapper() {
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
        mapEntity(Entity1_11Types.EntityType.ELDER_GUARDIAN, Entity1_11Types.EntityType.GUARDIAN);
        // Skeletons
        mapEntity(Entity1_11Types.EntityType.WITHER_SKELETON, Entity1_11Types.EntityType.SKELETON).mobName("Wither Skeleton").spawnMetadata(storage -> storage.add(getSkeletonTypeMeta(1)));
        mapEntity(Entity1_11Types.EntityType.STRAY, Entity1_11Types.EntityType.SKELETON).mobName("Stray").spawnMetadata(storage -> storage.add(getSkeletonTypeMeta(2)));
        // Zombies
        mapEntity(Entity1_11Types.EntityType.HUSK, Entity1_11Types.EntityType.ZOMBIE).mobName("Husk").spawnMetadata(storage -> handleZombieType(storage, 6));
        mapEntity(Entity1_11Types.EntityType.ZOMBIE_VILLAGER, Entity1_11Types.EntityType.ZOMBIE).spawnMetadata(storage -> handleZombieType(storage, 1));
        // Horses
        mapEntity(Entity1_11Types.EntityType.HORSE, Entity1_11Types.EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(0))); // Nob able to ride the horse without having the MetaType sent.
        mapEntity(Entity1_11Types.EntityType.DONKEY, Entity1_11Types.EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(1)));
        mapEntity(Entity1_11Types.EntityType.MULE, Entity1_11Types.EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(2)));
        mapEntity(Entity1_11Types.EntityType.SKELETON_HORSE, Entity1_11Types.EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(4)));
        mapEntity(Entity1_11Types.EntityType.ZOMBIE_HORSE, Entity1_11Types.EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(3)));
        // New mobs
        mapEntity(Entity1_11Types.EntityType.EVOCATION_FANGS, Entity1_11Types.EntityType.SHULKER);
        mapEntity(Entity1_11Types.EntityType.EVOCATION_ILLAGER, Entity1_11Types.EntityType.VILLAGER).mobName("Evoker");
        mapEntity(Entity1_11Types.EntityType.VEX, Entity1_11Types.EntityType.BAT).mobName("Vex");
        mapEntity(Entity1_11Types.EntityType.VINDICATION_ILLAGER, Entity1_11Types.EntityType.VILLAGER).mobName("Vindicator").spawnMetadata(storage -> storage.add(new Metadata(13, MetaType1_9.VarInt, 4))); // Base Profession
        mapEntity(Entity1_11Types.EntityType.LIAMA, Entity1_11Types.EntityType.HORSE).mobName("Llama").spawnMetadata(storage -> storage.add(getHorseMetaType(1)));
        mapEntity(Entity1_11Types.EntityType.LIAMA_SPIT, Entity1_11Types.EntityType.SNOWBALL);

        mapObjectType(Entity1_11Types.ObjectType.LIAMA_SPIT, Entity1_11Types.ObjectType.SNOWBALL, -1);
        // Replace with endertorchthingies
        mapObjectType(Entity1_11Types.ObjectType.EVOCATION_FANGS, Entity1_11Types.ObjectType.FALLING_BLOCK, 198 | 1 << 12);

        // Handle ElderGuardian & target metadata
        registerMetaHandler().filter(Entity1_11Types.EntityType.GUARDIAN, true, 12).handle(e -> {
            Metadata data = e.getData();

            boolean b = (boolean) data.getValue();
            int bitmask = b ? 0x02 : 0;

            if (e.getEntity().getType().is(Entity1_11Types.EntityType.ELDER_GUARDIAN))
                bitmask |= 0x04;

            data.setMetaType(MetaType1_9.Byte);
            data.setValue((byte) bitmask);

            return data;
        });

        // Handle skeleton swing
        registerMetaHandler().filter(Entity1_11Types.EntityType.ABSTRACT_SKELETON, true, 12).handleIndexChange(13);

        /*
            ZOMBIE CHANGES
         */
        registerMetaHandler().filter(Entity1_11Types.EntityType.ZOMBIE, true).handle(e -> {
            Metadata data = e.getData();

            switch (data.getId()) {
                case 13:
                    throw RemovedValueException.EX;
                case 14:
                    data.setId(15);
                    break;
                case 15:
                    data.setId(14);
                    break;
                // Profession
                case 16:
                    data.setId(13);
                    data.setValue(1 + (int) data.getValue());
                    break;
            }

            return data;
        });

        // Handle Evocation Illager
        registerMetaHandler().filter(Entity1_11Types.EntityType.EVOCATION_ILLAGER, 12).handle(e -> {
            Metadata data = e.getData();
            data.setId(13);
            data.setMetaType(MetaType1_9.VarInt);
            data.setValue(((Byte) data.getValue()).intValue()); // Change the profession for the states

            return data;
        });

        // Handle Vex (Remove this field completely since the position is not updated correctly when idling for bats. Sad ):
        registerMetaHandler().filter(Entity1_11Types.EntityType.VEX, 12).handle(e -> {
            Metadata data = e.getData();
            data.setValue((byte) 0x00);
            return data;
        });

        // Handle VindicationIllager
        registerMetaHandler().filter(Entity1_11Types.EntityType.VINDICATION_ILLAGER, 12).handle(e -> {
            Metadata data = e.getData();
            data.setId(13);
            data.setMetaType(MetaType1_9.VarInt);
            data.setValue(((Number) data.getValue()).intValue() == 1 ? 2 : 4);
            return data;
        });

        /*
            HORSES
         */

        // Handle horse flags
        registerMetaHandler().filter(Entity1_11Types.EntityType.ABSTRACT_HORSE, true, 13).handle(e -> {
            Metadata data = e.getData();
            byte b = (byte) data.getValue();
            if (e.getEntity().has(ChestedHorseStorage.class) &&
                    e.getEntity().get(ChestedHorseStorage.class).isChested()) {
                b |= 0x08; // Chested
                data.setValue(b);
            }
            return data;
        });

        // Create chested horse storage
        registerMetaHandler().filter(Entity1_11Types.EntityType.CHESTED_HORSE, true).handle(e -> {
            if (!e.getEntity().has(ChestedHorseStorage.class))
                e.getEntity().put(new ChestedHorseStorage());
            return e.getData();
        });

        // Handle horse armor
        registerMetaHandler().filter(Entity1_11Types.EntityType.HORSE, 16).handleIndexChange(17);

        // Handle chested horse
        registerMetaHandler().filter(Entity1_11Types.EntityType.CHESTED_HORSE, true, 15).handle(e -> {
            ChestedHorseStorage storage = e.getEntity().get(ChestedHorseStorage.class);
            boolean b = (boolean) e.getData().getValue();
            storage.setChested(b);

            throw RemovedValueException.EX;
        });

        // Get rid of Liama metadata
        registerMetaHandler().filter(Entity1_11Types.EntityType.LIAMA).handle(e -> {
            Metadata data = e.getData();
            ChestedHorseStorage storage = e.getEntity().get(ChestedHorseStorage.class);

            int index = e.getIndex();
            // Store them for later (:
            switch (index) {
                case 16:
                    storage.setLiamaStrength((int) data.getValue());
                    throw RemovedValueException.EX;
                case 17:
                    storage.setLiamaCarpetColor((int) data.getValue());
                    throw RemovedValueException.EX;
                case 18:
                    storage.setLiamaVariant((int) data.getValue());
                    throw RemovedValueException.EX;
            }
            return e.getData();
        });

        // Handle Horse (Correct owner)
        registerMetaHandler().filter(Entity1_11Types.EntityType.ABSTRACT_HORSE, true, 14).handleIndexChange(16);

        // Handle villager - Change non-existing profession
        registerMetaHandler().filter(Entity1_11Types.EntityType.VILLAGER, 13).handle(e -> {
            Metadata data = e.getData();
            if ((int) data.getValue() == 5)
                data.setValue(0);

            return data;
        });

        // handle new Shulker color meta
        registerMetaHandler().filter(Entity1_11Types.EntityType.SHULKER, 15).removed();

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

    private void handleZombieType(MetaStorage storage, int type) {
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
    protected EntityType getTypeFromId(int typeId) {
        return Entity1_11Types.getTypeFromId(typeId, false);
    }

    @Override
    protected EntityType getObjectTypeFromId(int typeId) {
        return Entity1_11Types.getTypeFromId(typeId, true);
    }
}
