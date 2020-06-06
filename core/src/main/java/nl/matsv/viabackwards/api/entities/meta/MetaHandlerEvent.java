/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.api.entities.meta;

import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.api.entities.storage.MetaStorage;
import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;

import java.util.ArrayList;
import java.util.List;

public class MetaHandlerEvent {
    private final UserConnection user;
    private final EntityTracker.StoredEntity entity;
    private final int index;
    private final Metadata data;
    private final MetaStorage storage;
    private List<Metadata> extraData;

    public MetaHandlerEvent(UserConnection user, EntityTracker.StoredEntity entity, int index, Metadata data, MetaStorage storage) {
        this.user = user;
        this.entity = entity;
        this.index = index;
        this.data = data;
        this.storage = storage;
    }

    public boolean hasData() {
        return data != null;
    }

    public Metadata getMetaByIndex(int index) {
        for (Metadata meta : storage.getMetaDataList()) {
            if (index == meta.getId()) {
                return meta;
            }
        }
        return null;
    }

    public void clearExtraData() {
        extraData = null;
    }

    public void createMeta(Metadata metadata) {
        (extraData != null ? extraData : (extraData = new ArrayList<>())).add(metadata);
    }

    public UserConnection getUser() {
        return user;
    }

    public EntityTracker.StoredEntity getEntity() {
        return entity;
    }

    public int getIndex() {
        return index;
    }

    public Metadata getData() {
        return data;
    }

    public MetaStorage getStorage() {
        return storage;
    }

    /**
     * May be null, use {@link #createMeta(Metadata)} for adding metadata.
     */
    @Nullable
    public List<Metadata> getExtraData() {
        return extraData;
    }
}
