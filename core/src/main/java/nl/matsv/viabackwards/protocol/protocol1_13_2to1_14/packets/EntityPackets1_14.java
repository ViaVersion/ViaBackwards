package nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.entities.storage.EntityData;
import nl.matsv.viabackwards.api.entities.storage.MetaStorage;
import nl.matsv.viabackwards.api.entities.types.AbstractEntityType;
import nl.matsv.viabackwards.api.entities.types.EntityType1_13;
import nl.matsv.viabackwards.api.entities.types.EntityType1_13.EntityType;
import nl.matsv.viabackwards.api.entities.types.EntityType1_14;
import nl.matsv.viabackwards.api.rewriters.EntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets.BlockItemPackets1_13;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.Protocol1_13_2To1_14;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.data.EntityTypeMapping;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.entities.Entity1_14Types;
import us.myles.ViaVersion.api.minecraft.VillagerData;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_13_2;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_14;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.version.Types1_13_2;
import us.myles.ViaVersion.api.type.types.version.Types1_14;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.Particle;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.Protocol1_14To1_13_2;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.packets.InventoryPackets;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

import java.util.Optional;

public class EntityPackets1_14 extends EntityRewriter<Protocol1_13_2To1_14> {
    @Override
    protected void registerPackets(Protocol1_13_2To1_14 protocol) {
        // Spawn Object
        protocol.registerOutgoing(State.PLAY, 0x0, 0x0, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.UUID); // 1 - UUID
                map(Type.VAR_INT, Type.BYTE); // 2 - Type
                map(Type.DOUBLE); // 3 - X
                map(Type.DOUBLE); // 4 - Y
                map(Type.DOUBLE); // 5 - Z
                map(Type.BYTE); // 6 - Pitch
                map(Type.BYTE); // 7 - Yaw
                map(Type.INT); // 8 - Data

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        byte type = wrapper.get(Type.BYTE, 0);
                        EntityType1_14.EntityType entityType = EntityType1_14.getTypeFromId(type);
                        if (entityType == null) {
                            ViaBackwards.getPlatform().getLogger().warning("Could not find 1.14 entity type " + type);
                            return;
                        }
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                entityType
                        );
                    }
                });
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int id = wrapper.get(Type.BYTE, 0);
                        EntityType1_13.EntityType entityType  = EntityType1_13.getTypeFromId(EntityTypeMapping.getOldId(id).orElse(id), false);
                        Optional<EntityType1_13.ObjectType> type = EntityType1_13.ObjectType.fromEntityType(entityType);
                        if (type.isPresent()) {
                            wrapper.set(Type.BYTE, 0, (byte) type.get().getId());
                        }
                        if (type.isPresent() && type.get() == EntityType1_13.ObjectType.FALLING_BLOCK) {
                            int blockState = wrapper.get(Type.INT, 0);
                            int combined = BlockItemPackets1_13.toOldId(blockState);
                            combined = ((combined >> 4) & 0xFFF) | ((combined & 0xF) << 12);
                            wrapper.set(Type.INT, 0, combined);
                        } else if (type.isPresent() && type.get() == EntityType1_13.ObjectType.ITEM_FRAME) {
                            int data = wrapper.get(Type.INT, 0);
                            switch (data) {
                                case 3:
                                    data = 0;
                                    break;
                                case 4:
                                    data = 1;
                                    break;
                                case 5:
                                    data = 3;
                                    break;
                            }
                            wrapper.set(Type.INT, 0, data);
                        }
                    }
                });
            }
        });

        // Spawn mob packet
        protocol.registerOutgoing(State.PLAY, 0x3, 0x3, new PacketRemapper() {
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
                map(Types1_14.METADATA_LIST, Types1_13_2.METADATA_LIST); // 12 - Metadata

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int type = wrapper.get(Type.VAR_INT, 1);
                        EntityType1_14.EntityType entityType = EntityType1_14.getTypeFromId(type);
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                entityType
                        );
                        Optional<Integer> oldId = EntityTypeMapping.getOldId(type);
                        if (!oldId.isPresent()) {
                            if (!hasData(entityType))
                                ViaBackwards.getPlatform().getLogger().warning("Could not find 1.13.2 entity type for 1.14 entity type " + type + "/" + entityType);
                        } else {
                            wrapper.set(Type.VAR_INT, 1, oldId.get());
                        }
                    }
                });

                // Handle entity type & metadata
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int entityId = wrapper.get(Type.VAR_INT, 0);
                        AbstractEntityType type = getEntityType(wrapper.user(), entityId);

                        MetaStorage storage = new MetaStorage(wrapper.get(Types1_13_2.METADATA_LIST, 0));
                        handleMeta(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                storage
                        );

                        Optional<EntityData> optEntDat = getEntityData(type);
                        if (optEntDat.isPresent()) {
                            EntityData data = optEntDat.get();

                            Optional<Integer> replacementId = EntityTypeMapping.getOldId(data.getReplacementId());
                            wrapper.set(Type.VAR_INT, 1, replacementId.orElse(EntityType1_13.EntityType.ZOMBIE.getId()));
                            if (data.hasBaseMeta())
                                data.getDefaultMeta().handle(storage);
                        }

                        // Rewrite Metadata
                        wrapper.set(
                                Types1_13_2.METADATA_LIST,
                                0,
                                storage.getMetaDataList()
                        );
                    }
                });
            }
        });

        // Spawn painting
        protocol.registerOutgoing(State.PLAY, 0x04, 0x04, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.UUID);
                map(Type.VAR_INT);
                map(Type.POSITION1_14, Type.POSITION);
                map(Type.BYTE);
            }
        });

        // Spawn player packet
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
                map(Types1_14.METADATA_LIST, Types1_13_2.METADATA_LIST); // 7 - Metadata

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int entityId = wrapper.get(Type.VAR_INT, 0);

                        EntityType1_14.EntityType entType = EntityType1_14.EntityType.PLAYER;
                        // Register Type ID
                        addTrackedEntity(wrapper.user(), entityId, entType);
                        wrapper.set(Types1_13_2.METADATA_LIST, 0,
                                handleMeta(
                                        wrapper.user(),
                                        entityId,
                                        new MetaStorage(wrapper.get(Types1_13_2.METADATA_LIST, 0))
                                ).getMetaDataList()
                        );
                    }
                });
            }
        });

        // Destroy entities
        protocol.registerOutgoing(State.PLAY, 0x37, 0x35, new PacketRemapper() {
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
        protocol.registerOutgoing(State.PLAY, 0x43, 0x3F, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Types1_14.METADATA_LIST, Types1_13_2.METADATA_LIST); // 1 - Metadata list
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int entityId = wrapper.get(Type.VAR_INT, 0);

                        wrapper.set(Types1_13_2.METADATA_LIST, 0,
                                handleMeta(
                                        wrapper.user(),
                                        entityId,
                                        new MetaStorage(wrapper.get(Types1_13_2.METADATA_LIST, 0))
                                ).getMetaDataList()
                        );
                    }
                });
            }
        });

        //join game
        protocol.registerOutgoing(State.PLAY, 0x25, 0x25, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Entity ID
                map(Type.UNSIGNED_BYTE); // 1 - Gamemode
                map(Type.INT); // 2 - Dimension

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        // Store the player
                        ClientWorld clientChunks = wrapper.user().get(ClientWorld.class);
                        int dimensionId = wrapper.get(Type.INT, 1);
                        clientChunks.setEnvironment(dimensionId);

                        int entityId = wrapper.get(Type.INT, 0);

                        // Register Type ID
                        addTrackedEntity(wrapper.user(), entityId, EntityType1_14.EntityType.PLAYER);

                        wrapper.write(Type.UNSIGNED_BYTE, (short) 0);

                        wrapper.passthrough(Type.UNSIGNED_BYTE); // Max Players
                        wrapper.passthrough(Type.STRING); // Level Type
                        wrapper.read(Type.VAR_INT); //Read View Distance
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        regEntType(EntityType1_14.EntityType.CAT, EntityType1_14.EntityType.OCELOT).mobName("Cat").spawnMetadata(e -> {
          //  e.add(new Metadata(13, MetaType1_13_2.Byte, (byte) 0x4)); // Tamed cat
        });
        regEntType(EntityType1_14.EntityType.OCELOT, EntityType1_14.EntityType.OCELOT).mobName("Ocelot");
        regEntType(EntityType1_14.EntityType.TRADER_LLAMA, EntityType1_14.EntityType.LLAMA).mobName("Trader Llama");
        regEntType(EntityType1_14.EntityType.FOX, EntityType1_14.EntityType.WOLF).mobName("Fox");
        regEntType(EntityType1_14.EntityType.PANDA, EntityType1_14.EntityType.POLAR_BEAR).mobName("Panda");

        registerMetaHandler().filter(EntityType1_14.EntityType.FOX, 15).removed();
        registerMetaHandler().filter(EntityType1_14.EntityType.FOX, 16).removed();
        registerMetaHandler().filter(EntityType1_14.EntityType.FOX, 17).removed();
        registerMetaHandler().filter(EntityType1_14.EntityType.FOX, 18).removed();

        registerMetaHandler().filter(EntityType1_14.EntityType.PANDA, 15).removed();
        registerMetaHandler().filter(EntityType1_14.EntityType.PANDA, 16).removed();
        registerMetaHandler().filter(EntityType1_14.EntityType.PANDA, 17).removed();
        registerMetaHandler().filter(EntityType1_14.EntityType.PANDA, 18).removed();
        registerMetaHandler().filter(EntityType1_14.EntityType.PANDA, 19).removed();
        registerMetaHandler().filter(EntityType1_14.EntityType.PANDA, 20).removed();

        registerMetaHandler().handle(e -> {
            if (e.getData().getMetaType().getTypeID() == 6) { // Slot
                Protocol1_13_2To1_14.blockItem.handleItemToClient((Item) e.getData().getValue());
            }
            return e.getData();
        });
        // Remove bed location - todo send sleep packet
        registerMetaHandler().filter(EntityType1_14.EntityType.LIVINGENTITY, true, 12).removed();
        registerMetaHandler().filter(EntityType1_14.EntityType.LIVINGENTITY, true).handle(e -> {
            if (e.getIndex() > 12) e.getData().setId(e.getIndex() - 1);
            return e.getData();
        });
        // Remove entity pose
        registerMetaHandler().filter(EntityType1_14.EntityType.ENTITY, true, 6).removed();
        registerMetaHandler().filter(EntityType1_14.EntityType.ENTITY, true).handle(e -> {
            if (e.getIndex() > 6) e.getData().setId(e.getIndex() - 1);
            return e.getData();
        });
        registerMetaHandler().filter(EntityType1_14.EntityType.CAT, 17).removed();
        registerMetaHandler().filter(EntityType1_14.EntityType.CAT, 18).removed();
        registerMetaHandler().filter(EntityType1_14.EntityType.CAT, 19).removed();
        registerMetaHandler().filter(EntityType1_14.EntityType.CAT, 20).removed();
        // Villager data -> var int
        registerMetaHandler().handle(e -> {
            EntityType type = (EntityType) e.getEntity().getType();
            Metadata metadata = e.getData();
            if(e.getData().getId() > 6){
                e.getData().setValue(e.getData().getId() - 1);
            }

            //Metadata 12 added to living_entity
            if (metadata.getId() > 12 && type.isOrHasParent(EntityType.LIVINGENTITY)) {
                metadata.setId(metadata.getId() - 1);
            }
            if (type.isOrHasParent(EntityType.ABSTRACT_INSENTIENT)) { //TODO
                if (metadata.getId() == 13) {
                    tracker.setInsentientData(entityId, (byte) ((((Number) metadata.getValue()).byteValue() & ~0x4)
                            | (tracker.getInsentientData(entityId) & 0x4))); // New attacking metadata
                    metadata.setValue(tracker.getInsentientData(entityId));
                }
            }

            if (type.isOrHasParent(EntityType.PLAYER)) { //TODO
                if (entityId != e.getEntity().getClientEntityId()) {
                    if (metadata.getId() == 0) {
                        byte flags = ((Number) metadata.getValue()).byteValue();
                        // Mojang overrides the client-side pose updater, see OtherPlayerEntity#updateSize
                        tracker.setEntityFlags(entityId, flags);
                    } else if (metadata.getId() == 7) {
                        tracker.setRiptide(entityId, (((Number) metadata.getValue()).byteValue() & 0x4) != 0);
                    }
                    if (metadata.getId() == 0 || metadata.getId() == 7) {
                        metadatas.add(new Metadata(6, MetaType1_14.Pose, recalculatePlayerPose(entityId, tracker)));
                    }
                }
            } else if (type.isOrHasParent(EntityType.ZOMBIE)) { //TODO
                if (metadata.getId() == 16) {
                    tracker.setInsentientData(entityId, (byte) ((tracker.getInsentientData(entityId) & ~0x4)
                            | ((boolean) metadata.getValue() ? 0x4 : 0))); // New attacking
                    metadatas.remove(metadata);  // "Are hands held up"
                    metadatas.add(new Metadata(13, MetaType1_14.Byte, tracker.getInsentientData(entityId)));
                } else if (metadata.getId() > 16) {
                    metadata.setId(metadata.getId() - 1);
                }
            }
            if (type.isOrHasParent(EntityType.MINECART_ABSTRACT)) {
                if (metadata.getId() == 10) {
                    // New block format
                    int data = (int) metadata.getValue();
                    metadata.setValue(Protocol1_13_2To1_14.getNewBlockStateId(data));
                }
            } else if (type.is(EntityType.HORSE)) { //TODO
                if (metadata.getId() == 18) {
                    metadatas.remove(metadata);

                    int armorType = (int) metadata.getValue();
                    Item armorItem = null;
                    if (armorType == 1) {  //iron armor
                        armorItem = new Item(InventoryPackets.getNewItemId(727), (byte) 1, (short) 0, null);
                    } else if (armorType == 2) {  //gold armor
                        armorItem = new Item(InventoryPackets.getNewItemId(728), (byte) 1, (short) 0, null);
                    } else if (armorType == 3) {  //diamond armor
                        armorItem = new Item(InventoryPackets.getNewItemId(729), (byte) 1, (short) 0, null);
                    }

                    PacketWrapper equipmentPacket = new PacketWrapper(0x46, null, connection);
                    equipmentPacket.write(Type.VAR_INT, entityId);
                    equipmentPacket.write(Type.VAR_INT, 4);
                    equipmentPacket.write(Type.FLAT_VAR_INT_ITEM, armorItem);
                    equipmentPacket.send(Protocol1_14To1_13_2.class);
                }
            } else if(type.isOrHasParent(EntityType.ABSTRACT_ARROW)){
                if (metadata.getId() >= 10) { // New piercing
                    metadata.setId(metadata.getId() - 1);
                }
            } else if (type.is(EntityType.FIREWORKS_ROCKET)) { //TODO
                if (metadata.getId() == 8) {
                    if (metadata.getValue().equals(0))
                        metadata.setValue(null); // https://bugs.mojang.com/browse/MC-111480
                    metadata.setMetaType(MetaType1_14.OptVarInt);
                }
            } else if (type.isOrHasParent(EntityType.ABSTRACT_SKELETON)) { //TODO
                if (metadata.getId() == 14) {
                    tracker.setInsentientData(entityId, (byte) ((tracker.getInsentientData(entityId) & ~0x4)
                            | ((boolean) metadata.getValue() ? 0x4 : 0))); // New attacking
                    metadatas.remove(metadata);  // "Is swinging arms"
                    metadatas.add(new Metadata(13, MetaType1_14.Byte, tracker.getInsentientData(entityId)));
                }
            } else if (type.is(EntityType.AREA_EFFECT_CLOUD)) {
                if (metadata.getId() == 10) {
                    Particle particle = (Particle) metadata.getValue();
                    particle.setId(getOldParticleId(particle.getId()));
                }
            }


            if (e.getData().getValue() instanceof VillagerData) {
                e.getData().setMetaType(MetaType1_13_2.VarInt);
                e.getData().setValue(villagerDataToProfession(((VillagerData) e.getData().getValue())));
            }
            return e.getData();
        });
    }

    public int villagerDataToProfession(VillagerData data) {
        switch (data.getProfession()) {
            case 1: // Armorer
            case 10: // Mason
            case 13: // Toolsmith
            case 14: // Weaponsmith
                return 3; // Blacksmith
            case 2: // Butcher
            case 8: // Leatherworker
                return 4; // Butcher
            case 3: // Cartographer
            case 9: // Librarian
                return 1; // Librarian
            case 4: // Cleric
                return 2; // Priest
            case 5: // Farmer
            case 6: // Fisherman
            case 7: // Fletcher
            case 12: // Shepherd
                return 0; // Farmer
            case 0: // None
            case 11: // Nitwit
            default:
                return 5; // Nitwit
        }
    }

    public static int getOldParticleId(int id) {
        if (id >= 12) {
            id -= 2; // new lava drips 10, 11
        }
        if (id >= 14) {
            id -= 1; // new water drip 11 -> 13
        }
        if (id >= 28) {
            id -= 1; // new 24 -> 27
        }
        if (id >= 30) {
            id -= 1; // skip new short happy villager
        }
        if (id >= 45) {
            id -= 1; // new 39 -> 44
        }
        return id;
    }
}
