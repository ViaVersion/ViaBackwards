/*
 *
 *     Copyright (C) 2016 Matsv
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.matsv.viabackwards.api.rewriters;

import lombok.RequiredArgsConstructor;
import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.MetaRewriter;
import nl.matsv.viabackwards.api.exceptions.RemovedValueException;
import nl.matsv.viabackwards.api.storage.EntityTracker;
import nl.matsv.viabackwards.api.storage.EntityType;
import org.bukkit.Bukkit;
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
    private final Map<Short, Short> entityTypes = new ConcurrentHashMap<>();
    private final Map<Short, Short> objectTypes = new ConcurrentHashMap<>();

    private final List<MetaRewriter> metaRewriters = new ArrayList<>();

    protected short getNewEntityType(UserConnection connection, int id) {
        EntityType type = getEntityType(connection, id);
        if (type.isObject()) {
            return getNewObjectId(type.getEntityType());
        } else {
            return getNewEntityId(type.getEntityType());
        }
    }

    protected EntityType getEntityType(UserConnection connection, int id) {
        return connection.get(EntityTracker.class).getEntityType(id);
    }

    protected void addTrackedEntity(UserConnection connection, int entityId, boolean isObject, short typeId) {
        connection.get(EntityTracker.class).trackEntityType(entityId, new EntityType(isObject, typeId));
    }

    protected void rewriteEntityId(int oldId, int newId) {
        entityTypes.put((short) oldId, (short) newId);
    }

    protected boolean isRewriteEntityId(short id) {
        return entityTypes.containsKey(id);
    }

    protected short getNewEntityId(short oldId) {
        if (!isRewriteEntityId(oldId))
            return oldId;
        return entityTypes.get(oldId);
    }

    protected void rewriteObjectId(int oldId, int newId) {
        objectTypes.put((short) oldId, (short) newId);
    }

    protected boolean isRewriteObjectId(short id) {
        return objectTypes.containsKey(id);
    }

    protected short getNewObjectId(short oldId) {
        if (!isRewriteObjectId(oldId))
            return oldId;
        return objectTypes.get(oldId);
    }

    public void registerMetaRewriter(MetaRewriter rewriter) {
        metaRewriters.add(rewriter);
    }

    protected List<Metadata> handleMeta(UserConnection userConnection, int entityId, List<Metadata> metaData) {
        EntityTracker tracker = userConnection.get(EntityTracker.class);
        EntityType type = tracker.getEntityType(entityId);

        List<Metadata> newMeta = new CopyOnWriteArrayList<>();
        for (Metadata md : metaData) {
            Metadata nmd = md;
            try {
                for (MetaRewriter rewriter : metaRewriters) {
                    if (type != null)
                        nmd = rewriter.handleMetadata(type.isObject(), type.getEntityType(), nmd);
                    else
                        nmd = rewriter.handleMetadata(false, -1, nmd);
                    if (nmd == null)
                        throw new RemovedValueException();
                }
                newMeta.add(nmd);
            } catch (RemovedValueException ignored) {
            } catch (Exception e) {
                if (ViaVersion.getInstance().isDebug()) {
                    Logger log = Bukkit.getPluginManager().getPlugin("ViaBackwards").getLogger();
                    log.warning("Unable to handle metadata " + md);
                    log.warning("Full metadata list " + metaData);
                    e.printStackTrace();
                }
            }
        }

        return newMeta;
    }
}
