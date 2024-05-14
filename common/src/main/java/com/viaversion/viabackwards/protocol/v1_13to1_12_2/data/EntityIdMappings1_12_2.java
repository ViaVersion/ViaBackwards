/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2024 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_13to1_12_2.data;

import com.viaversion.viaversion.libs.fastutil.ints.Int2IntMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntOpenHashMap;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.data.EntityIdMappings1_13;

public class EntityIdMappings1_12_2 {
    private static final Int2IntMap TYPES = new Int2IntOpenHashMap();

    static {
        TYPES.defaultReturnValue(-1);
        for (Int2IntMap.Entry entry : EntityIdMappings1_13.getEntityTypes().int2IntEntrySet()) {
            EntityIdMappings1_12_2.TYPES.put(entry.getIntValue(), entry.getIntKey());
        }
    }

    public static int getOldId(int type1_13) {
        return TYPES.get(type1_13);
    }
}
