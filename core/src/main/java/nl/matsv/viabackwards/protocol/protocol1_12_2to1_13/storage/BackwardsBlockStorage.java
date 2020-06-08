/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.storage;

import us.myles.ViaVersion.api.data.StoredObject;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.minecraft.Position;
import us.myles.ViaVersion.util.fastutil.CollectionUtil;
import us.myles.ViaVersion.util.fastutil.IntSet;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BackwardsBlockStorage extends StoredObject {
    // This BlockStorage is very exclusive (;
    private static final IntSet WHITELIST = CollectionUtil.createIntSet(779);

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

    public BackwardsBlockStorage(UserConnection user) {
        super(user);
    }

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
