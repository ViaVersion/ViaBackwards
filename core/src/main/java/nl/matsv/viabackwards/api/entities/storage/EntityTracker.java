/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.api.entities.storage;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.data.StoredObject;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.entities.EntityType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityTracker extends StoredObject {
    private final Map<BackwardsProtocol, ProtocolEntityTracker> trackers = new ConcurrentHashMap<>();

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
        private final Map<Class<? extends EntityStorage>, EntityStorage> storedObjects = new ConcurrentHashMap<>();

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
            return (T) storedObjects.get(objectClass);
        }

        /**
         * Check if the storage has an object
         *
         * @param objectClass The object class to check
         * @return True if the object is in the storage
         */
        public boolean has(Class<? extends EntityStorage> objectClass) {
            return storedObjects.containsKey(objectClass);
        }

        /**
         * Put an object into the stored objects based on class
         *
         * @param object The object to store.
         */
        public void put(EntityStorage object) {
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
