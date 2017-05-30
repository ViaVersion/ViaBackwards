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
import nl.matsv.viabackwards.api.entities.AbstractEntityType;
import nl.matsv.viabackwards.api.entities.AbstractObjectType;
import nl.matsv.viabackwards.api.exceptions.RemovedValueException;
import nl.matsv.viabackwards.api.storage.EntityData;
import nl.matsv.viabackwards.api.storage.EntityTracker;
import nl.matsv.viabackwards.api.v2.MetaHandlerEvent;
import nl.matsv.viabackwards.api.v2.MetaHandlerSettings;
import nl.matsv.viabackwards.api.v2.MetaStorage;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

@RequiredArgsConstructor
public abstract class EntityRewriter<T extends BackwardsProtocol> extends Rewriter<T> {
    private final Map<AbstractEntityType, EntityData> entityTypes = new ConcurrentHashMap<>();
    private final Map<AbstractObjectType, EntityData> objectTypes = new ConcurrentHashMap<>();
    private final List<MetaHandlerSettings> metaHandlers = new ArrayList<>();

    protected AbstractEntityType getEntityType(UserConnection connection, int id) {
        return getEntityTracker(connection).getEntityType(id);
    }

    protected void addTrackedEntity(UserConnection connection, int entityId, AbstractEntityType type) {
        getEntityTracker(connection).trackEntityType(entityId, type);
    }

    protected Optional<EntityData> getEntityData(AbstractEntityType type) {
        if (!entityTypes.containsKey(type))
            return Optional.empty();
        return Optional.of(entityTypes.get(type));
    }

    protected Optional<EntityData> getObjectData(AbstractObjectType type) {
        if (!objectTypes.containsKey(type))
            return Optional.empty();
        return Optional.of(objectTypes.get(type));
    }

    protected EntityData regEntType(AbstractEntityType oldEnt, AbstractEntityType replacement) {
        return regEntType(oldEnt, (short) replacement.getId());
    }

    private EntityData regEntType(AbstractEntityType oldEnt, short replacementId) {
        EntityData data = new EntityData(oldEnt.getId(), false, replacementId, -1);
        entityTypes.put(oldEnt, data);
        return data;
    }

    protected EntityData regObjType(AbstractObjectType oldObj, AbstractObjectType replacement, int data) {
        return regObjType(oldObj, (short) replacement.getId(), data);
    }

    private EntityData regObjType(AbstractObjectType oldObj, short replacementId, int data) {
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
        EntityTracker tracker = user.get(EntityTracker.class);

        AbstractEntityType type = tracker.get(getProtocol()).getEntityType(entityId);

        List<Metadata> newList = new CopyOnWriteArrayList<>();

        for (MetaHandlerSettings settings : metaHandlers) {
            for (Metadata md : storage.getMetaDataList()) {
                Metadata nmd = md;
                try {
                    if (settings.isGucci(type, nmd))
                        nmd = settings.getHandler().handle(new MetaHandlerEvent(type, nmd.getId(), nmd, storage));

                    if (nmd == null)
                        throw new RemovedValueException();
                    newList.add(nmd);
                } catch (RemovedValueException ignored) {
                } catch (Exception e) {
                    if (Via.getManager().isDebug()) {
                        Logger log = ViaBackwards.getPlatform().getLogger();
                        log.warning("Unable to handle metadata " + nmd);
                        log.warning("Full metadata list " + storage);
                        e.printStackTrace();
                    }
                }
            }
            storage.setMetaDataList(new ArrayList<>(newList));
            newList.clear();
        }

        return storage;
    }

    protected EntityTracker.ProtocolEntityTracker getEntityTracker(UserConnection user) {
        return user.get(EntityTracker.class).get(getProtocol());
    }
}
