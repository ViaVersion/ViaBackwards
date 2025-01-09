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

package com.viaversion.viabackwards.protocol.v1_13to1_12_2.data;

import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.Protocol1_12_2To1_13;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.data.StatisticMappings1_13;
import java.util.HashMap;
import java.util.Map;

public class BackwardsMappingData1_13 extends BackwardsMappingData {
    private final Int2ObjectMap<String> statisticMappings = new Int2ObjectOpenHashMap<>();
    private final Map<String, String> translateMappings = new HashMap<>();

    public BackwardsMappingData1_13() {
        super("1.13", "1.12", Protocol1_12_2To1_13.class);
    }

    @Override
    public void loadExtras(final CompoundTag data) {
        super.loadExtras(data);

        for (Map.Entry<String, Integer> entry : StatisticMappings1_13.CUSTOM_STATS.entrySet()) {
            statisticMappings.put(entry.getValue().intValue(), entry.getKey());
        }
        for (Map.Entry<String, String> entry : Protocol1_12_2To1_13.MAPPINGS.getTranslateMapping().entrySet()) {
            translateMappings.put(entry.getValue(), entry.getKey());
        }
    }

    @Override
    public int getNewBlockStateId(int id) {
        // Comparator funkyness: https://github.com/ViaVersion/ViaBackwards/issues/524
        if (id >= 5635 && id <= 5650) {
            if (id < 5639) {
                id += 4;
            } else if (id < 5643) {
                id -= 4;
            } else if (id < 5647) {
                id += 4;
            } else {
                id -= 4;
            }
        }

        int mappedId = super.getNewBlockStateId(id);

        // https://github.com/ViaVersion/ViaBackwards/issues/290
        return switch (mappedId) {
            case 1595, 1596, 1597 -> 1584; // brown mushroom block
            case 1611, 1612, 1613 -> 1600; // red mushroom block
            default -> mappedId;
        };
    }

    @Override
    protected int checkValidity(int id, int mappedId, String type) {
        // Don't warn for missing ids here
        return mappedId;
    }

    public Int2ObjectMap<String> getStatisticMappings() {
        return statisticMappings;
    }

    public Map<String, String> getTranslateMappings() {
        return translateMappings;
    }
}
