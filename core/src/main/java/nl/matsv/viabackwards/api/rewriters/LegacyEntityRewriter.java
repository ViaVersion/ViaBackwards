package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.entities.storage.EntityData;
import nl.matsv.viabackwards.api.entities.storage.MetaStorage;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.entities.ObjectType;
import us.myles.ViaVersion.api.minecraft.metadata.MetaType;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class LegacyEntityRewriter<T extends BackwardsProtocol> extends EntityRewriterBase<T> {
    private final Map<ObjectType, EntityData> objectTypes = new ConcurrentHashMap<>();

    protected LegacyEntityRewriter(T protocol) {
        super(protocol);
    }

    protected LegacyEntityRewriter(T protocol, MetaType displayType, boolean isDisplayJson) {
        super(protocol, displayType, 2, isDisplayJson);
    }

    protected EntityData mapObjectType(ObjectType oldObjectType, ObjectType replacement, int data) {
        EntityData entData = new EntityData(oldObjectType.getId(), true, replacement.getId(), data);
        objectTypes.put(oldObjectType, entData);
        return entData;
    }

    protected EntityData getObjectData(ObjectType type) {
        return objectTypes.get(type);
    }

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
                    List<Metadata> metadata = wrapper.get(newMetaType, 0);
                    wrapper.set(newMetaType, 0,
                            handleMeta(wrapper.user(), wrapper.get(Type.VAR_INT, 0), new MetaStorage(metadata)).getMetaDataList());
                });
            }
        });
    }

    protected void registerMetadataRewriter(int oldPacketId, int newPacketId, Type<List<Metadata>> metaType) {
        registerMetadataRewriter(oldPacketId, newPacketId, null, metaType);
    }

    protected PacketHandler getMobSpawnRewriter(Type<List<Metadata>> metaType) {
        return wrapper -> {
            int entityId = wrapper.get(Type.VAR_INT, 0);
            EntityType type = getEntityType(wrapper.user(), entityId);

            MetaStorage storage = new MetaStorage(wrapper.get(metaType, 0));
            handleMeta(wrapper.user(), entityId, storage);

            EntityData entityData = getEntityData(type);
            if (entityData != null) {
                int replacementId = getOldEntityId(entityData.getReplacementId());
                wrapper.set(Type.VAR_INT, 1, replacementId);
                if (entityData.hasBaseMeta()) {
                    entityData.getDefaultMeta().handle(storage);
                }
            }

            // Rewrite Metadata
            wrapper.set(metaType, 0, storage.getMetaDataList());
        };
    }

    protected PacketHandler getObjectTrackerHandler() {
        return wrapper -> addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), getObjectTypeFromId(wrapper.get(Type.BYTE, 0)));
    }

    protected PacketHandler getTrackerAndMetaHandler(Type<List<Metadata>> metaType, EntityType entityType) {
        return wrapper -> {
            addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), entityType);

            List<Metadata> metaDataList = handleMeta(wrapper.user(), wrapper.get(Type.VAR_INT, 0),
                    new MetaStorage(wrapper.get(metaType, 0))).getMetaDataList();
            wrapper.set(metaType, 0, metaDataList);
        };
    }

    protected EntityType getObjectTypeFromId(int typeId) {
        return getTypeFromId(typeId);
    }
}
