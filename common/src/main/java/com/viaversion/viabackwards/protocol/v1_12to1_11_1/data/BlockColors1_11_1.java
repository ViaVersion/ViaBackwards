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

package com.viaversion.viabackwards.protocol.v1_12to1_11_1.data;

public class BlockColors1_11_1 {
    private static final String[] COLORS = new String[16];

    static {
        COLORS[0] = "White";
        COLORS[1] = "Orange";
        COLORS[2] = "Magenta";
        COLORS[3] = "Light Blue";
        COLORS[4] = "Yellow";
        COLORS[5] = "Lime";
        COLORS[6] = "Pink";
        COLORS[7] = "Gray";
        COLORS[8] = "Light Gray";
        COLORS[9] = "Cyan";
        COLORS[10] = "Purple";
        COLORS[11] = "Blue";
        COLORS[12] = "Brown";
        COLORS[13] = "Green";
        COLORS[14] = "Red";
        COLORS[15] = "Black";
    }

    public static String get(int key) {
        return key >= 0 && key < COLORS.length ? COLORS[key] : "Unknown color";
    }
}
