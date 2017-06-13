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

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.entities.storage.EntityData;
import nl.matsv.viabackwards.api.entities.storage.MetaStorage;
import nl.matsv.viabackwards.api.entities.types.AbstractEntityType;
import nl.matsv.viabackwards.api.entities.types.EntityType1_12;
import nl.matsv.viabackwards.api.exceptions.RemovedValueException;
import nl.matsv.viabackwards.api.rewriters.EntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_10to1_11.Protocol1_10To1_11;
import nl.matsv.viabackwards.protocol.protocol1_10to1_11.storage.ChestedHorseStorage;
import nl.matsv.viabackwards.utils.Block;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_9;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.version.Types1_9;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

import java.util.Optional;

import static nl.matsv.viabackwards.api.entities.types.EntityType1_11.*;


public class EntityPackets1_11 extends EntityRewriter<Protocol1_10To1_11> {

    @Override
    protected void registerPackets(Protocol1_10To1_11 protocol) {
        // Spawn Object
        protocol.registerOutgoing(State.PLAY, 0x00, 0x00, new PacketRemapper() {
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
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                getTypeFromId(wrapper.get(Type.BYTE, 0), true)
                        );
                    }
                });

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Optional<ObjectType> type = ObjectType.findById(wrapper.get(Type.BYTE, 0));

                        if (type.isPresent()) {
                            Optional<EntityData> optEntDat = getObjectData(type.get());
                            if (optEntDat.isPresent()) {
                                EntityData data = optEntDat.get();
                                wrapper.set(Type.BYTE, 0, ((Integer) data.getReplacementId()).byteValue());
                                if (data.getObjectData() != -1)
                                    wrapper.set(Type.INT, 0, data.getObjectData());
                            }
                        } else {
                            if (Via.getManager().isDebug()) {
                                ViaBackwards.getPlatform().getLogger().warning("Could not find Entity Type" + wrapper.get(Type.BYTE, 0));
                            }
                        }
                    }
                });

                // Handle FallingBlock blocks
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Optional<EntityType1_12.ObjectType> type = EntityType1_12.ObjectType.findById(wrapper.get(Type.BYTE, 0));
                        if (type.isPresent() && type.get().equals(EntityType1_12.ObjectType.FALLING_BLOCK)) {
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

        // Spawn Experience Orb
        protocol.registerOutgoing(State.PLAY, 0x01, 0x01, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity id

                // Track entity
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                ObjectType.THROWN_EXP_BOTTLE.getType()
                        );
                    }
                });
            }
        });

        // Spawn Global Entity
        protocol.registerOutgoing(State.PLAY, 0x02, 0x02, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.BYTE); // 1 - Type

                // Track entity
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                EntityType.WEATHER // Always thunder according to wiki.vg
                        );
                    }
                });
            }
        });

        // Spawn Mob
        protocol.registerOutgoing(State.PLAY, 0x03, 0x03, new PacketRemapper() {
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
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                getTypeFromId(wrapper.get(Type.UNSIGNED_BYTE, 0), false)
                        );
                    }
                });

                // Rewrite entity type / metadata
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int entityId = wrapper.get(Type.VAR_INT, 0);
                        AbstractEntityType type = getEntityType(wrapper.user(), entityId);

                        MetaStorage storage = new MetaStorage(wrapper.get(Types1_9.METADATA_LIST, 0));
                        handleMeta(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                storage
                        );

                        Optional<EntityData> optEntDat = getEntityData(type);
                        if (optEntDat.isPresent()) {
                            EntityData data = optEntDat.get();
                            wrapper.set(Type.UNSIGNED_BYTE, 0, ((Integer) data.getReplacementId()).shortValue());
                            if (data.hasBaseMeta())
                                data.getDefaultMeta().handle(storage);
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

        // Spawn Painting
        protocol.registerOutgoing(State.PLAY, 0x04, 0x04, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID

                // Track entity
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                EntityType.PAINTING
                        );
                    }
                });
            }
        });

        // Join game
        protocol.registerOutgoing(State.PLAY, 0x23, 0x23, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Entity ID
                map(Type.UNSIGNED_BYTE); // 1 - Gamemode
                map(Type.INT); // 2 - Dimension

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.INT, 0),
                                EntityType.PLAYER
                        );
                    }
                });

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                        int dimensionId = wrapper.get(Type.INT, 1);
                        clientWorld.setEnvironment(dimensionId);
                    }
                });
            }
        });

        // Respawn Packet (save dimension id)
        protocol.registerOutgoing(State.PLAY, 0x33, 0x33, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Dimension ID
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                        int dimensionId = wrapper.get(Type.INT, 0);
                        clientWorld.setEnvironment(dimensionId);
                    }
                });
            }
        });

        // Spawn Player
        protocol.registerOutgoing(State.PLAY, 0x05, 0x05, new PacketRemapper() {
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

                // Track Entity
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                EntityType.PLAYER
                        );
                    }
                });

                // Rewrite Metadata
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.set(
                                Types1_9.METADATA_LIST,
                                0,
                                handleMeta(
                                        wrapper.user(),
                                        wrapper.get(Type.VAR_INT, 0),
                                        new MetaStorage(wrapper.get(Types1_9.METADATA_LIST, 0))
                                ).getMetaDataList()
                        );
                    }
                });
            }
        });

        // Destroy entities
        protocol.registerOutgoing(State.PLAY, 0x30, 0x30, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT_ARRAY); // 0 - Entity IDS

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        for (int entity : wrapper.get(Type.VAR_INT_ARRAY, 0))
                            getEntityTracker(wrapper.user()).removeEntity(entity);
                    }
                });
            }
        });

        // Metadata packet
        protocol.registerOutgoing(State.PLAY, 0x39, 0x39, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Types1_9.METADATA_LIST); // 1 - Metadata list

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.set(
                                Types1_9.METADATA_LIST,
                                0,
                                handleMeta(
                                        wrapper.user(),
                                        wrapper.get(Type.VAR_INT, 0),
                                        new MetaStorage(wrapper.get(Types1_9.METADATA_LIST, 0))
                                ).getMetaDataList()
                        );
                    }
                });
            }
        });

        // Entity Status
        protocol.registerOutgoing(State.PLAY, 0x1B, 0x1B, new PacketRemapper() {
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
        regEntType(EntityType.ELDER_GUARDIAN, EntityType.GUARDIAN);
        // Skeletons
        regEntType(EntityType.WITHER_SKELETON, EntityType.SKELETON).spawnMetadata(storage -> storage.add(getSkeletonTypeMeta(1)));
        regEntType(EntityType.STRAY, EntityType.SKELETON).spawnMetadata(storage -> storage.add(getSkeletonTypeMeta(2)));
        // Zombies
        regEntType(EntityType.HUSK, EntityType.ZOMBIE).spawnMetadata(storage -> handleZombieType(storage, 6));
        regEntType(EntityType.ZOMBIE_VILLAGER, EntityType.ZOMBIE).spawnMetadata(storage -> handleZombieType(storage, 1));
        // Horses
        regEntType(EntityType.HORSE, EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(0))); // Nob able to ride the horse without having the MetaType sent.
        regEntType(EntityType.DONKEY, EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(1)));
        regEntType(EntityType.MULE, EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(2)));
        regEntType(EntityType.SKELETON_HORSE, EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(4)));
        regEntType(EntityType.ZOMBIE_HORSE, EntityType.HORSE).spawnMetadata(storage -> storage.add(getHorseMetaType(3)));
        // New mobs
        regEntType(EntityType.EVOCATION_FANGS, EntityType.SHULKER);
        regEntType(EntityType.EVOCATION_ILLAGER, EntityType.VILLAGER).mobName("Evoker");
        regEntType(EntityType.VEX, EntityType.BAT).mobName("Vex");
        regEntType(EntityType.VINDICATION_ILLAGER, EntityType.VILLAGER).mobName("Vindicator").spawnMetadata(storage -> storage.add(new Metadata(13, MetaType1_9.VarInt, 4))); // Base Profession
        regEntType(EntityType.LIAMA, EntityType.HORSE).mobName("Llama").spawnMetadata(storage -> storage.add(getHorseMetaType(1)));
        regEntType(EntityType.LIAMA_SPIT, EntityType.SNOWBALL);

        regObjType(ObjectType.LIAMA_SPIT, ObjectType.SNOWBALL, -1);
        // Replace with endertorchthingies
        regObjType(ObjectType.EVOCATION_FANGS, ObjectType.FALLING_BLOCK, 198 | 1 << 12);

        // Handle ElderGuardian & target metadata
        registerMetaHandler().filter(EntityType.GUARDIAN, true, 12).handle(e -> {
            Metadata data = e.getData();

            boolean b = (boolean) data.getValue();
            int bitmask = b ? 0x02 : 0;

            if (e.getEntity().getType().is(EntityType.ELDER_GUARDIAN))
                bitmask |= 0x04;

            data.setMetaType(MetaType1_9.Byte);
            data.setValue((byte) bitmask);

            return data;
        });

        // Handle skeleton swing
        registerMetaHandler().filter(EntityType.ABSTRACT_SKELETON, true, 12).handleIndexChange(13);

        /*
            ZOMBIE CHANGES
         */
        registerMetaHandler().filter(EntityType.ZOMBIE, true).handle(e -> {
            Metadata data = e.getData();

            switch (data.getId()) {
                case 13:
                    throw new RemovedValueException();
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
        registerMetaHandler().filter(EntityType.EVOCATION_ILLAGER, 12).handle(e -> {
            Metadata data = e.getData();
            data.setId(13);
            data.setMetaType(MetaType1_9.VarInt);
            data.setValue(((Byte) data.getValue()).intValue()); // Change the profession for the states

            return data;
        });

        // Handle Vex (Remove this field completely since the position is not updated correctly when idling for bats. Sad ):
        registerMetaHandler().filter(EntityType.VEX, 12).handle(e -> {
            Metadata data = e.getData();
            data.setValue((byte) 0x00);
            return data;
        });

        // Handle VindicationIllager
        registerMetaHandler().filter(EntityType.VINDICATION_ILLAGER, 12).handle(e -> {
            Metadata data = e.getData();
            data.setId(13);
            data.setMetaType(MetaType1_9.VarInt);

            data.setValue((int) data.getValue() == 1 ? 2 : 4);

            return data;
        });

        /*
            HORSES
         */

        // Handle horse flags
        registerMetaHandler().filter(EntityType.ABSTRACT_HORSE, true, 13).handle(e -> {
            Metadata data = e.getData();
            byte b = (byte) data.getValue();
            if (e.getEntity().has(ChestedHorseStorage.class) &&
                    e.getEntity().get(ChestedHorseStorage.class).isChested()) {
                b |= 0x08; // Chested
                data.setValue(b);
            }
            return data;
        });

        // Create chested horse storage TODO create on mob spawn?
        registerMetaHandler().filter(EntityType.CHESTED_HORSE, true).handle(e -> {
            if (!e.getEntity().has(ChestedHorseStorage.class))
                e.getEntity().put(new ChestedHorseStorage());
            return e.getData();
        });

        // Handle Horse (Correct owner)
        registerMetaHandler().filter(EntityType.ABSTRACT_HORSE, true, 14).handleIndexChange(16);

        // Handle horse armor
        registerMetaHandler().filter(EntityType.HORSE, 16).handleIndexChange(17);

        // Handle chested horse
        registerMetaHandler().filter(EntityType.CHESTED_HORSE, true, 15).handle(e -> {
            ChestedHorseStorage storage = e.getEntity().get(ChestedHorseStorage.class);
            boolean b = (boolean) e.getData().getValue();
            storage.setChested(b);

            throw new RemovedValueException();
        });

        // Get rid of Liama metadata
        registerMetaHandler().filter(EntityType.LIAMA).handle(e -> {
            Metadata data = e.getData();
            ChestedHorseStorage storage = e.getEntity().get(ChestedHorseStorage.class);

            int index = e.getIndex();
            // Store them for later (:
            switch (index) {
                case 16:
                    storage.setLiamaStrength((int) data.getValue());
                    throw new RemovedValueException();
                case 17:
                    storage.setLiamaCarpetColor((int) data.getValue());
                    throw new RemovedValueException();
                case 18:
                    storage.setLiamaVariant((int) data.getValue());
                    throw new RemovedValueException();
            }
            return e.getData();
        });

        // Handle villager - Change non-existing profession
        registerMetaHandler().filter(EntityType.VILLAGER, 13).handle(e -> {
            Metadata data = e.getData();
            if ((int) data.getValue() == 5)
                data.setValue(0);

            return data;
        });

        // handle new Shulker color meta
        registerMetaHandler().filter(EntityType.SHULKER, 15).removed();

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
        Optional<Metadata> meta = storage.get(13);

        if (!meta.isPresent())
            storage.add(getZombieTypeMeta(type));
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

}
