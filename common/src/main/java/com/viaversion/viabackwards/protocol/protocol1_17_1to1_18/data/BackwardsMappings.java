/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.data;

import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntMap;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.protocols.protocol1_18to1_17_1.Protocol1_18To1_17_1;

public final class BackwardsMappings extends com.viaversion.viabackwards.api.data.BackwardsMappings {

    private final Int2ObjectMap<String> blockEntities = new Int2ObjectOpenHashMap<>();

    public BackwardsMappings() {
        super("1.18", "1.17", Protocol1_18To1_17_1.class, true);
    }

    @Override
    protected void loadVBExtras(final JsonObject unmapped, final JsonObject mapped) {
        for (final Object2IntMap.Entry<String> entry : Protocol1_18To1_17_1.MAPPINGS.blockEntityIds().object2IntEntrySet()) {
            blockEntities.put(entry.getIntValue(), entry.getKey());
        }
    }

    public Int2ObjectMap<String> blockEntities() {
        return blockEntities;
    }
}
