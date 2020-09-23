package nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.entities.meta.MetaHandler;
import nl.matsv.viabackwards.api.entities.storage.EntityData;
import nl.matsv.viabackwards.api.entities.storage.EntityPositionHandler;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.api.exceptions.RemovedValueException;
import nl.matsv.viabackwards.api.rewriters.LegacyEntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.Protocol1_13_2To1_14;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.storage.ChunkLightStorage;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.storage.EntityPositionStorage1_14;
import us.myles.ViaVersion.api.PacketWrapper;
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
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.ClientboundPackets1_14;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

public class EntityPackets1_14 extends LegacyEntityRewriter<Protocol1_13_2To1_14> {

    private EntityPositionHandler positionHandler;

    public EntityPackets1_14(Protocol1_13_2To1_14 protocol) {
        super(protocol, MetaType1_13_2.OptChat, MetaType1_13_2.Boolean);
    }

    @Override
    protected void addTrackedEntity(PacketWrapper wrapper, int entityId, EntityType type) throws Exception {
        super.addTrackedEntity(wrapper, entityId, type);

        // Cache the position for every newly tracked entity
        if (type == Entity1_14Types.EntityType.PAINTING) {
            final Position position = wrapper.get(Type.POSITION, 0);
            positionHandler.cacheEntityPosition(wrapper, position.getX(), position.getY(), position.getZ(), true, false);
        } else if (wrapper.getId() != 0x25) { // ignore join game
            positionHandler.cacheEntityPosition(wrapper, true, false);
        }
    }

    @Override
    protected void registerPackets() {
        positionHandler = new EntityPositionHandler(this, EntityPositionStorage1_14.class, EntityPositionStorage1_14::new);

        protocol.registerOutgoing(ClientboundPackets1_14.ENTITY_STATUS, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    int entityId = wrapper.passthrough(Type.INT);
                    byte status = wrapper.passthrough(Type.BYTE);
                    // Check for death status
                    if (status != 3) return;

                    EntityTracker.ProtocolEntityTracker tracker = getEntityTracker(wrapper.user());
                    EntityType entityType = tracker.getEntityType(entityId);
                    if (entityType != Entity1_14Types.EntityType.PLAYER) return;

                    // Remove equipment, else the client will see ghost items
                    for (int i = 0; i <= 5; i++) {
                        PacketWrapper equipmentPacket = wrapper.create(0x42);
                        equipmentPacket.write(Type.VAR_INT, entityId);
                        equipmentPacket.write(Type.VAR_INT, i);
                        equipmentPacket.write(Type.FLAT_VAR_INT_ITEM, null);
                        equipmentPacket.send(Protocol1_13_2To1_14.class, true, true);
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_14.ENTITY_TELEPORT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                handler(wrapper -> positionHandler.cacheEntityPosition(wrapper, false, false));
            }
        });

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
                        double x = wrapper.get(Type.SHORT, 0) / EntityPositionHandler.RELATIVE_MOVE_FACTOR;
                        double y = wrapper.get(Type.SHORT, 1) / EntityPositionHandler.RELATIVE_MOVE_FACTOR;
                        double z = wrapper.get(Type.SHORT, 2) / EntityPositionHandler.RELATIVE_MOVE_FACTOR;
                        positionHandler.cacheEntityPosition(wrapper, x, y, z, false, true);
                    }
                });
            }
        };
        protocol.registerOutgoing(ClientboundPackets1_14.ENTITY_POSITION, relativeMoveHandler);
        protocol.registerOutgoing(ClientboundPackets1_14.ENTITY_POSITION_AND_ROTATION, relativeMoveHandler);

        protocol.registerOutgoing(ClientboundPackets1_14.SPAWN_ENTITY, new PacketRemapper() {
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
                        int mappedId = getOldEntityId(id);
                        Entity1_13Types.EntityType entityType = Entity1_13Types.getTypeFromId(mappedId, false);
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
                                case COMMAND_BLOCK_MINECART:
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
                            int combined = protocol.getMappingData().getNewBlockStateId(blockState);
                            wrapper.set(Type.INT, 0, combined);
                        } else if (entityType.isOrHasParent(Entity1_13Types.EntityType.ABSTRACT_ARROW)) {
                            wrapper.set(Type.INT, 0, data + 1);
                        }
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_14.SPAWN_MOB, new PacketRemapper() {
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

                        int oldId = typeMapping.get(type);
                        if (oldId == -1) {
                            EntityData entityData = getEntityData(entityType);
                            if (entityData == null) {
                                ViaBackwards.getPlatform().getLogger().warning("Could not find 1.13.2 entity type for 1.14 entity type " + type + "/" + entityType);
                                wrapper.cancel();
                            } else {
                                wrapper.set(Type.VAR_INT, 1, entityData.getReplacementId());
                            }
                        } else {
                            wrapper.set(Type.VAR_INT, 1, oldId);
                        }
                    }
                });

                // Handle entity type & metadata
                handler(getMobSpawnRewriter(Types1_13_2.METADATA_LIST));
            }
        });

        getProtocol().registerOutgoing(ClientboundPackets1_14.SPAWN_EXPERIENCE_ORB, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.DOUBLE); // Needs to be mapped for the position cache
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                handler(wrapper -> addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), Entity1_14Types.EntityType.EXPERIENCE_ORB));
            }
        });

        getProtocol().registerOutgoing(ClientboundPackets1_14.SPAWN_GLOBAL_ENTITY, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.BYTE);
                map(Type.DOUBLE); // Needs to be mapped for the position cache
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                handler(wrapper -> addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), Entity1_14Types.EntityType.LIGHTNING_BOLT));
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_14.SPAWN_PAINTING, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.UUID);
                map(Type.VAR_INT);
                map(Type.POSITION1_14, Type.POSITION);
                map(Type.BYTE);

                // Track entity
                handler(wrapper -> addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), Entity1_14Types.EntityType.PAINTING));
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_14.SPAWN_PLAYER, new PacketRemapper() {
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
                handler(wrapper -> positionHandler.cacheEntityPosition(wrapper, true, false));
            }
        });

        registerEntityDestroy(ClientboundPackets1_14.DESTROY_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_14.ENTITY_METADATA, Types1_14.METADATA_LIST, Types1_13_2.METADATA_LIST);

        protocol.registerOutgoing(ClientboundPackets1_14.JOIN_GAME, new PacketRemapper() {
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

        protocol.registerOutgoing(ClientboundPackets1_14.RESPAWN, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Dimension ID

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                        int dimensionId = wrapper.get(Type.INT, 0);
                        clientWorld.setEnvironment(dimensionId);

                        wrapper.write(Type.UNSIGNED_BYTE, (short) 0); // Difficulty
                        wrapper.user().get(ChunkLightStorage.class).clear();
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        mapTypes(Entity1_14Types.EntityType.values(), Entity1_13Types.EntityType.class);

        mapEntity(Entity1_14Types.EntityType.CAT, Entity1_14Types.EntityType.OCELOT).jsonName("Cat");
        mapEntity(Entity1_14Types.EntityType.TRADER_LLAMA, Entity1_14Types.EntityType.LLAMA).jsonName("Trader Llama");
        mapEntity(Entity1_14Types.EntityType.FOX, Entity1_14Types.EntityType.WOLF).jsonName("Fox");
        mapEntity(Entity1_14Types.EntityType.PANDA, Entity1_14Types.EntityType.POLAR_BEAR).jsonName("Panda");
        mapEntity(Entity1_14Types.EntityType.PILLAGER, Entity1_14Types.EntityType.VILLAGER).jsonName("Pillager");
        mapEntity(Entity1_14Types.EntityType.WANDERING_TRADER, Entity1_14Types.EntityType.VILLAGER).jsonName("Wandering Trader");
        mapEntity(Entity1_14Types.EntityType.RAVAGER, Entity1_14Types.EntityType.COW).jsonName("Ravager");

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
                meta.setValue(protocol.getMappingData().getNewBlockStateId(blockstate));
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
                    throw RemovedValueException.EX;
                } else if (index > 14) {
                    meta.setId(index - 1);
                }
            }
            return meta;
        });

        registerMetaHandler().filter(Entity1_14Types.EntityType.AREA_EFFECT_CLOUD, 10).handle(e -> {
            Metadata meta = e.getData();
            rewriteParticle((Particle) meta.getValue());
            return meta;
        });

        registerMetaHandler().filter(Entity1_14Types.EntityType.FIREWORK_ROCKET, 8).handle(e -> {
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

        // Holding arms up - from bitfield into own boolean
        registerMetaHandler().filter(Entity1_14Types.EntityType.ABSTRACT_SKELETON, true, 13).handle(e -> {
            byte value = (byte) e.getData().getValue();
            if ((value & 4) != 0) {
                e.createMeta(new Metadata(14, MetaType1_13_2.Boolean, true));
            }
            return e.getData();
        });
        registerMetaHandler().filter(Entity1_14Types.EntityType.ZOMBIE, true, 13).handle(e -> {
            byte value = (byte) e.getData().getValue();
            if ((value & 4) != 0) {
                e.createMeta(new Metadata(16, MetaType1_13_2.Boolean, true));
            }
            return e.getData();
        });

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

    @Override
    protected EntityType getTypeFromId(int typeId) {
        return Entity1_14Types.getTypeFromId(typeId);
    }
}
