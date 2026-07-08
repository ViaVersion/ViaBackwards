/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
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

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viaversion.api.data.MappingDataLoader;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntOpenHashMap;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.Protocol1_12_2To1_13;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.blockconnections.ConnectionData;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.data.StatisticMappings1_13;
import java.util.HashMap;
import java.util.Map;

public class BackwardsMappingData1_13 extends BackwardsMappingData {
    private final Int2ObjectMap<String> statisticMappings = new Int2ObjectOpenHashMap<>();
    private final Map<String, String> translateMappings = new HashMap<>();
    private final Object2IntMap<String> pistonIds = new Object2IntOpenHashMap<>();

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

        pistonIds.defaultReturnValue(-1);
        Object2IntMap<String> keyToId = ConnectionData.getKeyToId();
        if (!keyToId.isEmpty()) {
            for (final Object2IntMap.Entry<String> entry : keyToId.object2IntEntrySet()) {
                if (!entry.getKey().contains("piston")) {
                    continue;
                }

                addPistonEntries(entry.getKey(), entry.getIntValue());
            }
        } else { // not loaded by VV
            ListTag<StringTag> blockStates = MappingDataLoader.INSTANCE.loadNBT("blockstates-1.13.nbt").getListTag("blockstates", StringTag.class);
            for (int id = 0; id < blockStates.size(); id++) {
                StringTag state = blockStates.get(id);
                String key = state.getValue();
                if (!key.contains("piston")) {
                    continue;
                }

                addPistonEntries(key, id);
            }
        }
    }

    private void addPistonEntries(String data, int id) {
        id = getNewBlockStateId(id);
        pistonIds.put(data, id);

        String substring = data.substring(10);
        if (!substring.startsWith("piston") && !substring.startsWith("sticky_piston")) return;

        // Swap properties and add them to the map
        String[] split = data.substring(0, data.length() - 1).split("\\[");
        String[] properties = split[1].split(",");
        data = split[0] + "[" + properties[1] + "," + properties[0] + "]";
        pistonIds.put(data, id);
    }

    @Override
    public int getNewBlockStateId(int id) {
        // Always map to unpowered_comparator; powered_comparator is unused in 1.12 https://github.com/ViaVersion/ViaBackwards/issues/1222
        if (id >= 5635 && id <= 5650) {
            int mappedId = super.getNewBlockStateId(id);
            if (mappedId >= 2400) {
                mappedId -= 16;
            }
            return mappedId;
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

    public Object2IntMap<String> getPistonIds() {
        return pistonIds;
    }
}
