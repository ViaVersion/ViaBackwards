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
package com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.data;

import com.viaversion.viaversion.libs.fastutil.ints.Int2IntMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntOpenHashMap;

public final class MapColorRewrites {

    private static final Int2IntMap MAPPINGS = new Int2IntOpenHashMap();

    static {
        MAPPINGS.put(236, 85); // (70, 70, 70) -> (65, 65, 65)
        MAPPINGS.put(237, 27); // (86, 86, 86) -> (88, 88, 88)
        MAPPINGS.put(238, 45); // (100, 100, 100) -> (96, 96, 96)
        MAPPINGS.put(239, 84); // (52, 52, 52) -> (53, 53, 53)
        MAPPINGS.put(240, 144); // (152, 123, 103) -> (147, 124, 113)
        MAPPINGS.put(241, 145); // (186, 150, 126) -> (180, 152, 138)
        MAPPINGS.put(242, 146); // (216, 175, 147) -> (209, 177, 161)
        MAPPINGS.put(243, 147); // (114, 92, 77) -> (110, 93, 85)
        MAPPINGS.put(244, 127); // (89, 117, 105) -> (48, 115, 112)
        MAPPINGS.put(245, 226); // (109, 144, 129) -> (58, 142, 140)
        MAPPINGS.put(246, 124); // (127, 167, 150) -> (64, 154, 150)
        MAPPINGS.put(247, 227); // (67, 88, 79) -> (30, 75, 74)
    }

    public static int getMappedColor(int color) {
        return MAPPINGS.getOrDefault(color, -1);
    }
}
