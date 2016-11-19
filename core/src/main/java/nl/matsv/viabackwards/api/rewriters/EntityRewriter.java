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
import nl.matsv.viabackwards.api.MetaRewriter;
import nl.matsv.viabackwards.api.entities.AbstractEntityType;
import nl.matsv.viabackwards.api.exceptions.RemovedValueException;
import nl.matsv.viabackwards.api.storage.EntityTracker;
import us.myles.ViaVersion.api.ViaVersion;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

@RequiredArgsConstructor
public abstract class EntityRewriter<T extends BackwardsProtocol> extends Rewriter<T> {
    private final Map<AbstractEntityType, Short> entityTypes = new ConcurrentHashMap<>();

    private final List<MetaRewriter> metaRewriters = new ArrayList<>();

    protected AbstractEntityType getEntityType(UserConnection connection, int id) {
        return getEntityTracker(connection).getEntityType(id);
    }

    protected void addTrackedEntity(UserConnection connection, int entityId, AbstractEntityType type) {
        getEntityTracker(connection).trackEntityType(entityId, type);
    }

    protected void rewriteEntityType(AbstractEntityType type, int newId) {
        entityTypes.put(type, (short) newId);
    }

    protected boolean isRewriteEntityType(AbstractEntityType type) {
        return entityTypes.containsKey(type);
    }

    protected short getNewEntityType(AbstractEntityType type) {
        if (!isRewriteEntityType(type))
            return -1;
        return entityTypes.get(type);
    }

    public void registerMetaRewriter(MetaRewriter rewriter) {
        metaRewriters.add(rewriter);
    }

    protected List<Metadata> handleMeta(UserConnection user, int entityId, List<Metadata> metaData) {
        EntityTracker tracker = user.get(EntityTracker.class);
        AbstractEntityType type = tracker.get(getProtocol()).getEntityType(entityId);

        List<Metadata> newMeta = new CopyOnWriteArrayList<>();
        for (Metadata md : metaData) {
            Metadata nmd = md;
            try {
                for (MetaRewriter rewriter : metaRewriters) {
                    if (type != null)
                        nmd = rewriter.handleMetadata(type, nmd);
                    else
                        throw new Exception("Panic, entitytype is null");
                    if (nmd == null)
                        throw new RemovedValueException();
                }
                newMeta.add(nmd);
            } catch (RemovedValueException ignored) {
            } catch (Exception e) {
                if (ViaVersion.getInstance().isDebug()) {
                    Logger log = ViaBackwards.getPlatform().getLogger();
                    log.warning("Unable to handle metadata " + md);
                    log.warning("Full metadata list " + metaData);
                    e.printStackTrace();
                }
            }
        }

        return newMeta;
    }

    protected EntityTracker.ProtocolEntityTracker getEntityTracker(UserConnection user) {
        return user.get(EntityTracker.class).get(getProtocol());
    }
}
