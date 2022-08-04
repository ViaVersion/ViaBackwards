/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2022 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.storage;

import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.Protocol1_18_2To1_19;
import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class DimensionRegistryStorage implements StorableObject {

    private final Map<String, CompoundTag> dimensions = new HashMap<>();
    private final Int2ObjectMap<CompoundTag> chatTypes = new Int2ObjectOpenHashMap<>();

    public @Nullable CompoundTag dimension(final String dimensionKey) {
        final CompoundTag compoundTag = dimensions.get(dimensionKey);
        return compoundTag != null ? compoundTag.clone() : null;
    }

    public void addDimension(final String dimensionKey, final CompoundTag dimension) {
        dimensions.put(dimensionKey, dimension);
    }

    public @Nullable CompoundTag chatType(final int id) {
        return chatTypes.isEmpty() ? Protocol1_18_2To1_19.MAPPINGS.chatType(id) : chatTypes.get(id);
    }

    public void addChatType(final int id, final CompoundTag chatType) {
        chatTypes.put(id, chatType);
    }

    public void clear() {
        dimensions.clear();
        chatTypes.clear();
    }

    @Override
    public boolean clearOnServerSwitch() {
        return false;
    }
}
