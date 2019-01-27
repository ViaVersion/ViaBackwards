package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.entities.storage.EntityData;
import nl.matsv.viabackwards.api.entities.storage.MetaStorage;
import nl.matsv.viabackwards.api.entities.types.AbstractEntityType;
import nl.matsv.viabackwards.api.entities.types.EntityType1_12;
import nl.matsv.viabackwards.api.entities.types.EntityType1_13;
import nl.matsv.viabackwards.api.entities.types.EntityType1_13.EntityType;
import nl.matsv.viabackwards.api.exceptions.RemovedValueException;
import nl.matsv.viabackwards.api.rewriters.EntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.EntityTypeMapping;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.PaintingMapping;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_12;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.version.Types1_12;
import us.myles.ViaVersion.api.type.types.version.Types1_13;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ChatRewriter;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.Particle;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

import java.util.Optional;

public class EntityPackets1_13 extends EntityRewriter<Protocol1_12_2To1_13> {

    @Override
    protected void registerPackets(Protocol1_12_2To1_13 protocol) {

        //Spawn Object
        protocol.out(State.PLAY, 0x00, 0x00, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.UUID);
                map(Type.BYTE);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.BYTE);
                map(Type.BYTE);
                map(Type.INT);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        byte type = wrapper.get(Type.BYTE, 0);
                        EntityType entityType = EntityType1_13.getTypeFromId(type, true);
                        if (entityType == null) {
                            ViaBackwards.getPlatform().getLogger().warning("Could not find 1.13 entity type " + type);
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
                        Optional<EntityType1_13.ObjectType> type = EntityType1_13.ObjectType.findById(wrapper.get(Type.BYTE, 0));
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

        //Spawn Experience Orb
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

        //Spawn Global Entity
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

        //Spawn Mob
        protocol.out(State.PLAY, 0x03, 0x03, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.UUID);
                map(Type.VAR_INT);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.BYTE);
                map(Type.BYTE);
                map(Type.BYTE);
                map(Type.SHORT);
                map(Type.SHORT);
                map(Type.SHORT);
                map(Types1_13.METADATA_LIST, Types1_12.METADATA_LIST);

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
                        Optional<Integer> oldId = EntityTypeMapping.getOldId(type);
                        if (!oldId.isPresent()) {
                            if (!hasData(entityType))
                                ViaBackwards.getPlatform().getLogger().warning("Could not find 1.12 entity type for 1.13 entity type " + type + "/" + entityType);
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

                        MetaStorage storage = new MetaStorage(wrapper.get(Types1_12.METADATA_LIST, 0));
                        handleMeta(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                storage
                        );

                        Optional<EntityData> optEntDat = getEntityData(type);
                        if (optEntDat.isPresent()) {
                            EntityData data = optEntDat.get();

                            Optional<Integer> replacementId = EntityTypeMapping.getOldId(data.getReplacementId());
                            wrapper.set(Type.VAR_INT, 1, replacementId.orElse(EntityType1_12.EntityType.ZOMBIE.getId()));
                            if (data.hasBaseMeta())
                                data.getDefaultMeta().handle(storage);
                        }

                        // Rewrite Metadata
                        wrapper.set(
                                Types1_12.METADATA_LIST,
                                0,
                                storage.getMetaDataList()
                        );
                    }
                });
            }
        });

        // Spawn Player
        protocol.out(State.PLAY, 0x05, 0x05, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.UUID);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.BYTE);
                map(Type.BYTE);
                map(Types1_13.METADATA_LIST, Types1_12.METADATA_LIST);

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
                                Types1_12.METADATA_LIST,
                                0,
                                handleMeta(
                                        wrapper.user(),
                                        wrapper.get(Type.VAR_INT, 0),
                                        new MetaStorage(wrapper.get(Types1_12.METADATA_LIST, 0))
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
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int motive = wrapper.read(Type.VAR_INT);
                        String title = PaintingMapping.getStringId(motive);
                        wrapper.write(Type.STRING, title);
                    }
                });
            }
        });

        // Join game
        protocol.out(State.PLAY, 0x25, 0x23, new PacketRemapper() {
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


        // Respawn Packet (save dimension id)
        protocol.registerOutgoing(State.PLAY, 0x38, 0x35, new PacketRemapper() {
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

        // Destroy Entities Packet
        protocol.registerOutgoing(State.PLAY, 0x35, 0x32, new PacketRemapper() {
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

        // Entity Metadata packet
        protocol.registerOutgoing(State.PLAY, 0x3F, 0x3C, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Types1_13.METADATA_LIST, Types1_12.METADATA_LIST); // 1 - Metadata list

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.set(
                                Types1_12.METADATA_LIST,
                                0,
                                handleMeta(
                                        wrapper.user(),
                                        wrapper.get(Type.VAR_INT, 0),
                                        new MetaStorage(wrapper.get(Types1_12.METADATA_LIST, 0))
                                ).getMetaDataList()
                        );
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        // Rewrite new Entity 'drowned'
        regEntType(EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER).mobName("Drowned");

        // Fishy
        regEntType(EntityType.COD_MOB, EntityType.SQUID).mobName("Cod");
        regEntType(EntityType.SALMON_MOB, EntityType.SQUID).mobName("Salmon");
        regEntType(EntityType.PUFFER_FISH, EntityType.SQUID).mobName("Puffer Fish");
        regEntType(EntityType.TROPICAL_FISH, EntityType.SQUID).mobName("Tropical Fish");

        // Phantom
        regEntType(EntityType.PHANTOM, EntityType.PARROT).mobName("Phantom").spawnMetadata(storage -> {
            // The phantom is grey/blue so let's do yellow/blue
            storage.add(new Metadata(15, MetaType1_12.VarInt, 3));
        });

        // Dolphin
        regEntType(EntityType.DOLPHIN, EntityType.SQUID).mobName("Dolphin");

        // Turtle
        regEntType(EntityType.TURTLE, EntityType.OCELOT).mobName("Turtle");


        // Rewrite Meta types
        registerMetaHandler().handle(e -> {
            Metadata meta = e.getData();
            int typeId = meta.getMetaType().getTypeID();

            // Rewrite optional chat to chat
            if (typeId == 5) {
                meta.setMetaType(MetaType1_12.String);

                if (meta.getValue() == null) {
                    meta.setValue("");
                }
            }

            // Rewrite items
            else if (typeId == 6) {
                meta.setMetaType(MetaType1_12.Slot);
                Item item = (Item) meta.getValue();
                meta.setValue(getProtocol().getBlockItemPackets().handleItemToClient(item));
            }

            // Discontinue particles
            else if (typeId == 15) {
                meta.setMetaType(MetaType1_12.Discontinued);
            }

            // Rewrite to 1.12 ids
            else if (typeId > 5) {
                meta.setMetaType(MetaType1_12.byId(
                        typeId - 1
                ));
            }

            return meta;
        });

        // Rewrite Custom Name from Chat to String
        registerMetaHandler().filter(EntityType.ENTITY, true, 2).handle(e -> {
            Metadata meta = e.getData();

            meta.setValue(
                    ChatRewriter.jsonTextToLegacy(
                            (String) meta.getValue()
                    )
            );

            return meta;
        });

        // Handle zombie metadata
        registerMetaHandler().filter(EntityType.ZOMBIE, true, 15).removed();
        registerMetaHandler().filter(EntityType.ZOMBIE, true).handle(e -> {
            Metadata meta = e.getData();

            if (meta.getId() > 15) {
                meta.setId(meta.getId() - 1);
            }

            return meta;
        });

        // Handle turtle metadata (Remove them all for now)
        registerMetaHandler().filter(EntityType.TURTLE, 13).removed(); // Home pos
        registerMetaHandler().filter(EntityType.TURTLE, 14).removed(); // Has egg
        registerMetaHandler().filter(EntityType.TURTLE, 15).removed(); // Laying egg
        registerMetaHandler().filter(EntityType.TURTLE, 16).removed(); // Travel pos
        registerMetaHandler().filter(EntityType.TURTLE, 17).removed(); // Going home
        registerMetaHandler().filter(EntityType.TURTLE, 18).removed(); // Traveling

        // Remove additional fish meta
        registerMetaHandler().filter(EntityType.ABSTRACT_FISHES, true, 12).removed();
        registerMetaHandler().filter(EntityType.ABSTRACT_FISHES, true, 13).removed();

        // Remove phantom size
        registerMetaHandler().filter(EntityType.PHANTOM, 12).removed();

        // Remove boat splash timer
        registerMetaHandler().filter(EntityType.BOAT, 12).removed();

        // Remove Trident special loyalty level
        registerMetaHandler().filter(EntityType.TRIDENT, 7).removed();

        // Handle new wolf colors
        registerMetaHandler().filter(EntityType.WOLF, 17).handle(e -> {
            Metadata meta = e.getData();

            meta.setValue(15 - (int) meta.getValue());

            return meta;
        });

        // Rewrite AreaEffectCloud
        registerMetaHandler().filter(EntityType.AREA_EFFECT_CLOUD, 9).handle(e -> {
            Metadata meta = e.getData();
            Particle particle = (Particle) meta.getValue();

            // TODO Rewrite particle ids
            e.getStorage().add(new Metadata(9, MetaType1_12.VarInt, 0));
            e.getStorage().add(new Metadata(10, MetaType1_12.VarInt, 0));
            e.getStorage().add(new Metadata(11, MetaType1_12.VarInt, 0));

            throw new RemovedValueException();
        });

        // TODO REWRITE BLOCKS IN MINECART

    }
}
