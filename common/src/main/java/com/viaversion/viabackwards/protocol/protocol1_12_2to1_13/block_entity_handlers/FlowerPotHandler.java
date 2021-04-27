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

package com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers;

import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.providers.BackwardsBlockEntityProvider;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.util.Pair;

public class FlowerPotHandler implements BackwardsBlockEntityProvider.BackwardsBlockEntityHandler {

    private static final Int2ObjectMap<Pair<String, Byte>> FLOWERS = new Int2ObjectOpenHashMap<>(22, 1F);
    private static final Pair<String, Byte> AIR = new Pair<>("minecraft:air", (byte) 0);

    static {
        FLOWERS.put(5265, AIR);
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
        FLOWERS.put(id, new Pair<>(identifier, data));
    }

    public static boolean isFlowah(int id) {
        return id >= 5265 && id <= 5286;
    }

    public Pair<String, Byte> getOrDefault(int blockId) {
        Pair<String, Byte> pair = FLOWERS.get(blockId);
        return pair != null ? pair : AIR;
    }

    // TODO THIS IS NEVER CALLED BECAUSE ITS NO LONGER A BLOCK ENTITY :(
    @Override
    public CompoundTag transform(UserConnection user, int blockId, CompoundTag tag) {
        Pair<String, Byte> item = getOrDefault(blockId);

        tag.put("Item", new StringTag(item.getKey()));
        tag.put("Data", new IntTag(item.getValue()));

        return tag;
    }

}
