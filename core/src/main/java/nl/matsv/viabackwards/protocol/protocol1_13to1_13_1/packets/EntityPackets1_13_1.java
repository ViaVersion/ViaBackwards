package nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.entities.storage.MetaStorage;
import nl.matsv.viabackwards.api.entities.types.EntityType1_12;
import nl.matsv.viabackwards.api.entities.types.EntityType1_13;
import nl.matsv.viabackwards.api.entities.types.EntityType1_13.EntityType;
import nl.matsv.viabackwards.api.rewriters.EntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.Protocol1_13To1_13_1;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_13;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.version.Types1_13;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

public class EntityPackets1_13_1 extends EntityRewriter<Protocol1_13To1_13_1> {

    @Override
    protected void registerPackets(Protocol1_13To1_13_1 protocol) {

        // Spawn Object
        protocol.out(State.PLAY, 0x00, 0x00, new PacketRemapper() {
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
                        EntityType entType = EntityType1_13.getTypeFromId(type, true);
                        if (entType == null) {
                            ViaBackwards.getPlatform().getLogger().warning("Could not find 1.13 entity type " + type);
                            return;
                        }

                        // Rewrite falling block
                        if (entType.is(EntityType.FALLING_BLOCK)) {
                            int data = wrapper.get(Type.INT, 0);
                            wrapper.set(Type.INT, 0, Protocol1_13To1_13_1.getNewBlockStateId(data));
                        }

                        // Track Entity
                        addTrackedEntity(
                                wrapper.user(),
                                entityId,
                                entType
                        );
                    }
                });
            }
        });

        // Spawn Experience Orb
        protocol.out(State.PLAY, 0x01, 0x01, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                EntityType.XP_ORB
                        );
                    }
                });
            }
        });

        // Spawn Global Entity
        protocol.out(State.PLAY, 0x02, 0x02, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.BYTE);

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                EntityType.LIGHTNING_BOLT
                        );
                    }
                });
            }
        });

        // Spawn Mob
        protocol.out(State.PLAY, 0x3, 0x3, new PacketRemapper() {
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
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int type = wrapper.get(Type.VAR_INT, 1);

                        EntityType entityType = EntityType1_13.getTypeFromId(type, false);
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                entityType
                        );
                    }
                });

                // Rewrite Metadata
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        MetaStorage storage = new MetaStorage(wrapper.get(Types1_13.METADATA_LIST, 0));
                        handleMeta(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                storage
                        );

                        // Don't handle new ids / base meta since it's not used for this version

                        // Rewrite Metadata
                        wrapper.set(
                                Types1_13.METADATA_LIST,
                                0,
                                storage.getMetaDataList()
                        );
                    }
                });
            }
        });

        // Spawn player packet
        protocol.out(State.PLAY, 0x05, 0x05, new PacketRemapper() {
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
                                Types1_13.METADATA_LIST,
                                0,
                                handleMeta(
                                        wrapper.user(),
                                        wrapper.get(Type.VAR_INT, 0),
                                        new MetaStorage(wrapper.get(Types1_13.METADATA_LIST, 0))
                                ).getMetaDataList()
                        );
                    }
                });
            }
        });

        //Spawn Painting
        protocol.out(State.PLAY, 0x04, 0x04, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.UUID);
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

        // Join Game
        protocol.registerOutgoing(State.PLAY, 0x25, 0x25, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Entity ID
                map(Type.UNSIGNED_BYTE); // 1 - Gamemode
                map(Type.INT); // 2 - Dimension

                // Track Entity
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.INT, 0),
                                EntityType1_12.EntityType.PLAYER
                        );
                    }
                });

                // Save dimension
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ClientWorld clientChunks = wrapper.user().get(ClientWorld.class);
                        int dimensionId = wrapper.get(Type.INT, 1);
                        clientChunks.setEnvironment(dimensionId);
                    }
                });
            }
        });

        // Respawn
        protocol.out(State.PLAY, 0x38, 0x38, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Dimension ID\

                // Save dimension
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

        // Destroy entities
        protocol.out(State.PLAY, 0x35, 0x35, new PacketRemapper() {
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
        protocol.out(State.PLAY, 0x3F, 0x3F, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Types1_13.METADATA_LIST); // 1 - Metadata list

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.set(
                                Types1_13.METADATA_LIST,
                                0,
                                handleMeta(
                                        wrapper.user(),
                                        wrapper.get(Type.VAR_INT, 0),
                                        new MetaStorage(wrapper.get(Types1_13.METADATA_LIST, 0))
                                ).getMetaDataList()
                        );
                    }
                });
            }
        });
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
                meta.setValue(Protocol1_13To1_13_1.getNewBlockStateId(data));
            }

            return meta;
        });

        // Remove shooter UUID
        registerMetaHandler().
                filter(EntityType.ABSTRACT_ARROW, true, 7)
                .removed();

        // Move colors to old position
        registerMetaHandler().filter(EntityType.SPECTRAL_ARROW, 8)
                .handleIndexChange(7);

        // Move loyalty level to old position
        registerMetaHandler().filter(EntityType.TRIDENT, 8)
                .handleIndexChange(7);

        // Rewrite Minecart blocks
        registerMetaHandler()
                .filter(EntityType.MINECART_ABSTRACT, true, 9)
                .handle(e -> {
                    Metadata meta = e.getData();

                    int data = (int) meta.getValue();
                    meta.setValue(Protocol1_13To1_13_1.getNewBlockStateId(data));

                    return meta;
                });
    }
}
