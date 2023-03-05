/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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

package com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.data;

import com.viaversion.viaversion.api.data.BiMappings;
import com.viaversion.viaversion.api.data.Mappings;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.data.StatisticMappings;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BackwardsMappings extends com.viaversion.viabackwards.api.data.BackwardsMappings {
    private final Int2ObjectMap<String> statisticMappings = new Int2ObjectOpenHashMap<>();
    private final Map<String, String> translateMappings = new HashMap<>();

    public BackwardsMappings() {
        super("1.13", "1.12", Protocol1_13To1_12_2.class);
    }

    @Override
    public void loadExtras(final CompoundTag data) {
        final Mappings itemsToMapped = loadMappings(data, "items");
        final BiMappings itemsToUnmapped = Protocol1_13To1_12_2.MAPPINGS.getItemMappings();
        this.itemMappings = new BiMappings() {
            @Override
            public BiMappings inverse() {
                return itemsToUnmapped;
            }

            @Override
            public int getNewId(int id) {
                return itemsToMapped.getNewId(id);
            }

            @Override
            public void setNewId(int id, int mappedId) {
                itemsToMapped.setNewId(id, mappedId);
            }

            @Override
            public int size() {
                return itemsToMapped.size();
            }

            @Override
            public int mappedSize() {
                return itemsToMapped.mappedSize();
            }

            @Override
            public Mappings createInverse() {
                return itemsToMapped.createInverse();
            }
        };

        super.loadExtras(data);

        for (Map.Entry<String, Integer> entry : StatisticMappings.CUSTOM_STATS.entrySet()) {
            statisticMappings.put(entry.getValue().intValue(), entry.getKey());
        }
        for (Map.Entry<String, String> entry : Protocol1_13To1_12_2.MAPPINGS.getTranslateMapping().entrySet()) {
            translateMappings.put(entry.getValue(), entry.getKey());
        }
    }

    @Override
    protected @Nullable BiMappings loadBiMappings(final CompoundTag data, final String key) {
        // Special cursed case
        if (key.equals("items")) {
            return null;
        } else {
            return super.loadBiMappings(data, key);
        }
    }

    @Override
    public BiMappings getItemMappings() {
        return itemMappings;
    }

    @Override
    public int getNewItemId(final int id) {
        return itemMappings.getNewId(id);
    }

    @Override
    public int getOldItemId(final int id) {
        return itemMappings.inverse().getNewId(id);
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
        switch (mappedId) {
            case 1595:
            case 1596:
            case 1597:
                return 1584; // brown mushroom block
            case 1611:
            case 1612:
            case 1613:
                return 1600; // red mushroom block
            default:
                return mappedId;
        }
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

    @Override
    protected boolean shouldLoad(final String key) {
        return !key.equals("blocks");
    }
}
