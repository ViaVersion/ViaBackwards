package nl.matsv.viabackwards.protocol.protocol1_14to1_14_1.packets;

import nl.matsv.viabackwards.api.entities.storage.MetaStorage;
import nl.matsv.viabackwards.api.rewriters.EntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_14to1_14_1.Protocol1_14To1_14_1;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.entities.Entity1_14Types;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.version.Types1_14;
import us.myles.ViaVersion.packets.State;

public class EntityPackets1_14_1 extends EntityRewriter<Protocol1_14To1_14_1> {

    public EntityPackets1_14_1(Protocol1_14To1_14_1 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerExtraTracker(0x01, Entity1_14Types.EntityType.XP_ORB);
        registerExtraTracker(0x02, Entity1_14Types.EntityType.LIGHTNING_BOLT);
        registerExtraTracker(0x04, Entity1_14Types.EntityType.PAINTING);
        registerExtraTracker(0x05, Entity1_14Types.EntityType.PLAYER);
        registerExtraTracker(0x25, Entity1_14Types.EntityType.PLAYER, Type.INT); // Join game
        registerEntityDestroy(0x37);

        // Spawn Object
        protocol.registerOutgoing(State.PLAY, 0x00, 0x00, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.UUID); // 1 - UUID
                map(Type.VAR_INT); // 2 - Type

                handler(getTrackerHandler());
            }
        });

        // Spawn Mob
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
                map(Types1_14.METADATA_LIST); // 12 - Metadata

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int entityId = wrapper.get(Type.VAR_INT, 0);
                        int type = wrapper.get(Type.VAR_INT, 1);

                        // Register Type ID
                        addTrackedEntity(wrapper, entityId, Entity1_14Types.getTypeFromId(type));

                        MetaStorage storage = new MetaStorage(wrapper.get(Types1_14.METADATA_LIST, 0));
                        handleMeta(wrapper.user(), entityId, storage);
                    }
                });
            }
        });

        // Entity Metadata
        registerMetadataRewriter(0x43, 0x43, Types1_14.METADATA_LIST);
    }

    @Override
    protected void registerRewrites() {
        registerMetaHandler().filter(Entity1_14Types.EntityType.VILLAGER, 15).removed();
        registerMetaHandler().filter(Entity1_14Types.EntityType.VILLAGER, 16).handleIndexChange(15);
        registerMetaHandler().filter(Entity1_14Types.EntityType.WANDERING_TRADER, 15).removed();
    }

    @Override
    protected EntityType getTypeFromId(int typeId) {
        return Entity1_14Types.getTypeFromId(typeId);
    }
}
