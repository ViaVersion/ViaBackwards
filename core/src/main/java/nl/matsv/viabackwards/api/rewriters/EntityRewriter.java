/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.api.rewriters;

import lombok.RequiredArgsConstructor;
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
import us.myles.ViaVersion.api.entities.ObjectType;
import us.myles.ViaVersion.api.minecraft.metadata.MetaType;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_9;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.exception.CancelException;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ChatRewriter;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@RequiredArgsConstructor
public abstract class EntityRewriter<T extends BackwardsProtocol> extends Rewriter<T> {
    private final Map<EntityType, EntityData> entityTypes = new ConcurrentHashMap<>();
    private final Map<ObjectType, EntityData> objectTypes = new ConcurrentHashMap<>();
    private final List<MetaHandlerSettings> metaHandlers = new ArrayList<>();

    private MetaType displayNameMetaType = MetaType1_9.String;
    private int displayNameIndex = 2;
    private boolean isDisplayNameJson;

    protected EntityType getEntityType(UserConnection connection, int id) {
        return getEntityTracker(connection).getEntityType(id);
    }

    protected void addTrackedEntity(UserConnection connection, int entityId, EntityType type) {
        getEntityTracker(connection).trackEntityType(entityId, type);
    }

    protected boolean hasData(EntityType type) {
        return entityTypes.containsKey(type);
    }

    protected Optional<EntityData> getEntityData(EntityType type) {
        return Optional.ofNullable(entityTypes.get(type));
    }

    protected Optional<EntityData> getObjectData(ObjectType type) {
        return Optional.ofNullable(objectTypes.get(type));
    }

    protected EntityData regEntType(EntityType oldEnt, EntityType replacement) {
        return regEntType(oldEnt, (short) replacement.getId());
    }

    private EntityData regEntType(EntityType oldEnt, short replacementId) {
        EntityData data = new EntityData(oldEnt.getId(), false, replacementId, -1);
        entityTypes.put(oldEnt, data);
        return data;
    }

    protected EntityData regObjType(ObjectType oldObj, ObjectType replacement, int data) {
        return regObjType(oldObj, (short) replacement.getId(), data);
    }

    private EntityData regObjType(ObjectType oldObj, short replacementId, int data) {
        EntityData entData = new EntityData(oldObj.getId(), true, replacementId, data);
        objectTypes.put(oldObj, entData);
        return entData;
    }

    public MetaHandlerSettings registerMetaHandler() {
        MetaHandlerSettings settings = new MetaHandlerSettings();
        metaHandlers.add(settings);
        return settings;
    }

    protected MetaStorage handleMeta(UserConnection user, int entityId, MetaStorage storage) throws Exception {
        Optional<EntityTracker.StoredEntity> optEntity = getEntityTracker(user).getEntity(entityId);
        if (!optEntity.isPresent()) {
            if (!Via.getConfig().isSuppressMetadataErrors())
                ViaBackwards.getPlatform().getLogger().warning("Metadata for entity id: " + entityId + " not sent because the entity doesn't exist. " + storage);
            throw new CancelException();
        }

        EntityTracker.StoredEntity entity = optEntity.get();
        EntityType type = entity.getType();

        List<Metadata> newList = new ArrayList<>();

        for (MetaHandlerSettings settings : metaHandlers) {
            List<Metadata> extraData = null;
            for (Metadata md : storage.getMetaDataList()) {
                Metadata nmd = md;
                MetaHandlerEvent event = null;
                try {
                    if (settings.isGucci(type, nmd)) {
                        event = new MetaHandlerEvent(user, entity, nmd.getId(), nmd, storage);
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
        Optional<Metadata> opMd = storage.get(displayNameIndex);
        if (opMd.isPresent()) {
            Optional<EntityData> opEd = getEntityData(type);
            if (opEd.isPresent()) {
                Metadata data = opMd.get();
                EntityData entData = opEd.get();
                if (entData.getMobName() != null &&
                        (data.getValue() == null || ((String) data.getValue()).isEmpty()) &&
                        data.getMetaType().getTypeID() == displayNameMetaType.getTypeID()) {
                    String mobName = entData.getMobName();
                    if (isDisplayNameJson) {
                        mobName = ChatRewriter.legacyTextToJson(mobName);
                    }
                    data.setValue(mobName);
                }
            }

        }

        return storage;
    }

    /**
     * Helper method to handle a metadata list packet.
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
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        List<Metadata> metadata = wrapper.get(newMetaType, 0);
                        wrapper.set(newMetaType, 0,
                                handleMeta(wrapper.user(), wrapper.get(Type.VAR_INT, 0), new MetaStorage(metadata)).getMetaDataList());
                    }
                });
            }
        });
    }

    protected void registerMetadataRewriter(int oldPacketId, int newPacketId, Type<List<Metadata>> metaType) {
        registerMetadataRewriter(oldPacketId, newPacketId, null, metaType);
    }

    /**
     * Helper method to handle player, painting, or xp orb trackers without meta changes.
     */
    protected void registerExtraTracker(int packetId, EntityType entityType, Type intType) {
        getProtocol().registerOutgoing(State.PLAY, packetId, packetId, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(intType); // 0 - Entity id
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(wrapper.user(), (int) wrapper.get(intType, 0), entityType);
                    }
                });
            }
        });
    }

    protected void registerExtraTracker(int packetId, EntityType entityType) {
        registerExtraTracker(packetId, entityType, Type.VAR_INT);
    }

    /**
     * Helper method to handle the destroy entities packet.
     */
    protected void registerEntityDestroy(int oldPacketId, int newPacketId) {
        getProtocol().registerOutgoing(State.PLAY, oldPacketId, newPacketId, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT_ARRAY); // 0 - Entity ids
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        EntityTracker.ProtocolEntityTracker tracker = getEntityTracker(wrapper.user());
                        for (int entity : wrapper.get(Type.VAR_INT_ARRAY, 0)) {
                            tracker.removeEntity(entity);
                        }
                    }
                });
            }
        });
    }

    protected void registerEntityDestroy(int packetId) {
        registerEntityDestroy(packetId, packetId);
    }

    protected PacketHandler getObjectTrackerHandler() {
        return new PacketHandler() {
            @Override
            public void handle(PacketWrapper wrapper) throws Exception {
                addTrackedEntity(wrapper.user(), wrapper.get(Type.VAR_INT, 0), getObjectTypeFromId(wrapper.get(Type.BYTE, 0)));
            }
        };
    }

    protected PacketHandler getTrackerHandler(Type intType, int typeIndex) {
        return new PacketHandler() {
            @Override
            public void handle(PacketWrapper wrapper) throws Exception {
                Number id = (Number) wrapper.get(intType, typeIndex);
                addTrackedEntity(wrapper.user(), wrapper.get(Type.VAR_INT, 0), getTypeFromId(id.intValue()));
            }
        };
    }

    protected PacketHandler getTrackerHandler() {
        return getTrackerHandler(Type.VAR_INT, 1);
    }

    protected PacketHandler getTrackerHandler(EntityType entityType, Type intType) {
        return new PacketHandler() {
            @Override
            public void handle(PacketWrapper wrapper) throws Exception {
                addTrackedEntity(wrapper.user(), (int) wrapper.get(intType, 0), entityType);
            }
        };
    }

    protected PacketHandler getMobSpawnRewriter(Type<List<Metadata>> metaType) {
        return new PacketHandler() {
            @Override
            public void handle(PacketWrapper wrapper) throws Exception {
                int entityId = wrapper.get(Type.VAR_INT, 0);
                EntityType type = getEntityType(wrapper.user(), entityId);

                MetaStorage storage = new MetaStorage(wrapper.get(metaType, 0));
                handleMeta(wrapper.user(), entityId, storage);

                Optional<EntityData> optEntDat = getEntityData(type);
                if (optEntDat.isPresent()) {
                    EntityData data = optEntDat.get();

                    int replacementId = getOldEntityId(data.getReplacementId());
                    wrapper.set(Type.VAR_INT, 1, replacementId);
                    if (data.hasBaseMeta()) {
                        data.getDefaultMeta().handle(storage);
                    }
                }

                // Rewrite Metadata
                wrapper.set(metaType, 0, storage.getMetaDataList());
            }
        };
    }

    protected PacketHandler getTrackerAndMetaHandler(Type<List<Metadata>> metaType, EntityType entityType) {
        return new PacketHandler() {
            @Override
            public void handle(PacketWrapper wrapper) throws Exception {
                addTrackedEntity(wrapper.user(), wrapper.get(Type.VAR_INT, 0), entityType);

                List<Metadata> metaDataList = handleMeta(wrapper.user(), wrapper.get(Type.VAR_INT, 0),
                        new MetaStorage(wrapper.get(metaType, 0))).getMetaDataList();
                wrapper.set(metaType, 0, metaDataList);
            }
        };
    }

    protected PacketHandler getDimensionHandler(int index) {
        return new PacketHandler() {
            @Override
            public void handle(PacketWrapper wrapper) throws Exception {
                ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                int dimensionId = wrapper.get(Type.INT, index);
                clientWorld.setEnvironment(dimensionId);
            }
        };
    }

    protected EntityTracker.ProtocolEntityTracker getEntityTracker(UserConnection user) {
        return user.get(EntityTracker.class).get(getProtocol());
    }

    protected MetaType getDisplayNameMetaType() {
        return displayNameMetaType;
    }

    protected void setDisplayNameMetaType(MetaType displayNameMetaType) {
        this.displayNameMetaType = displayNameMetaType;
    }

    protected int getDisplayNameIndex() {
        return displayNameIndex;
    }

    protected void setDisplayNameIndex(int displayNameIndex) {
        this.displayNameIndex = displayNameIndex;
    }

    protected boolean isDisplayNameJson() {
        return isDisplayNameJson;
    }

    protected void setDisplayNameJson(boolean displayNameJson) {
        isDisplayNameJson = displayNameJson;
    }

    protected abstract EntityType getTypeFromId(int typeId);

    protected EntityType getObjectTypeFromId(int typeId) {
        return getTypeFromId(typeId);
    }

    // Only needs to be overriden when getMobSpawnTracker is used
    protected Optional<Integer> getOptOldEntityId(int newId) {
        return Optional.of(newId);
    }

    protected int getOldEntityId(int newId) {
        return newId;
    }
}
