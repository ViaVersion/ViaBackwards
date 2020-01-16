package nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.entities.storage.MetaStorage;
import nl.matsv.viabackwards.api.rewriters.EntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.Protocol1_13To1_13_1;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.entities.Entity1_13Types;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_13;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.version.Types1_13;
import us.myles.ViaVersion.packets.State;

public class EntityPackets1_13_1 extends EntityRewriter<Protocol1_13To1_13_1> {

    public EntityPackets1_13_1(Protocol1_13To1_13_1 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {

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
                        Entity1_13Types.EntityType entType = Entity1_13Types.getTypeFromId(type, true);
                        if (entType == null) {
                            ViaBackwards.getPlatform().getLogger().warning("Could not find 1.13 entity type " + type);
                            return;
                        }

                        // Rewrite falling block
                        if (entType.is(Entity1_13Types.EntityType.FALLING_BLOCK)) {
                            int data = wrapper.get(Type.INT, 0);
                            wrapper.set(Type.INT, 0, Protocol1_13To1_13_1.getNewBlockStateId(data));
                        }

                        // Track Entity
                        addTrackedEntity(wrapper, entityId, entType);
                    }
                });
            }
        });

        // Spawn Experience Orb
        registerExtraTracker(0x01, Entity1_13Types.EntityType.XP_ORB);

        // Spawn Global Entity
        registerExtraTracker(0x02, Entity1_13Types.EntityType.LIGHTNING_BOLT);

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
                handler(getTrackerHandler());

                // Rewrite Metadata
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        MetaStorage storage = new MetaStorage(wrapper.get(Types1_13.METADATA_LIST, 0));
                        handleMeta(wrapper.user(), wrapper.get(Type.VAR_INT, 0), storage);

                        // Don't handle new ids / base meta since it's not used for this version

                        // Rewrite Metadata
                        wrapper.set(Types1_13.METADATA_LIST, 0, storage.getMetaDataList());
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

                handler(getTrackerAndMetaHandler(Types1_13.METADATA_LIST, Entity1_13Types.EntityType.PLAYER));
            }
        });

        // Spawn Painting
        registerExtraTracker(0x04, Entity1_13Types.EntityType.PAINTING);

        // Join Game
        protocol.registerOutgoing(State.PLAY, 0x25, 0x25, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Entity ID
                map(Type.UNSIGNED_BYTE); // 1 - Gamemode
                map(Type.INT); // 2 - Dimension

                handler(getTrackerHandler(Entity1_13Types.EntityType.PLAYER, Type.INT));
                handler(getDimensionHandler(1));
            }
        });

        // Respawn
        protocol.out(State.PLAY, 0x38, 0x38, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Dimension ID\

                // Save dimension
                handler(getDimensionHandler(0));
            }
        });

        // Destroy entities
        registerEntityDestroy(0x35);

        // Entity Metadata packet
        registerMetadataRewriter(0x3F, 0x3F, Types1_13.METADATA_LIST);
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
                filter(Entity1_13Types.EntityType.ABSTRACT_ARROW, true, 7)
                .removed();

        // Move colors to old position
        registerMetaHandler().filter(Entity1_13Types.EntityType.SPECTRAL_ARROW, 8)
                .handleIndexChange(7);

        // Move loyalty level to old position
        registerMetaHandler().filter(Entity1_13Types.EntityType.TRIDENT, 8)
                .handleIndexChange(7);

        // Rewrite Minecart blocks
        registerMetaHandler()
                .filter(Entity1_13Types.EntityType.MINECART_ABSTRACT, true, 9)
                .handle(e -> {
                    Metadata meta = e.getData();

                    int data = (int) meta.getValue();
                    meta.setValue(Protocol1_13To1_13_1.getNewBlockStateId(data));

                    return meta;
                });
    }

    @Override
    protected EntityType getTypeFromId(int typeId) {
        return Entity1_13Types.getTypeFromId(typeId, false);
    }

    @Override
    protected EntityType getObjectTypeFromId(final int typeId) {
        return Entity1_13Types.getTypeFromId(typeId, true);
    }
}
