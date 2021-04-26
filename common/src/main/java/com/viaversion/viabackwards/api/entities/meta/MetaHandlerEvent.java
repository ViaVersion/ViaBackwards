/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
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

package com.viaversion.viabackwards.api.entities.meta;

import com.viaversion.viabackwards.api.entities.storage.EntityTracker;
import com.viaversion.viabackwards.api.entities.storage.MetaStorage;
import org.checkerframework.checker.nullness.qual.Nullable;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;

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
    public @Nullable List<Metadata> getExtraData() {
        return extraData;
    }
}
