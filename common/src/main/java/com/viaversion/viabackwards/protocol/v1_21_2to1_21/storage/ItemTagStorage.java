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
package com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage;

import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.util.Key;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ItemTagStorage implements StorableObject {

    private Map<String, int[]> itemTags = new HashMap<>();

    public int @Nullable [] itemTag(final String key) {
        return itemTags.get(Key.stripMinecraftNamespace(key));
    }

    public void readItemTags(final PacketWrapper wrapper) {
        final int length = wrapper.passthrough(Types.VAR_INT);
        for (int i = 0; i < length; i++) {
            final String registryKey = wrapper.passthrough(Types.STRING);
            final int tagsSize = wrapper.passthrough(Types.VAR_INT);

            final boolean itemRegistry = Key.stripMinecraftNamespace(registryKey).equals("item");
            if (itemRegistry) {
                this.itemTags = new HashMap<>(tagsSize);
            }

            for (int j = 0; j < tagsSize; j++) {
                final String key = wrapper.passthrough(Types.STRING);
                final int[] ids = wrapper.passthrough(Types.VAR_INT_ARRAY_PRIMITIVE);
                if (itemRegistry) {
                    this.itemTags.put(Key.stripMinecraftNamespace(key), ids);
                }
            }
        }
    }
}
