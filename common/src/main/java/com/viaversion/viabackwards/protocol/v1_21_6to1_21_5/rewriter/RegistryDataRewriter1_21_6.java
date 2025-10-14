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
package com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.BackwardsRegistryRewriter;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.storage.RegistryAndTags;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.libs.fastutil.objects.Object2ObjectArrayMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2ObjectMap;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.KeyMappings;

public final class RegistryDataRewriter1_21_6 extends BackwardsRegistryRewriter {

    public RegistryDataRewriter1_21_6(final BackwardsProtocol<?, ?, ?, ?> protocol) {
        super(protocol);

        remove("dialog"); // Tracked and now removed
    }

    @Override
    public RegistryEntry[] handle(final UserConnection connection, final String key, final RegistryEntry[] entries) {
        if (Key.stripMinecraftNamespace(key).equals("dialog")) {
            final String[] keys = new String[entries.length];
            for (int i = 0; i < entries.length; i++) {
                keys[i] = Key.stripMinecraftNamespace(entries[i].key());
            }

            final Object2ObjectMap<String, CompoundTag> dialogs = new Object2ObjectArrayMap<>();
            for (final RegistryEntry entry : entries) {
                if (entry.tag() instanceof final CompoundTag tag) {
                    dialogs.put(Key.stripMinecraftNamespace(entry.key()), tag);
                }
            }

            final RegistryAndTags registryAndTags = connection.get(RegistryAndTags.class);
            registryAndTags.storeRegistry(new KeyMappings(keys), dialogs);
        }
        return super.handle(connection, key, entries);
    }
}
