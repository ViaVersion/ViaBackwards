package nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.packets;

import nl.matsv.viabackwards.api.entities.storage.MetaStorage;
import nl.matsv.viabackwards.api.entities.types.EntityType1_13;
import nl.matsv.viabackwards.api.entities.types.EntityType1_14;
import nl.matsv.viabackwards.api.rewriters.EntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.Protocol1_13_2To1_14;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.data.EntityTypeMapping;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.VillagerData;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_13_2;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.version.Types1_13_2;
import us.myles.ViaVersion.api.type.types.version.Types1_14;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

public class EntityPackets1_14 extends EntityRewriter<Protocol1_13_2To1_14> {
    @Override
    protected void registerPackets(Protocol1_13_2To1_14 protocol) {
// Spawn entity
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

                // Track Entity
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int entityId = wrapper.get(Type.VAR_INT, 0);
                        int typeId = wrapper.get(Type.VAR_INT, 1);

                        EntityType1_14.EntityType type1_14 = EntityType1_14.getTypeFromId(typeId);
                        typeId = EntityTypeMapping.getOldId(type1_14.getId()).orElse(type1_14.getId());
                        EntityType1_13.EntityType type1_13 = EntityType1_13.getTypeFromId(typeId, true);
                        typeId = EntityTypeMapping.getObjectId(type1_14.getId()).orElse(type1_14.getId());

                        if (type1_13 != null) {
                            if (type1_13.is(EntityType1_13.EntityType.FALLING_BLOCK)) {
                                int data = wrapper.get(Type.INT, 0);
                                wrapper.set(Type.INT, 0, Protocol1_13_2To1_14.blockItem.handleBlockID(data));
                            }
                        }

                        wrapper.set(Type.VAR_INT, 1, typeId);
                        // Register Type ID
                        getEntityTracker(wrapper.user()).trackEntityType(entityId, type1_13);
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
                        int entityId = wrapper.get(Type.VAR_INT, 0);
                        int type = wrapper.get(Type.VAR_INT, 1);

                        type = EntityTypeMapping.getOldId(type).orElse(type);

                        EntityType1_13.EntityType entType = EntityType1_13.getTypeFromId(type, false);

                        wrapper.set(Type.VAR_INT, 1, type);

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

                        EntityType1_13.EntityType entType = EntityType1_13.EntityType.PLAYER;
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


        // Metadata packet
        protocol.registerOutgoing(State.PLAY, 0x3F, 0x3F, new PacketRemapper() {
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
                        addTrackedEntity(wrapper.user(), entityId, EntityType1_13.EntityType.PLAYER);
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        regEntType(EntityType1_14.EntityType.CAT, EntityType1_14.EntityType.OCELOT).mobName("Cat").spawnMetadata(e -> {
            e.add(new Metadata(13, MetaType1_13_2.Byte, (byte) 0x4)); // Tamed cat
        });
        regEntType(EntityType1_14.EntityType.OCELOT, EntityType1_14.EntityType.OCELOT).mobName("Ocelot");
        regEntType(EntityType1_14.EntityType.TRADER_LLAMA, EntityType1_14.EntityType.LLAMA).mobName("Trader Llama");

        registerMetaHandler().handle(e -> {
            if (e.getData().getMetaType().getTypeID() == 6) { // Slot
                Protocol1_13_2To1_14.blockItem.handleItemToClient((Item) e.getData().getValue());
            }
            return e.getData();
        });
        // Remove entity pose
        registerMetaHandler().filter(EntityType1_14.EntityType.ENTITY, 6).removed();
        registerMetaHandler().filter(EntityType1_14.EntityType.ENTITY).handle(e -> {
            if (e.getIndex() > 6) e.getData().setId(e.getIndex() - 1);
            return e.getData();
        });
        registerMetaHandler().filter(EntityType1_14.EntityType.CAT, 13).removed();
        registerMetaHandler().filter(EntityType1_14.EntityType.CAT, 14).removed();
        registerMetaHandler().filter(EntityType1_14.EntityType.CAT, 15).removed();
        // Villager data -> var int
        registerMetaHandler().handle(e -> {
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
}
