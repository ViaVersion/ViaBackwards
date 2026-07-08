/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation);either version 3 of the License);or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not);see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.viabackwards.protocol.v1_13to1_12_2.data;

import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;

// The mildly inconvenient alternative to the very inconvenient task of adding full 1.12 identifiers
public final class BackwardsItemMappings {

    private static final Int2ObjectMap<String> MODEL_DATA_TO_NAME = new Int2ObjectOpenHashMap<>();

    static {
        add("acacia_button", 245);
        add("birch_button", 243);
        add("spruce_button", 242);
        add("jungle_button", 244);
        add("dark_oak_button", 246);
        add("acacia_trapdoor", 191);
        add("birch_trapdoor", 189);
        add("spruce_trapdoor", 188);
        add("jungle_trapdoor", 190);
        add("dark_oak_trapdoor", 192);
        add("acacia_pressure_plate", 164);
        add("birch_pressure_plate", 162);
        add("spruce_pressure_plate", 161);
        add("jungle_pressure_plate", 163);
        add("dark_oak_pressure_plate", 165);
        add("acacia_boat", 762);
        add("birch_boat", 760);
        add("spruce_boat", 759);
        add("jungle_boat", 761);
        add("dark_oak_boat", 763);
        add("blue_ice", 453);
        add("pufferfish_bucket", 547);
        add("salmon_bucket", 548);
        add("cod_bucket", 549);
        add("tropical_fish_bucket", 550);
        add("heart_of_the_sea", 784);
        add("nautilus_shell", 783);
        add("phantom_membrane", 782);
        add("turtle_helmet", 465);
        add("turtle_egg", 427);
        add("scute", 466);
        add("trident", 781);
        add("sea_pickle", 80);
        add("seagrass", 79);
        add("conduit", 454);
        add("kelp", 554);
        add("dried_kelp", 611);
        add("dried_kelp_block", 555);
        add("stripped_oak_log", 38);
        add("stripped_acacia_log", 42);
        add("stripped_birch_log", 40);
        add("stripped_spruce_log", 39);
        add("stripped_jungle_log", 41);
        add("stripped_dark_oak_log", 43);
        add("stripped_oak_wood", 44);
        add("stripped_acacia_wood", 48);
        add("stripped_birch_wood", 46);
        add("stripped_spruce_wood", 45);
        add("stripped_jungle_wood", 47);
        add("stripped_dark_oak_wood", 49);
        add("oak_wood", 50);
        add("spruce_wood", 51);
        add("birch_wood", 52);
        add("jungle_wood", 53);
        add("acacia_wood", 54);
        add("dark_oak_wood", 55);
        add("prismarine_slab", 128);
        add("prismarine_brick_slab", 129);
        add("dark_prismarine_slab", 130);
        add("prismarine_stairs", 346);
        add("prismarine_brick_stairs", 347);
        add("dark_prismarine_stairs", 348);
        add("drowned_spawn_egg", 643);
        add("phantom_spawn_egg", 658);
        add("dolphin_spawn_egg", 641);
        add("turtle_spawn_egg", 674);
        add("cod_spawn_egg", 638);
        add("salmon_spawn_egg", 663);
        add("pufferfish_spawn_egg", 661);
        add("tropical_fish_spawn_egg", 673);
        add("tube_coral", 438);
        add("brain_coral", 439);
        add("bubble_coral", 440);
        add("fire_coral", 441);
        add("horn_coral", 442);
        add("tube_coral_fan", 443);
        add("brain_coral_fan", 444);
        add("bubble_coral_fan", 445);
        add("fire_coral_fan", 446);
        add("horn_coral_fan", 447);
        add("dead_tube_coral_fan", 448);
        add("dead_brain_coral_fan", 449);
        add("dead_bubble_coral_fan", 450);
        add("dead_fire_coral_fan", 451);
        add("dead_horn_coral_fan", 452);
        add("dead_tube_coral_block", 428);
        add("dead_brain_coral_block", 429);
        add("dead_bubble_coral_block", 430);
        add("dead_fire_coral_block", 431);
        add("dead_horn_coral_block", 432);
        add("tube_coral_block", 433);
        add("brain_coral_block", 434);
        add("bubble_coral_block", 435);
        add("fire_coral_block", 436);
        add("horn_coral_block", 437);
        add("smooth_quartz", 131);
        add("smooth_red_sandstone", 132);
        add("smooth_sandstone", 133);
        add("smooth_stone", 134);
        add("pumpkin", 181);
        add("mushroom_stem", 205);
        add("debug_stick", 768);
    }

    public static String identifier(final int id) {
        return MODEL_DATA_TO_NAME.get(id);
    }

    private static void add(final String key, final int id) {
        MODEL_DATA_TO_NAME.put(id, key);
    }
}
