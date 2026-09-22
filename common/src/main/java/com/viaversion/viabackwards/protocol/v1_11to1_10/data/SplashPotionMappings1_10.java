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
package com.viaversion.viabackwards.protocol.v1_11to1_10.data;

import com.viaversion.viaversion.libs.fastutil.ints.Int2IntMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntOpenHashMap;

public class SplashPotionMappings1_10 {

    private static final Int2IntMap DATA = new Int2IntOpenHashMap(26, 0.99F);

    static {
        DATA.defaultReturnValue(-1);
        DATA.put(0x1F1FA1, 5); // night vision
        DATA.put(0xC2FF66, 5); // night vision 1.19.4
        DATA.put(0x7F8392, 7); // invisibility
        DATA.put(0xF6F6F6, 7); // invisibility 1.19.4
        DATA.put(0x22FF4C, 9); // jump boost
        DATA.put(0xFDFF84, 9); // jump boost 1.19.4
        DATA.put(0xE49A3A, 12); // fire resistance
        DATA.put(0xFF9900, 12); // fire resistance 1.19.4
        DATA.put(0x7CAFC6, 14); // swiftness
        DATA.put(0x33EBFF, 14); // swiftness 1.19.4
        DATA.put(0x5A6C81, 17); // slowness
        DATA.put(0x8BAFE0, 17); // slowness 1.19.4
        DATA.put(0x2E5299, 19); // water breathing
        DATA.put(0x98DAC0, 19); // water breathing 1.19.4
        DATA.put(0xF82423, 21); // instant health
        DATA.put(0x430A09, 23); // instant damage
        DATA.put(0xA9656A, 23); // instant damage 1.19.4
        DATA.put(0x4E9331, 25); // poison
        DATA.put(0x87A363, 25); // poison 1.19.4
        DATA.put(0xCD5CAB, 28); // regeneration
        DATA.put(0x932423, 31); // strength
        DATA.put(0xFFC700, 31); // strength 1.19.4
        DATA.put(0x484D48, 34); // weakness
        DATA.put(0x339900, 36); // luck
        DATA.put(0x59C106, 36); // luck 1.19.4
    }

    public static int getOldData(int data) {
        // newer servers send as ARGB
        return DATA.get(data & 0xFFFFFF);
    }
}
