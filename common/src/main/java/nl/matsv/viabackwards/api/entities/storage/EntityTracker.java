/*
 * ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.matsv.viabackwards.api.entities.storage;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.data.StoredObject;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.entities.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityTracker extends StoredObject {
    private final Map<BackwardsProtocol, ProtocolEntityTracker> trackers = new HashMap<>();
    private int currentWorldSectionHeight = 16;
    private int currentMinY;

    public EntityTracker(UserConnection user) {
        super(user);
    }

    public void initProtocol(BackwardsProtocol protocol) {
        trackers.put(protocol, new ProtocolEntityTracker());
    }

    @Nullable
    public ProtocolEntityTracker get(BackwardsProtocol protocol) {
        return trackers.get(protocol);
    }

    public Map<BackwardsProtocol, ProtocolEntityTracker> getTrackers() {
        return trackers;
    }

    /**
     * @return amount of chunk sections of the current world (block height / 16)
     */
    public int getCurrentWorldSectionHeight() {
        return currentWorldSectionHeight;
    }

    public void setCurrentWorldSectionHeight(int currentWorldSectionHeight) {
        this.currentWorldSectionHeight = currentWorldSectionHeight;
    }

    /**
     * @return absolute minimum y coordinate of the current world
     */
    public int getCurrentMinY() {
        return currentMinY;
    }

    public void setCurrentMinY(int currentMinY) {
        this.currentMinY = currentMinY;
    }

    public static class ProtocolEntityTracker {
        private final Map<Integer, StoredEntity> entityMap = new ConcurrentHashMap<>();

        public void trackEntityType(int id, EntityType type) {
            entityMap.putIfAbsent(id, new StoredEntity(id, type));
        }

        public void removeEntity(int id) {
            entityMap.remove(id);
        }

        @Nullable
        public EntityType getEntityType(int id) {
            StoredEntity storedEntity = entityMap.get(id);
            return storedEntity != null ? storedEntity.getType() : null;
        }

        @Nullable
        public StoredEntity getEntity(int id) {
            return entityMap.get(id);
        }
    }

    public static final class StoredEntity {
        private final int entityId;
        private final EntityType type;
        private Map<Class<? extends EntityStorage>, EntityStorage> storedObjects;

        private StoredEntity(final int entityId, final EntityType type) {
            this.entityId = entityId;
            this.type = type;
        }

        /**
         * Get an object from the storage
         *
         * @param objectClass The class of the object to get
         * @param <T>         The type of the class you want to get.
         * @return The requested object
         */
        @Nullable
        public <T extends EntityStorage> T get(Class<T> objectClass) {
            return storedObjects != null ? (T) storedObjects.get(objectClass) : null;
        }

        /**
         * Check if the storage has an object
         *
         * @param objectClass The object class to check
         * @return True if the object is in the storage
         */
        public boolean has(Class<? extends EntityStorage> objectClass) {
            return storedObjects != null && storedObjects.containsKey(objectClass);
        }

        /**
         * Put an object into the stored objects based on class
         *
         * @param object The object to store.
         */
        public void put(EntityStorage object) {
            if (storedObjects == null) {
                storedObjects = new ConcurrentHashMap<>();
            }
            storedObjects.put(object.getClass(), object);
        }

        public int getEntityId() {
            return entityId;
        }

        public EntityType getType() {
            return type;
        }

        @Override
        public String toString() {
            return "StoredEntity{" +
                    "entityId=" + entityId +
                    ", type=" + type +
                    ", storedObjects=" + storedObjects +
                    '}';
        }
    }
}
