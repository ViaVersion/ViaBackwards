package nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.entities.meta.MetaHandler;
import nl.matsv.viabackwards.api.entities.storage.EntityData;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.api.exceptions.RemovedValueException;
import nl.matsv.viabackwards.api.rewriters.EntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.Protocol1_13_2To1_14;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.data.EntityPositionStorage;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.data.EntityTypeMapping;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.storage.ChunkLightStorage;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.entities.Entity1_13Types;
import us.myles.ViaVersion.api.entities.Entity1_14Types;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.minecraft.Position;
import us.myles.ViaVersion.api.minecraft.VillagerData;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.MetaType;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_13_2;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.Particle;
import us.myles.ViaVersion.api.type.types.version.Types1_13_2;
import us.myles.ViaVersion.api.type.types.version.Types1_14;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

import java.util.Optional;

public class EntityPackets1_14 extends EntityRewriter<Protocol1_13_2To1_14> {

    private static final double RELATIVE_MOVE_FACTOR = 32 * 128;

    @Override
    protected void addTrackedEntity(PacketWrapper wrapper, int entityId, EntityType type) throws Exception {
        super.addTrackedEntity(wrapper, entityId, type);

        // Cache the position for every newly tracked entity
        if (type == Entity1_14Types.EntityType.PAINTING) {
            final Position position = wrapper.get(Type.POSITION, 0);
            cacheEntityPosition(wrapper, position.getX(), position.getY(), position.getZ(), true, false);
        } else if (wrapper.getId() != 0x25) { // ignore join game
            cacheEntityPosition(wrapper, true, false);
        }
    }

    @Override
    protected void registerPackets(Protocol1_13_2To1_14 protocol) {
        // Entity teleport
        protocol.registerOutgoing(State.PLAY, 0x56, 0x50, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                handler(wrapper -> cacheEntityPosition(wrapper, false, false));
            }
        });

        // Entity relative move + Entity look and relative move
        PacketRemapper relativeMoveHandler = new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.SHORT);
                map(Type.SHORT);
                map(Type.SHORT);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        double x = wrapper.get(Type.SHORT, 0) / RELATIVE_MOVE_FACTOR;
                        double y = wrapper.get(Type.SHORT, 1) / RELATIVE_MOVE_FACTOR;
                        double z = wrapper.get(Type.SHORT, 2) / RELATIVE_MOVE_FACTOR;
                        cacheEntityPosition(wrapper, x, y, z, false, true);
                    }
                });
            }
        };
        protocol.registerOutgoing(State.PLAY, 0x28, 0x28, relativeMoveHandler);
        protocol.registerOutgoing(State.PLAY, 0x29, 0x29, relativeMoveHandler);

        // Spawn Object
        protocol.registerOutgoing(State.PLAY, 0x00, 0x00, new PacketRemapper() {
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
                map(Type.SHORT); // 9 - Velocity X
                map(Type.SHORT); // 10 - Velocity Y
                map(Type.SHORT); // 11 - Velocity Z

                handler(getObjectTrackerHandler());

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int id = wrapper.get(Type.BYTE, 0);
                        Entity1_13Types.EntityType entityType = Entity1_13Types.getTypeFromId(EntityTypeMapping.getOldId(id).orElse(id), false);
                        Entity1_13Types.ObjectType objectType;
                        if (entityType.isOrHasParent(Entity1_13Types.EntityType.MINECART_ABSTRACT)) {
                            objectType = Entity1_13Types.ObjectType.MINECART;
                            int data = 0;
                            switch (entityType) {
                                case CHEST_MINECART:
                                    data = 1;
                                    break;
                                case FURNACE_MINECART:
                                    data = 2;
                                    break;
                                case TNT_MINECART:
                                    data = 3;
                                    break;
                                case SPAWNER_MINECART:
                                    data = 4;
                                    break;
                                case HOPPER_MINECART:
                                    data = 5;
                                    break;
                                case COMMANDBLOCK_MINECART:
                                    data = 6;
                                    break;
                            }
                            if (data != 0)
                                wrapper.set(Type.INT, 0, data);
                        } else {
                            objectType = Entity1_13Types.ObjectType.fromEntityType(entityType).orElse(null);
                        }

                        if (objectType == null) return;

                        wrapper.set(Type.BYTE, 0, (byte) objectType.getId());

                        int data = wrapper.get(Type.INT, 0);
                        if (objectType == Entity1_13Types.ObjectType.FALLING_BLOCK) {
                            int blockState = wrapper.get(Type.INT, 0);
                            int combined = Protocol1_13_2To1_14.getNewBlockStateId(blockState);
                            wrapper.set(Type.INT, 0, combined);
                        } else if (entityType.isOrHasParent(Entity1_13Types.EntityType.ABSTRACT_ARROW)) {
                            wrapper.set(Type.INT, 0, data + 1);
                        }
                    }
                });
            }
        });

        // Spawn mob packet
        protocol.registerOutgoing(State.PLAY, 0x03, 0x03, new PacketRemapper() {
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
                        Entity1_14Types.EntityType entityType = Entity1_14Types.getTypeFromId(type);
                        addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), entityType);

                        Optional<Integer> oldId = EntityTypeMapping.getOldId(type);
                        if (!oldId.isPresent()) {
                            Optional<EntityData> oldType = getEntityData(entityType);
                            if (!oldType.isPresent()) {
                                ViaBackwards.getPlatform().getLogger().warning("Could not find 1.13.2 entity type for 1.14 entity type " + type + "/" + entityType);
                                wrapper.cancel();
                            } else {
                                wrapper.set(Type.VAR_INT, 1, oldType.get().getReplacementId());
                            }
                        } else {
                            wrapper.set(Type.VAR_INT, 1, oldId.get());
                        }
                    }
                });

                // Handle entity type & metadata
                handler(getMobSpawnRewriter(Types1_13_2.METADATA_LIST));
            }
        });

        // Spawn Experience Orb
        getProtocol().registerOutgoing(State.PLAY, 0x01, 0x01, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.DOUBLE); // Needs to be mapped for the position cache
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), Entity1_14Types.EntityType.XP_ORB);
                    }
                });
            }
        });

        // Spawn Global Object
        getProtocol().registerOutgoing(State.PLAY, 0x02, 0x02, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.DOUBLE); // Needs to be mapped for the position cache
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), Entity1_14Types.EntityType.LIGHTNING_BOLT);
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

                // Track entity
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), Entity1_14Types.EntityType.PAINTING);
                    }
                });
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

                handler(getTrackerAndMetaHandler(Types1_13_2.METADATA_LIST, Entity1_14Types.EntityType.PLAYER));
                handler(wrapper -> cacheEntityPosition(wrapper, true, false));
            }
        });

        // Destroy entities
        registerEntityDestroy(0x37, 0x35);

        // Entity Metadata packet
        registerMetadataRewriter(0x43, 0x3F, Types1_14.METADATA_LIST, Types1_13_2.METADATA_LIST);

        // Join game
        protocol.registerOutgoing(State.PLAY, 0x25, 0x25, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Entity ID
                map(Type.UNSIGNED_BYTE); // 1 - Gamemode
                map(Type.INT); // 2 - Dimension

                handler(getTrackerHandler(Entity1_14Types.EntityType.PLAYER, Type.INT));
                handler(getDimensionHandler(1));
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.write(Type.UNSIGNED_BYTE, (short) 0);

                        wrapper.passthrough(Type.UNSIGNED_BYTE); // Max Players
                        wrapper.passthrough(Type.STRING); // Level Type
                        wrapper.read(Type.VAR_INT); // Read View Distance
                    }
                });
            }
        });

        // Respawn
        protocol.registerOutgoing(State.PLAY, 0x3A, 0x38, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Dimension ID

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                        int dimensionId = wrapper.get(Type.INT, 0);
                        clientWorld.setEnvironment(dimensionId);

                        wrapper.write(Type.UNSIGNED_BYTE, (short) 0); // todo - do we need to store it from difficulty packet?
                        wrapper.user().get(ChunkLightStorage.class).clear();
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        setDisplayNameJson(true);
        setDisplayNameMetaType(MetaType1_13_2.OptChat);

        regEntType(Entity1_14Types.EntityType.CAT, Entity1_14Types.EntityType.OCELOT).mobName("Cat");
        regEntType(Entity1_14Types.EntityType.TRADER_LLAMA, Entity1_14Types.EntityType.LLAMA).mobName("Trader Llama");
        regEntType(Entity1_14Types.EntityType.FOX, Entity1_14Types.EntityType.WOLF).mobName("Fox");
        regEntType(Entity1_14Types.EntityType.PANDA, Entity1_14Types.EntityType.POLAR_BEAR).mobName("Panda");
        regEntType(Entity1_14Types.EntityType.PILLAGER, Entity1_14Types.EntityType.VILLAGER).mobName("Pillager");
        regEntType(Entity1_14Types.EntityType.WANDERING_TRADER, Entity1_14Types.EntityType.VILLAGER).mobName("Wandering Trader");
        regEntType(Entity1_14Types.EntityType.RAVAGER, Entity1_14Types.EntityType.COW).mobName("Ravager");

        registerMetaHandler().handle(e -> {
            Metadata meta = e.getData();
            int typeId = meta.getMetaType().getTypeID();
            if (typeId <= 15) {
                meta.setMetaType(MetaType1_13_2.byId(typeId));
            }

            MetaType type = meta.getMetaType();

            if (type == MetaType1_13_2.Slot) {
                Item item = (Item) meta.getValue();
                meta.setValue(getProtocol().getBlockItemPackets().handleItemToClient(item));
            } else if (type == MetaType1_13_2.BlockID) {
                int blockstate = (Integer) meta.getValue();
                meta.setValue(Protocol1_13_2To1_14.getNewBlockStateId(blockstate));
            }

            return meta;
        });

        registerMetaHandler().filter(Entity1_14Types.EntityType.PILLAGER, 15).removed();

        registerMetaHandler().filter(Entity1_14Types.EntityType.FOX, 15).removed();
        registerMetaHandler().filter(Entity1_14Types.EntityType.FOX, 16).removed();
        registerMetaHandler().filter(Entity1_14Types.EntityType.FOX, 17).removed();
        registerMetaHandler().filter(Entity1_14Types.EntityType.FOX, 18).removed();

        registerMetaHandler().filter(Entity1_14Types.EntityType.PANDA, 15).removed();
        registerMetaHandler().filter(Entity1_14Types.EntityType.PANDA, 16).removed();
        registerMetaHandler().filter(Entity1_14Types.EntityType.PANDA, 17).removed();
        registerMetaHandler().filter(Entity1_14Types.EntityType.PANDA, 18).removed();
        registerMetaHandler().filter(Entity1_14Types.EntityType.PANDA, 19).removed();
        registerMetaHandler().filter(Entity1_14Types.EntityType.PANDA, 20).removed();

        registerMetaHandler().filter(Entity1_14Types.EntityType.CAT, 18).removed();
        registerMetaHandler().filter(Entity1_14Types.EntityType.CAT, 19).removed();
        registerMetaHandler().filter(Entity1_14Types.EntityType.CAT, 20).removed();

        registerMetaHandler().handle(e -> {
            EntityType type = e.getEntity().getType();
            Metadata meta = e.getData();
            if (type.isOrHasParent(Entity1_14Types.EntityType.ABSTRACT_ILLAGER_BASE) || type == Entity1_14Types.EntityType.RAVAGER || type == Entity1_14Types.EntityType.WITCH) {
                int index = e.getIndex();
                if (index == 14) {
                    //TODO handle
                    throw RemovedValueException.EX;
                } else if (index > 14) {
                    meta.setId(index - 1);
                }
            }
            return meta;
        });

        registerMetaHandler().filter(Entity1_14Types.EntityType.AREA_EFFECT_CLOUD, 10).handle(e -> {
            Metadata meta = e.getData();
            Particle particle = (Particle) meta.getValue();
            particle.setId(getOldParticleId(particle.getId()));
            return meta;
        });

        registerMetaHandler().filter(Entity1_14Types.EntityType.FIREWORKS_ROCKET, 8).handle(e -> {
            Metadata meta = e.getData();
            meta.setMetaType(MetaType1_13_2.VarInt);
            Integer value = (Integer) meta.getValue();
            if (value == null) meta.setValue(0);
            return meta;
        });

        registerMetaHandler().filter(Entity1_14Types.EntityType.ABSTRACT_ARROW, true).handle(e -> {
            Metadata meta = e.getData();
            int index = e.getIndex();
            if (index == 9) {
                throw RemovedValueException.EX;
            } else if (index > 9) {
                meta.setId(index - 1);
            }
            return meta;
        });

        registerMetaHandler().filter(Entity1_14Types.EntityType.VILLAGER, 15).removed(); // Head shake timer

        MetaHandler villagerDataHandler = e -> {
            Metadata meta = e.getData();
            VillagerData villagerData = (VillagerData) meta.getValue();
            meta.setValue(villagerDataToProfession(villagerData));
            meta.setMetaType(MetaType1_13_2.VarInt);
            if (meta.getId() == 16) {
                meta.setId(15); // decreased by 2 again in one of the following handlers
            }
            return meta;
        };

        registerMetaHandler().filter(Entity1_14Types.EntityType.ZOMBIE_VILLAGER, 18).handle(villagerDataHandler);
        registerMetaHandler().filter(Entity1_14Types.EntityType.VILLAGER, 16).handle(villagerDataHandler);

        registerMetaHandler().filter(Entity1_14Types.EntityType.ZOMBIE, true).handle(e -> {
            Metadata meta = e.getData();
            int index = e.getIndex();
            if (index >= 16) {
                meta.setId(index + 1);
            }
            return meta;
        });

        // Remove bed location
        registerMetaHandler().filter(Entity1_14Types.EntityType.LIVINGENTITY, true).handle(e -> {
            Metadata meta = e.getData();
            int index = e.getIndex();
            if (index == 12) {
                Position position = (Position) meta.getValue();
                if (position != null) {
                    // Use bed
                    PacketWrapper wrapper = new PacketWrapper(0x33, null, e.getUser());
                    wrapper.write(Type.VAR_INT, e.getEntity().getEntityId());
                    wrapper.write(Type.POSITION, position);

                    try {
                        wrapper.send(Protocol1_13_2To1_14.class);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                throw RemovedValueException.EX;
            } else if (index > 12) {
                meta.setId(index - 1);
            }
            return meta;
        });

        registerMetaHandler().handle(e -> {
            Metadata meta = e.getData();
            int index = e.getIndex();
            if (index == 6) {
                throw RemovedValueException.EX;
            } else if (index > 6) {
                meta.setId(index - 1);
            }
            return meta;
        });

        registerMetaHandler().handle(e -> {
            Metadata meta = e.getData();
            int typeId = meta.getMetaType().getTypeID();
            if (typeId > 15) {
                ViaBackwards.getPlatform().getLogger().warning("New 1.14 metadata was not handled: " + meta + " entity: " + e.getEntity().getType());
                return null;
            }
            return meta;
        });

        registerMetaHandler().filter(Entity1_14Types.EntityType.OCELOT, 13).handle(e -> {
            Metadata meta = e.getData();
            meta.setId(15);
            meta.setMetaType(MetaType1_13_2.VarInt);
            meta.setValue(0);
            return meta;
        });

        registerMetaHandler().filter(Entity1_14Types.EntityType.CAT).handle(e -> {
            Metadata meta = e.getData();
            if (meta.getId() == 15) {
                meta.setValue(1);
            } else if (meta.getId() == 13) {
                meta.setValue((byte) ((byte) meta.getValue() & 0x4));
            }
            return meta;
        });
    }

    private void cacheEntityPosition(PacketWrapper wrapper, boolean create, boolean relative) throws Exception {
        cacheEntityPosition(wrapper,
                wrapper.get(Type.DOUBLE, 0), wrapper.get(Type.DOUBLE, 1), wrapper.get(Type.DOUBLE, 2), create, relative);
    }

    private void cacheEntityPosition(PacketWrapper wrapper, double x, double y, double z, boolean create, boolean relative) throws Exception {
        int entityId = wrapper.get(Type.VAR_INT, 0);
        Optional<EntityTracker.StoredEntity> optStoredEntity = getEntityTracker(wrapper.user()).getEntity(entityId);
        if (!optStoredEntity.isPresent()) {
            if (!Via.getConfig().isSuppressMetadataErrors()) {
                ViaBackwards.getPlatform().getLogger().warning("Stored entity with id " + entityId + " missing at position: " + x + " - " + y + " - " + z);
            }
            return;
        }

        EntityTracker.StoredEntity storedEntity = optStoredEntity.get();
        EntityPositionStorage positionStorage = create ? new EntityPositionStorage() : storedEntity.get(EntityPositionStorage.class);
        if (positionStorage == null) {
            ViaBackwards.getPlatform().getLogger().warning("Stored entity with id " + entityId + " missing entitypositionstorage!");
            return;
        }

        positionStorage.setCoordinates(x, y, z, relative);
        storedEntity.put(positionStorage);
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
        if (id >= 45) {
            id -= 1; // new 39 -> 44
        }
        if (id >= 30) {
            id -= 1; // skip new short happy villager
        }
        if (id >= 28) {
            id -= 1; // new 24 -> 27
        }
        if (id >= 14) {
            id -= 1; // new water drip 11 -> 13
        }
        if (id >= 12) {
            id -= 2; // new lava drips 10, 11
        }
        return id;
    }

    @Override
    protected EntityType getTypeFromId(int typeId) {
        return Entity1_14Types.getTypeFromId(typeId);
    }

    @Override
    protected int getOldEntityId(final int newId) {
        return EntityTypeMapping.getOldId(newId).orElse(newId);
    }
}
