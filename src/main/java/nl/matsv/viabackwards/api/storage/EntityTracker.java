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

package nl.matsv.viabackwards.api.storage;

import us.myles.ViaVersion.api.data.StoredObject;
import us.myles.ViaVersion.api.data.UserConnection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityTracker extends StoredObject {
    private Map<Integer, EntityType> entityMap = new ConcurrentHashMap<>();

    public EntityTracker(UserConnection user) {
        super(user);
    }

    public void trackEntityType(int id, EntityType type) {
        if (entityMap.containsKey(id))
            return;
        entityMap.put(id, type);
    }

    public void removeEntity(int id) {
        entityMap.remove(id);
    }

    public EntityType getEntityType(int id) {
        return entityMap.get(id);
    }

    public boolean containsEntity(int id) {
        return entityMap.containsKey(id);
    }
}
