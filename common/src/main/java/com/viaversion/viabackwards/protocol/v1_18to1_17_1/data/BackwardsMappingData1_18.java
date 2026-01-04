/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_18to1_17_1.data;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntMap;
import com.viaversion.viaversion.protocols.v1_17_1to1_18.Protocol1_17_1To1_18;
import com.viaversion.viaversion.protocols.v1_17_1to1_18.data.BlockEntities1_18;

public final class BackwardsMappingData1_18 extends BackwardsMappingData {

    private final Int2ObjectMap<String> blockEntities = new Int2ObjectOpenHashMap<>();

    public BackwardsMappingData1_18() {
        super("1.18", "1.17", Protocol1_17_1To1_18.class);
    }

    @Override
    protected void loadExtras(final CompoundTag data) {
        super.loadExtras(data);

        for (final Object2IntMap.Entry<String> entry : BlockEntities1_18.blockEntityIds().object2IntEntrySet()) {
            blockEntities.put(entry.getIntValue(), entry.getKey());
        }
    }

    public Int2ObjectMap<String> blockEntities() {
        return blockEntities;
    }
}
