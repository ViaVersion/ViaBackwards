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
package com.viaversion.viabackwards.protocol.v1_11to1_10.data;

import com.viaversion.viaversion.libs.fastutil.ints.Int2IntMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntOpenHashMap;

public class SplashPotionMappings1_10 {

    private static final Int2IntMap DATA = new Int2IntOpenHashMap(14, 0.99F);

    static {
        DATA.defaultReturnValue(-1);
        DATA.put(2039713, 5); // night vision
        DATA.put(8356754, 7); // invisibility
        DATA.put(2293580, 9); // jump boost
        DATA.put(14981690, 12); // fire resistance
        DATA.put(8171462, 14); // swiftness
        DATA.put(5926017, 17); // slowness
        DATA.put(3035801, 19); // water breathing
        DATA.put(16262179, 21); // instant health
        DATA.put(4393481, 23); // instant damage
        DATA.put(5149489, 25); // poison
        DATA.put(13458603, 28); // regeneration
        DATA.put(9643043, 31); // strength
        DATA.put(4738376, 34); // weakness
        DATA.put(3381504, 36); // luck
    }

    public static int getOldData(int data) {
        return DATA.get(data);
    }
}
