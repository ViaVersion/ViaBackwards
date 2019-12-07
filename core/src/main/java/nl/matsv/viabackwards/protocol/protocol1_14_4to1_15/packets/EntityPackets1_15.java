package nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.packets;

import nl.matsv.viabackwards.api.exceptions.RemovedValueException;
import nl.matsv.viabackwards.api.rewriters.EntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.Protocol1_14_4To1_15;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data.EntityTypeMapping;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.entities.Entity1_14Types;
import us.myles.ViaVersion.api.entities.Entity1_15Types;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.MetaType;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_14;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.version.Types1_14;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

public class EntityPackets1_15 extends EntityRewriter<Protocol1_14_4To1_15> {

    @Override
    protected void registerPackets(Protocol1_14_4To1_15 protocol) {
        // Spawn Object
        protocol.registerOutgoing(State.PLAY, 0x00, 0x00, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.UUID); // 1 - UUID
                map(Type.VAR_INT); // 2 - Type
                map(Type.DOUBLE); // 3 - X
                map(Type.DOUBLE); // 4 - Y
                map(Type.DOUBLE); // 5 - Z
                map(Type.BYTE); // 6 - Pitch
                map(Type.BYTE); // 7 - Yaw
                map(Type.INT); // 8 - Data

                handler(getTrackerHandler());
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int typeId = wrapper.get(Type.VAR_INT, 1);
                        Entity1_14Types.EntityType entityType = Entity1_14Types.getTypeFromId(getOldEntityId(typeId));
                        wrapper.set(Type.VAR_INT, 1, entityType.getId());

                        if (entityType == Entity1_14Types.EntityType.FALLING_BLOCK) {
                            int blockState = wrapper.get(Type.INT, 0);
                            int combined = Protocol1_14_4To1_15.getNewBlockStateId(blockState);
                            wrapper.set(Type.INT, 0, combined);
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
                create(wrapper -> wrapper.write(Type.UNSIGNED_BYTE, (short) 0xff)); // Metadata is no longer sent in 1.15, so we have to send an empty one

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int type = wrapper.get(Type.VAR_INT, 1);
                        Entity1_15Types.EntityType entityType = Entity1_15Types.getTypeFromId(type);
                        addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), entityType);
                        wrapper.set(Type.VAR_INT, 1, EntityTypeMapping.getOldEntityId(type));
                    }
                });
            }
        });

        // Respawn
        protocol.registerOutgoing(State.PLAY, 0x3B, 0x3A, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT);
                map(Type.LONG, Type.NOTHING); // Seed
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                        clientWorld.setEnvironment(wrapper.get(Type.INT, 0));
                    }
                });
            }
        });

        // Join Game
        protocol.registerOutgoing(State.PLAY, 0x26, 0x25, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Entity ID
                map(Type.UNSIGNED_BYTE); // 1 - Gamemode
                map(Type.INT); // 2 - Dimension

                map(Type.LONG, Type.NOTHING); // Seed

                map(Type.UNSIGNED_BYTE); // 3 - Max Players
                map(Type.STRING); // 4 - Level Type
                map(Type.VAR_INT); // 5 - View Distance
                map(Type.BOOLEAN); // 6 - Reduce Debug Info

                map(Type.BOOLEAN, Type.NOTHING); // Show death screen

                handler(getTrackerHandler(Entity1_15Types.EntityType.PLAYER, Type.INT));
                handler(getDimensionHandler(1));
            }
        });

        // Edit Book
        protocol.registerIncoming(State.PLAY, 0x0D, 0x0C, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        getProtocol().getBlockItemPackets().handleItemToServer(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM));
                    }
                });
            }
        });

        // Spawn Experience Orb
        registerExtraTracker(0x01, Entity1_15Types.EntityType.XP_ORB);

        // Spawn Global Object
        registerExtraTracker(0x02, Entity1_15Types.EntityType.LIGHTNING_BOLT);

        // Spawn painting
        registerExtraTracker(0x04, Entity1_15Types.EntityType.PAINTING);

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
                create(wrapper -> wrapper.write(Type.UNSIGNED_BYTE, (short) 0xff)); // Metadata is no longer sent in 1.15, so we have to send an empty one

                handler(getTrackerHandler(Entity1_15Types.EntityType.PLAYER, Type.VAR_INT));
            }
        });

        // Destroy entities
        registerEntityDestroy(0x38, 0x37);

        // Entity Metadata packet
        register1_15MetadataRewriter(0x44, 0x43, Types1_14.METADATA_LIST);

        // Attributes (get rid of generic.flyingSpeed for the Bee remap)
        protocol.registerOutgoing(State.PLAY, 0x59, 0x58, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.INT);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int entityId = wrapper.get(Type.VAR_INT, 0);
                        EntityType entityType = getEntityType(wrapper.user(), entityId);
                        if (entityType != Entity1_15Types.EntityType.BEE) return;

                        int size = wrapper.get(Type.INT, 0);
                        for (int i = 0; i < size; i++) {
                            String key = wrapper.read(Type.STRING);
                            if (key.equals("generic.flyingSpeed")) {
                                size--;
                                wrapper.read(Type.DOUBLE);
                                int modSize = wrapper.read(Type.VAR_INT);
                                for (int j = 0; j < modSize; j++) {
                                    wrapper.read(Type.UUID);
                                    wrapper.read(Type.DOUBLE);
                                    wrapper.read(Type.BYTE);
                                }
                            } else {
                                wrapper.write(Type.STRING, key);
                                wrapper.passthrough(Type.DOUBLE);
                                int modSize = wrapper.passthrough(Type.VAR_INT);
                                for (int j = 0; j < modSize; j++) {
                                    wrapper.passthrough(Type.UUID);
                                    wrapper.passthrough(Type.DOUBLE);
                                    wrapper.passthrough(Type.BYTE);
                                }
                            }
                        }
                        wrapper.set(Type.INT, 0, size);
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        setDisplayNameJson(true);
        setDisplayNameMetaType(MetaType1_14.OptChat);

        registerMetaHandler().handle(e -> {
            Metadata meta = e.getData();
            MetaType type = meta.getMetaType();
            if (type == MetaType1_14.Slot) {
                Item item = (Item) meta.getValue();
                meta.setValue(getProtocol().getBlockItemPackets().handleItemToClient(item));
            } else if (type == MetaType1_14.BlockID) {
                int blockstate = (int) meta.getValue();
                meta.setValue(Protocol1_14_4To1_15.getNewBlockStateId(blockstate));
            }
            return meta;
        });

        registerMetaHandler().filter(Entity1_15Types.EntityType.LIVINGENTITY, true).handle(e -> {
            int index = e.getIndex();
            if (index == 12) {
                throw RemovedValueException.EX;
            } else if (index > 12) {
                e.getData().setId(index - 1);
            }
            return e.getData();
        });

        registerMetaHandler().filter(Entity1_15Types.EntityType.BEE, 15).removed();
        registerMetaHandler().filter(Entity1_15Types.EntityType.BEE, 16).removed();

        regEntType(Entity1_15Types.EntityType.BEE, Entity1_15Types.EntityType.PUFFER_FISH).mobName("Bee").spawnMetadata(storage -> {
            storage.add(new Metadata(14, MetaType1_14.Boolean, false));
            storage.add(new Metadata(15, MetaType1_14.VarInt, 2));
        });

        registerMetaHandler().filter(Entity1_15Types.EntityType.ENDERMAN, 16).removed();
        registerMetaHandler().filter(Entity1_15Types.EntityType.TRIDENT, 10).removed();

        registerMetaHandler().filter(Entity1_15Types.EntityType.WOLF).handle(e -> {
            int index = e.getIndex();
            if (index >= 17) {
                e.getData().setId(index + 1); // redundant health removed in 1.15
            }
            return e.getData();
        });
    }

    @Override
    protected EntityType getTypeFromId(int typeId) {
        return Entity1_15Types.getTypeFromId(typeId);
    }

    @Override
    protected int getOldEntityId(final int newId) {
        return EntityTypeMapping.getOldEntityId(newId);
    }
}
