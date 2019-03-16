/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers;

import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.providers.BackwardsBlockEntityProvider;
import us.myles.ViaVersion.api.Pair;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.IntTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlowerPotHandler implements BackwardsBlockEntityProvider.BackwardsBlockEntityHandler {

    private static final Map<Integer, Pair<String, Byte>> flowers = new ConcurrentHashMap<>();

    static {
        register(5265, "minecraft:air", (byte) 0);
        register(5266, "minecraft:sapling", (byte) 0);
        register(5267, "minecraft:sapling", (byte) 1);
        register(5268, "minecraft:sapling", (byte) 2);
        register(5269, "minecraft:sapling", (byte) 3);
        register(5270, "minecraft:sapling", (byte) 4);
        register(5271, "minecraft:sapling", (byte) 5);
        register(5272, "minecraft:tallgrass", (byte) 2);
        register(5273, "minecraft:yellow_flower", (byte) 0);
        register(5274, "minecraft:red_flower", (byte) 0);
        register(5275, "minecraft:red_flower", (byte) 1);
        register(5276, "minecraft:red_flower", (byte) 2);
        register(5277, "minecraft:red_flower", (byte) 3);
        register(5278, "minecraft:red_flower", (byte) 4);
        register(5279, "minecraft:red_flower", (byte) 5);
        register(5280, "minecraft:red_flower", (byte) 6);
        register(5281, "minecraft:red_flower", (byte) 7);
        register(5282, "minecraft:red_flower", (byte) 8);
        register(5283, "minecraft:red_mushroom", (byte) 0);
        register(5284, "minecraft:brown_mushroom", (byte) 0);
        register(5285, "minecraft:deadbush", (byte) 0);
        register(5286, "minecraft:cactus", (byte) 0);
    }

    private static void register(int id, String identifier, byte data) {
        flowers.put(id, new Pair<>(identifier, data));
    }

    public static boolean isFlowah(int id) {
        return flowers.containsKey(id);
    }

    public Pair<String, Byte> getOrDefault(int blockId) {
        if (flowers.containsKey(blockId))
            return flowers.get(blockId);

        return flowers.get(5265);
    }

    // TODO THIS IS NEVER CALLED BECAUSE ITS NO LONGER A BLOCK ENTITY :(
    @Override
    public CompoundTag transform(UserConnection user, int blockId, CompoundTag tag) {
        Pair<String, Byte> item = getOrDefault(blockId);

        tag.put(new StringTag("Item", item.getKey()));
        tag.put(new IntTag("Data", item.getValue()));

        return tag;
    }

}
