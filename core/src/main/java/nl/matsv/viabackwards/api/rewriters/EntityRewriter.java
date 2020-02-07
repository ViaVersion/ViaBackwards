package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.entities.storage.EntityData;
import nl.matsv.viabackwards.api.entities.storage.MetaStorage;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.minecraft.metadata.MetaType;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_14;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

import java.util.List;

public abstract class EntityRewriter<T extends BackwardsProtocol> extends EntityRewriterBase<T> {

    protected EntityRewriter(T protocol) {
        super(protocol, MetaType1_14.OptChat, 2, true);
    }

    protected EntityRewriter(T protocol, MetaType displayType) {
        super(protocol, displayType, 2, true);
    }

    public void registerSpawnTrackerWithData(int oldPacketId, int newPacketId, EntityType fallingBlockType, ItemRewriter itemRewriter) {
        protocol.registerOutgoing(State.PLAY, oldPacketId, newPacketId, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.UUID); // 1 - Entity UUID
                map(Type.VAR_INT); // 2 - Entity Type
                map(Type.DOUBLE); // 3 - X
                map(Type.DOUBLE); // 4 - Y
                map(Type.DOUBLE); // 5 - Z
                map(Type.BYTE); // 6 - Pitch
                map(Type.BYTE); // 7 - Yaw
                map(Type.INT); // 8 - Data
                handler(wrapper -> {
                    int typeId = wrapper.get(Type.VAR_INT, 1);
                    EntityType entityType = getTypeFromId(typeId);
                    addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), entityType);

                    int oldTypeId = getOldEntityId(entityType.getId());
                    if (typeId != oldTypeId) {
                        wrapper.set(Type.VAR_INT, 1, oldTypeId);
                    }

                    if (entityType == fallingBlockType) {
                        int blockState = wrapper.get(Type.INT, 0);
                        wrapper.set(Type.INT, 0, itemRewriter.toClientRewriter.rewrite(blockState));
                    }
                });
            }
        });
    }

    public void registerSpawnTracker(int oldPacketId, int newPacketId) {
        protocol.registerOutgoing(State.PLAY, oldPacketId, newPacketId, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.UUID); // 1 - Entity UUID
                map(Type.VAR_INT); // 2 - Entity Type
                handler(wrapper -> {
                    int typeId = wrapper.get(Type.VAR_INT, 1);
                    EntityType entityType = getTypeFromId(typeId);
                    addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), entityType);

                    int oldTypeId = getOldEntityId(entityType.getId());
                    if (typeId != oldTypeId) {
                        wrapper.set(Type.VAR_INT, 1, oldTypeId);
                    }
                });
            }
        });
    }

    /**
     * Helper method to handle a metadata list packet and its full initial meta rewrite.
     */
    protected void registerMetadataRewriter(int oldPacketId, int newPacketId, Type<List<Metadata>> oldMetaType, Type<List<Metadata>> newMetaType) {
        getProtocol().registerOutgoing(State.PLAY, oldPacketId, newPacketId, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                if (oldMetaType != null) {
                    map(oldMetaType, newMetaType);
                } else {
                    map(newMetaType);
                }
                handler(wrapper -> {
                    int entityId = wrapper.get(Type.VAR_INT, 0);
                    EntityType type = getEntityType(wrapper.user(), entityId);

                    MetaStorage storage = new MetaStorage(wrapper.get(newMetaType, 0));
                    handleMeta(wrapper.user(), entityId, storage);

                    EntityData entityData = getEntityData(type);
                    if (entityData != null) {
                        if (entityData.hasBaseMeta()) {
                            entityData.getDefaultMeta().handle(storage);
                        }
                    }

                    wrapper.set(newMetaType, 0, storage.getMetaDataList());
                });
            }
        });
    }

    protected void registerMetadataRewriter(int oldPacketId, int newPacketId, Type<List<Metadata>> metaType) {
        registerMetadataRewriter(oldPacketId, newPacketId, null, metaType);
    }
}
