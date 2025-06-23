/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2025 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.storage;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.libs.fastutil.objects.Object2ObjectArrayMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2ObjectMap;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.KeyMappings;

public final class RegistryAndTags implements StorableObject {

    private KeyMappings dialogMappings;
    private Object2ObjectMap<String, CompoundTag> dialogs;

    private Object2ObjectMap<String, int[]> dialogTags;

    public void storeRegistry(final KeyMappings dialogMappings, final Object2ObjectMap<String, CompoundTag> dialogs) {
        this.dialogMappings = dialogMappings;
        this.dialogs = dialogs;
    }

    public CompoundTag fromRegistry(final int id) {
        return dialogs.get(dialogMappings.idToKey(id));
    }

    public CompoundTag fromRegistry(final String key) {
        return dialogs.get(Key.stripMinecraftNamespace(key));
    }

    public boolean tagsSent() {
        return dialogTags != null && !dialogTags.isEmpty();
    }

    public void storeTags(final String key, final int[] ids) {
        if (dialogTags == null) {
            dialogTags = new Object2ObjectArrayMap<>();
        }
        dialogTags.put(Key.stripMinecraftNamespace(key), ids);
    }

    public int[] fromKey(final String key) {
        return dialogTags.get(Key.stripMinecraftNamespace(key));
    }

    public CompoundTag[] fromRegistryKey(final String key) {
        final int[] ids = fromKey(key);
        if (ids == null) {
            return null;
        }

        final CompoundTag[] tags = new CompoundTag[ids.length];
        for (int i = 0; i < ids.length; i++) {
            tags[i] = fromRegistry(ids[i]);
        }
        return tags;
    }
}
