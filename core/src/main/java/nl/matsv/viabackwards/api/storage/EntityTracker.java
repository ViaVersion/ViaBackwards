/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.api.storage;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.entities.AbstractEntityType;
import us.myles.ViaVersion.api.data.StoredObject;
import us.myles.ViaVersion.api.data.UserConnection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityTracker extends StoredObject {
    private Map<BackwardsProtocol, ProtocolEntityTracker> trackers = new ConcurrentHashMap<>();

    public EntityTracker(UserConnection user) {
        super(user);
    }

    public void initProtocol(BackwardsProtocol protocol) {
        trackers.put(protocol, new ProtocolEntityTracker());
    }

    public ProtocolEntityTracker get(BackwardsProtocol protocol) {
        return trackers.get(protocol);
    }

    public class ProtocolEntityTracker {
        private Map<Integer, AbstractEntityType> entityMap = new ConcurrentHashMap<>();

        public void trackEntityType(int id, AbstractEntityType type) {
            if (entityMap.containsKey(id))
                return;
            entityMap.put(id, type);
        }

        public void removeEntity(int id) {
            entityMap.remove(id);
        }

        public AbstractEntityType getEntityType(int id) {
            return entityMap.get(id);
        }

        public boolean containsEntity(int id) {
            return entityMap.containsKey(id);
        }
    }
}
