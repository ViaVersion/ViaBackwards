package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.entities.storage.EntityData;
import nl.matsv.viabackwards.api.entities.storage.EntityObjectData;
import nl.matsv.viabackwards.api.entities.storage.MetaStorage;
import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.entities.ObjectType;
import us.myles.ViaVersion.api.minecraft.metadata.MetaType;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.protocol.ClientboundPacketType;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class LegacyEntityRewriter<T extends BackwardsProtocol> extends EntityRewriterBase<T> {
    private final Map<ObjectType, EntityData> objectTypes = new HashMap<>();

    protected LegacyEntityRewriter(T protocol) {
        super(protocol);
    }

    protected LegacyEntityRewriter(T protocol, MetaType displayType) {
        super(protocol, displayType, 2);
    }

    protected EntityObjectData mapObjectType(ObjectType oldObjectType, ObjectType replacement, int data) {
        EntityObjectData entData = new EntityObjectData(oldObjectType.getId(), true, replacement.getId(), data);
        objectTypes.put(oldObjectType, entData);
        return entData;
    }

    @Nullable
    protected EntityData getObjectData(ObjectType type) {
        return objectTypes.get(type);
    }

    protected void registerRespawn(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT);
                handler(wrapper -> {
                    ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                    clientWorld.setEnvironment(wrapper.get(Type.INT, 0));
                });
            }
        });
    }

    protected void registerJoinGame(ClientboundPacketType packetType, EntityType playerType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Entity ID
                map(Type.UNSIGNED_BYTE); // 1 - Gamemode
                map(Type.INT); // 2 - Dimension
                handler(wrapper -> {
                    ClientWorld clientChunks = wrapper.user().get(ClientWorld.class);
                    clientChunks.setEnvironment(wrapper.get(Type.INT, 1));
                    getEntityTracker(wrapper.user()).trackEntityType(wrapper.get(Type.INT, 0), playerType);
                });
            }
        });
    }

    protected void registerMetadataRewriter(ClientboundPacketType packetType, Type<List<Metadata>> oldMetaType, Type<List<Metadata>> newMetaType) {
        getProtocol().registerOutgoing(packetType, new PacketRemapper() {
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

    protected void registerMetadataRewriter(ClientboundPacketType packetType, Type<List<Metadata>> metaType) {
        registerMetadataRewriter(packetType, null, metaType);
    }

    protected PacketHandler getMobSpawnRewriter(Type<List<Metadata>> metaType) {
        return wrapper -> {
            int entityId = wrapper.get(Type.VAR_INT, 0);
            EntityType type = getEntityType(wrapper.user(), entityId);

            MetaStorage storage = new MetaStorage(wrapper.get(metaType, 0));
            handleMeta(wrapper.user(), entityId, storage);

            EntityData entityData = getEntityData(type);
            if (entityData != null) {
                wrapper.set(Type.VAR_INT, 1, entityData.getReplacementId());
                if (entityData.hasBaseMeta()) {
                    entityData.getDefaultMeta().createMeta(storage);
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

    protected PacketHandler getObjectRewriter(Function<Byte, ObjectType> objectGetter) {
        return wrapper -> {
            ObjectType type = objectGetter.apply(wrapper.get(Type.BYTE, 0));
            if (type == null) {
                ViaBackwards.getPlatform().getLogger().warning("Could not find Entity Type" + wrapper.get(Type.BYTE, 0));
                return;
            }

            EntityData data = getObjectData(type);
            if (data != null) {
                wrapper.set(Type.BYTE, 0, (byte) data.getReplacementId());
                if (data.getObjectData() != -1) {
                    wrapper.set(Type.INT, 0, data.getObjectData());
                }
            }
        };
    }

    protected EntityType getObjectTypeFromId(int typeId) {
        return getTypeFromId(typeId);
    }
}
