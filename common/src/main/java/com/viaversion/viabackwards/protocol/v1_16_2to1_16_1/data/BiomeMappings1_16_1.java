/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2025 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_16_2to1_16_1.data;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.data.BackwardsMappingDataLoader;
import com.viaversion.viabackwards.protocol.v1_16_2to1_16_1.Protocol1_16_2To1_16_1;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntOpenHashMap;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.util.Key;
import java.util.Map;

public final class BiomeMappings1_16_1 {

    private static final Object2IntMap<String> MODERN_TO_LEGACY_ID = new Object2IntOpenHashMap<>();
    private static final Object2IntMap<String> LEGACY_BIOMES = new Object2IntOpenHashMap<>();

    static {
        LEGACY_BIOMES.defaultReturnValue(-1);
        MODERN_TO_LEGACY_ID.defaultReturnValue(-1);

        add(0, "ocean");
        add(1, "plains");
        add(2, "desert");
        add(3, "mountains");
        add(4, "forest");
        add(5, "taiga");
        add(6, "swamp");
        add(7, "river");
        add(8, "nether");
        add(9, "the_end");
        add(10, "frozen_ocean");
        add(11, "frozen_river");
        add(12, "snowy_tundra");
        add(13, "snowy_mountains");
        add(14, "mushroom_fields");
        add(15, "mushroom_field_shore");
        add(16, "beach");
        add(17, "desert_hills");
        add(18, "wooded_hills");
        add(19, "taiga_hills");
        add(20, "mountain_edge");
        add(21, "jungle");
        add(22, "jungle_hills");
        add(23, "jungle_edge");
        add(24, "deep_ocean");
        add(25, "stone_shore");
        add(26, "snowy_beach");
        add(27, "birch_forest");
        add(28, "birch_forest_hills");
        add(29, "dark_forest");
        add(30, "snowy_taiga");
        add(31, "snowy_taiga_hills");
        add(32, "giant_tree_taiga");
        add(33, "giant_tree_taiga_hills");
        add(34, "wooded_mountains");
        add(35, "savanna");
        add(36, "savanna_plateau");
        add(37, "badlands");
        add(38, "wooded_badlands_plateau");
        add(39, "badlands_plateau");
        add(40, "small_end_islands");
        add(41, "end_midlands");
        add(42, "end_highlands");
        add(43, "end_barrens");
        add(44, "warm_ocean");
        add(45, "lukewarm_ocean");
        add(46, "cold_ocean");
        add(47, "deep_warm_ocean");
        add(48, "deep_lukewarm_ocean");
        add(49, "deep_cold_ocean");
        add(50, "deep_frozen_ocean");
        add(127, "the_void");
        add(129, "sunflower_plains");
        add(130, "desert_lakes");
        add(131, "gravelly_mountains");
        add(132, "flower_forest");
        add(133, "taiga_mountains");
        add(134, "swamp_hills");
        add(140, "ice_spikes");
        add(149, "modified_jungle");
        add(151, "modified_jungle_edge");
        add(155, "tall_birch_forest");
        add(156, "tall_birch_hills");
        add(157, "dark_forest_hills");
        add(158, "snowy_taiga_mountains");
        add(160, "giant_spruce_taiga");
        add(161, "giant_spruce_taiga_hills");
        add(162, "modified_gravelly_mountains");
        add(163, "shattered_savanna");
        add(164, "shattered_savanna_plateau");
        add(165, "eroded_badlands");
        add(166, "modified_wooded_badlands_plateau");
        add(167, "modified_badlands_plateau");
        add(168, "bamboo_jungle");
        add(169, "bamboo_jungle_hills");

        // Include the legacy biomes themselves
        for (final Object2IntMap.Entry<String> entry : LEGACY_BIOMES.object2IntEntrySet()) {
            MODERN_TO_LEGACY_ID.put(entry.getKey(), entry.getIntValue());
        }

        final JsonObject mappings = BackwardsMappingDataLoader.INSTANCE.loadFromDataDir("biome-mappings.json");
        for (final Map.Entry<String, JsonElement> entry : mappings.entrySet()) {
            final int legacyBiome = LEGACY_BIOMES.getInt(entry.getValue().getAsString());
            if (legacyBiome == -1) {
                Protocol1_16_2To1_16_1.LOGGER.warning("Unknown legacy biome: " + entry.getValue().getAsString());
                continue;
            }

            MODERN_TO_LEGACY_ID.put(entry.getKey(), legacyBiome);
        }
    }

    private static void add(final int id, final String biome) {
        LEGACY_BIOMES.put(biome, id);
    }

    public static int toLegacyBiome(String biome) {
        final int legacyBiome = MODERN_TO_LEGACY_ID.getInt(Key.stripMinecraftNamespace(biome));
        if (legacyBiome == -1) {
            if (!Via.getConfig().isSuppressConversionWarnings()) {
                Protocol1_16_2To1_16_1.LOGGER.warning("Biome with id " + biome + " has no legacy biome mapping (custom datapack?)");
            }
            return 1; // Plains
        }
        return legacyBiome;
    }
}
