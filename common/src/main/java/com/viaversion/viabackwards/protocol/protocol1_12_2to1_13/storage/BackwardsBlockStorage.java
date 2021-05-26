/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
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

package com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.storage;

import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.libs.fastutil.ints.IntOpenHashSet;
import com.viaversion.viaversion.libs.fastutil.ints.IntSet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BackwardsBlockStorage implements StorableObject {
    // This BlockStorage is very exclusive (;
    private static final IntSet WHITELIST = new IntOpenHashSet(779);

    static {
        // Flower pots
        for (int i = 5265; i <= 5286; i++) {
            WHITELIST.add(i);
        }

        // Add those beds
        for (int i = 0; i < (16 * 16); i++) {
            WHITELIST.add(748 + i);
        }

        // Add the banners
        for (int i = 6854; i <= 7173; i++) {
            WHITELIST.add(i);
        }

        // Spawner
        WHITELIST.add(1647);

        // Skulls
        for (int i = 5447; i <= 5566; i++) {
            WHITELIST.add(i);
        }

        // pistons
        for (int i = 1028; i <= 1039; i++) {
            WHITELIST.add(i);
        }
        for (int i = 1047; i <= 1082; i++) {
            WHITELIST.add(i);
        }
        for (int i = 1099; i <= 1110; i++) {
            WHITELIST.add(i);
        }
    }

    private final Map<Position, Integer> blocks = new ConcurrentHashMap<>();

    public void checkAndStore(Position position, int block) {
        if (!WHITELIST.contains(block)) {
            // Remove if not whitelisted
            blocks.remove(position);
            return;
        }

        blocks.put(position, block);
    }

    public boolean isWelcome(int block) {
        return WHITELIST.contains(block);
    }

    public Integer get(Position position) {
        return blocks.get(position);
    }

    public int remove(Position position) {
        return blocks.remove(position);
    }

    public void clear() {
        blocks.clear();
    }

    public Map<Position, Integer> getBlocks() {
        return blocks;
    }
}
