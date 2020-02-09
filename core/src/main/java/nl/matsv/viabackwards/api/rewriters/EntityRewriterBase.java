package nl.matsv.viabackwards.api.rewriters;

import com.google.common.base.Preconditions;
import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.entities.meta.MetaHandlerEvent;
import nl.matsv.viabackwards.api.entities.meta.MetaHandlerSettings;
import nl.matsv.viabackwards.api.entities.storage.EntityData;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.api.entities.storage.MetaStorage;
import nl.matsv.viabackwards.api.exceptions.RemovedValueException;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.minecraft.metadata.MetaType;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_9;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.exception.CancelException;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public abstract class EntityRewriterBase<T extends BackwardsProtocol> extends Rewriter<T> {
    private final Map<EntityType, EntityData> entityTypes = new HashMap<>();
    private final List<MetaHandlerSettings> metaHandlers = new ArrayList<>();
    private final MetaType displayNameMetaType;
    private final int displayNameIndex;

    protected EntityRewriterBase(T protocol) {
        this(protocol, MetaType1_9.String, 2);
    }

    protected EntityRewriterBase(T protocol, MetaType displayNameMetaType, int displayNameIndex) {
        super(protocol);
        this.displayNameMetaType = displayNameMetaType;
        this.displayNameIndex = displayNameIndex;
    }

    protected EntityType getEntityType(UserConnection connection, int id) {
        return getEntityTracker(connection).getEntityType(id);
    }

    protected void addTrackedEntity(PacketWrapper wrapper, int entityId, EntityType type) throws Exception {
        getEntityTracker(wrapper.user()).trackEntityType(entityId, type);
    }

    protected boolean hasData(EntityType type) {
        return entityTypes.containsKey(type);
    }

    protected EntityData getEntityData(EntityType type) {
        return entityTypes.get(type);
    }

    protected EntityData mapEntity(EntityType oldType, EntityType replacement) {
        Preconditions.checkArgument(oldType.getClass() == replacement.getClass());

        // Already rewrite the id here
        EntityData data = new EntityData(oldType.getId(), getOldEntityId(replacement.getId()));
        entityTypes.put(oldType, data);
        return data;
    }

    public MetaHandlerSettings registerMetaHandler() {
        MetaHandlerSettings settings = new MetaHandlerSettings();
        metaHandlers.add(settings);
        return settings;
    }

    protected MetaStorage handleMeta(UserConnection user, int entityId, MetaStorage storage) throws Exception {
        EntityTracker.StoredEntity storedEntity = getEntityTracker(user).getEntity(entityId);
        if (storedEntity == null) {
            if (!Via.getConfig().isSuppressMetadataErrors()) {
                ViaBackwards.getPlatform().getLogger().warning("Metadata for entity id: " + entityId + " not sent because the entity doesn't exist. " + storage);
            }
            throw CancelException.CACHED;
        }

        EntityType type = storedEntity.getType();
        List<Metadata> newList = new ArrayList<>();
        for (MetaHandlerSettings settings : metaHandlers) {
            List<Metadata> extraData = null;
            for (Metadata md : storage.getMetaDataList()) {
                Metadata nmd = md;
                MetaHandlerEvent event = null;
                try {
                    if (settings.isGucci(type, nmd)) {
                        event = new MetaHandlerEvent(user, storedEntity, nmd.getId(), nmd, storage);
                        nmd = settings.getHandler().handle(event);

                        if (event.getExtraData() != null) {
                            (extraData != null ? extraData : (extraData = new ArrayList<>())).addAll(event.getExtraData());
                            event.clearExtraData();
                        }
                    }

                    if (nmd == null) {
                        throw RemovedValueException.EX;
                    }

                    newList.add(nmd);
                } catch (RemovedValueException e) {
                    // add the additionally created data here in case of an interruption
                    if (event != null && event.getExtraData() != null) {
                        (extraData != null ? extraData : (extraData = new ArrayList<>())).addAll(event.getExtraData());
                    }
                } catch (Exception e) {
                    Logger log = ViaBackwards.getPlatform().getLogger();
                    log.warning("Unable to handle metadata " + nmd);
                    log.warning("Full metadata list " + storage);
                    e.printStackTrace();
                }
            }

            List<Metadata> newData = new ArrayList<>(newList);
            if (extraData != null) {
                newData.addAll(extraData);
            }

            storage.setMetaDataList(newData);
            newList.clear();
        }

        // Handle Entity Name
        Metadata data = storage.get(displayNameIndex);
        if (data != null) {
            EntityData entityData = getEntityData(type);
            if (entityData != null) {
                if (entityData.getMobName() != null &&
                        (data.getValue() == null || ((String) data.getValue()).isEmpty()) &&
                        data.getMetaType().getTypeID() == displayNameMetaType.getTypeID()) {
                    data.setValue(entityData.getMobName());
                }
            }
        }

        return storage;
    }

    public void registerRespawn(int oldPacketId, int newPacketId) {
        protocol.registerOutgoing(State.PLAY, oldPacketId, newPacketId, new PacketRemapper() {
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

    public void registerJoinGame(int oldPacketId, int newPacketId, EntityType playerType) {
        protocol.registerOutgoing(State.PLAY, oldPacketId, newPacketId, new PacketRemapper() {
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

    /**
     * Helper method to handle player, painting, or xp orb trackers without meta changes.
     */
    protected void registerExtraTracker(int packetId, EntityType entityType, Type intType) {
        getProtocol().registerOutgoing(State.PLAY, packetId, packetId, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(intType); // 0 - Entity id
                handler(wrapper -> addTrackedEntity(wrapper, (int) wrapper.get(intType, 0), entityType));
            }
        });
    }

    protected void registerExtraTracker(int packetId, EntityType entityType) {
        registerExtraTracker(packetId, entityType, Type.VAR_INT);
    }

    protected void registerEntityDestroy(int oldPacketId, int newPacketId) {
        getProtocol().registerOutgoing(State.PLAY, oldPacketId, newPacketId, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT_ARRAY_PRIMITIVE); // 0 - Entity ids
                handler(wrapper -> {
                    EntityTracker.ProtocolEntityTracker tracker = getEntityTracker(wrapper.user());
                    for (int entity : wrapper.get(Type.VAR_INT_ARRAY_PRIMITIVE, 0)) {
                        tracker.removeEntity(entity);
                    }
                });
            }
        });
    }

    protected void registerEntityDestroy(int packetId) {
        registerEntityDestroy(packetId, packetId);
    }

    // ONLY TRACKS, DOESN'T REWRITE IDS
    protected PacketHandler getTrackerHandler(Type intType, int typeIndex) {
        return wrapper -> {
            Number id = (Number) wrapper.get(intType, typeIndex);
            addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), getTypeFromId(id.intValue()));
        };
    }

    protected PacketHandler getTrackerHandler() {
        return getTrackerHandler(Type.VAR_INT, 1);
    }

    protected PacketHandler getTrackerHandler(EntityType entityType, Type intType) {
        return wrapper -> addTrackedEntity(wrapper, (int) wrapper.get(intType, 0), entityType);
    }

    protected PacketHandler getDimensionHandler(int index) {
        return wrapper -> {
            ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
            int dimensionId = wrapper.get(Type.INT, index);
            clientWorld.setEnvironment(dimensionId);
        };
    }

    public EntityTracker.ProtocolEntityTracker getEntityTracker(UserConnection user) {
        return user.get(EntityTracker.class).get(getProtocol());
    }

    protected abstract EntityType getTypeFromId(int typeId);

    protected int getOldEntityId(int newId) {
        return newId;
    }
}
