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

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.data.VBMappingDataLoader;
import com.viaversion.viabackwards.api.data.VBMappings;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.IntArrayMappings;
import com.viaversion.viaversion.api.data.MappingDataLoader;
import com.viaversion.viaversion.api.data.Mappings;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntMap;
import com.viaversion.viaversion.libs.gson.JsonArray;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.data.StatisticMappings;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BackwardsMappings extends com.viaversion.viabackwards.api.data.BackwardsMappings {
    private final Int2ObjectMap<String> statisticMappings = new Int2ObjectOpenHashMap<>();
    private final Map<String, String> translateMappings = new HashMap<>();

    public BackwardsMappings() {
        super("1.13", "1.12", Protocol1_13To1_12_2.class, true);
    }

    @Override
    public void loadVBExtras(JsonObject unmappedIdentifiers, JsonObject mappedIdentifiers, JsonObject diffMappings) {
        JsonObject diffItems = diffMappings.getAsJsonObject("items");
        JsonArray unmappedItems = unmappedIdentifiers.getAsJsonArray("items");
        JsonObject mappedItems = mappedIdentifiers.getAsJsonObject("items");
        backwardsItemMappings = VBMappingDataLoader.loadItemMappings(MappingDataLoader.arrayToMap(unmappedItems), MappingDataLoader.indexedObjectToMap(mappedItems), diffItems, false);

        enchantmentMappings = VBMappings.vbBuilder().warnOnMissing(false)
                .unmapped(unmappedIdentifiers.getAsJsonArray("enchantments")).mapped(mappedIdentifiers.getAsJsonObject("legacy_enchantments")).build();
        for (Map.Entry<String, Integer> entry : StatisticMappings.CUSTOM_STATS.entrySet()) {
            statisticMappings.put(entry.getValue().intValue(), entry.getKey());
        }
        for (Map.Entry<String, String> entry : Protocol1_13To1_12_2.MAPPINGS.getTranslateMapping().entrySet()) {
            translateMappings.put(entry.getValue(), entry.getKey());
        }
    }

    // Has lots of compat layers, so we can't use the default Via method
    private static void mapIdentifiers(int[] output, JsonObject newIdentifiers, JsonObject oldIdentifiers, JsonObject mapping) {
        Object2IntMap<String> newIdentifierMap = MappingDataLoader.indexedObjectToMap(oldIdentifiers);
        for (Map.Entry<String, JsonElement> entry : newIdentifiers.entrySet()) {
            String key = entry.getValue().getAsString();
            int value = newIdentifierMap.getInt(key);
            short hardId = -1;
            if (value == -1) {
                JsonPrimitive replacement = mapping.getAsJsonPrimitive(key);
                int propertyIndex;
                if (replacement == null && (propertyIndex = key.indexOf('[')) != -1) {
                    replacement = mapping.getAsJsonPrimitive(key.substring(0, propertyIndex));
                }
                if (replacement != null) {
                    if (replacement.getAsString().startsWith("id:")) {
                        String id = replacement.getAsString().replace("id:", "");
                        hardId = Short.parseShort(id);
                        value = newIdentifierMap.getInt(oldIdentifiers.getAsJsonPrimitive(id).getAsString());
                    } else {
                        value = newIdentifierMap.getInt(replacement.getAsString());
                    }
                }
                if (value == -1) {
                    if (!Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                        if (replacement != null) {
                            ViaBackwards.getPlatform().getLogger().warning("No key for " + entry.getValue() + "/" + replacement.getAsString() + " :( ");
                        } else {
                            ViaBackwards.getPlatform().getLogger().warning("No key for " + entry.getValue() + " :( ");
                        }
                    }
                    continue;
                }
            }
            output[Integer.parseInt(entry.getKey())] = hardId != -1 ? hardId : (short) value;
        }
    }

    @Override
    protected @Nullable Mappings loadFromArray(JsonObject unmappedIdentifiers, JsonObject mappedIdentifiers, @Nullable JsonObject diffMappings, String key) {
        if (key.equals("blockstates")) {
            int[] oldToNew = new int[8582];
            Arrays.fill(oldToNew, -1);
            mapIdentifiers(oldToNew, toJsonObject(unmappedIdentifiers.getAsJsonArray("blockstates")), mappedIdentifiers.getAsJsonObject("blocks"), diffMappings.getAsJsonObject("blockstates"));
            return IntArrayMappings.of(oldToNew, -1);
        } else {
            return super.loadFromArray(unmappedIdentifiers, mappedIdentifiers, diffMappings, key);
        }
    }

    private JsonObject toJsonObject(final JsonArray array) {
        final JsonObject object = new JsonObject();
        for (int i = 0; i < array.size(); i++) {
            final JsonElement element = array.get(i);
            object.add(Integer.toString(i), element);
        }
        return object;
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

    @Override
    protected boolean shouldWarnOnMissing(String key) {
        return super.shouldWarnOnMissing(key) && !key.equals("items");
    }

    public Int2ObjectMap<String> getStatisticMappings() {
        return statisticMappings;
    }

    public Map<String, String> getTranslateMappings() {
        return translateMappings;
    }

    @Override
    protected boolean shouldLoad(final String key) {
        return super.shouldLoad(key) && !key.equals("blocks");
    }
}
