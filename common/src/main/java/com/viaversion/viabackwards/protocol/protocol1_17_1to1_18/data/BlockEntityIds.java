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
package com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.data;

import java.util.Arrays;

public final class BlockEntityIds {

    private static final int[] IDS;

    static {
        final int[] ids = com.viaversion.viaversion.protocols.protocol1_18to1_17_1.BlockEntityIds.getIds();
        IDS = new int[Arrays.stream(ids).max().getAsInt() + 1];
        Arrays.fill(IDS, -1);
        for (int i = 0; i < ids.length; i++) {
            final int id = ids[i];
            if (id != -1) {
                IDS[id] = i;
            }
        }
    }

    public static int mappedId(final int id) {
        if (id < 0 || id > IDS.length) {
            return -1;
        }
        return IDS[id];
    }
}
