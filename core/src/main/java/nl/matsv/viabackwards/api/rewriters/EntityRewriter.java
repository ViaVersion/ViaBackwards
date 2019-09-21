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
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.entities.ObjectType;
import us.myles.ViaVersion.api.minecraft.metadata.MetaType;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_9;
import us.myles.ViaVersion.exception.CancelException;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ChatRewriter;

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
        if (!entityTypes.containsKey(type))
            return Optional.empty();
        return Optional.of(entityTypes.get(type));
    }

    protected Optional<EntityData> getObjectData(ObjectType type) {
        if (!objectTypes.containsKey(type))
            return Optional.empty();
        return Optional.of(objectTypes.get(type));
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
}
