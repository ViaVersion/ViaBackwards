/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2022 ViaVersion and contributors
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
package com.viaversion.viabackwards.api.data;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.MappingDataLoader;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntMap;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonIOException;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import com.viaversion.viaversion.libs.gson.JsonSyntaxException;
import com.viaversion.viaversion.util.GsonUtil;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public final class VBMappingDataLoader {

    public static JsonObject loadFromDataDir(String name) {
        File file = new File(ViaBackwards.getPlatform().getDataFolder(), name);
        if (!file.exists()) return loadData(name);

        // Load the file from the platform's directory if present
        try (FileReader reader = new FileReader(file)) {
            return GsonUtil.getGson().fromJson(reader, JsonObject.class);
        } catch (JsonSyntaxException e) {
            ViaBackwards.getPlatform().getLogger().warning(name + " is badly formatted!");
            e.printStackTrace();
            ViaBackwards.getPlatform().getLogger().warning("Falling back to resource's file!");
            return loadData(name);
        } catch (IOException | JsonIOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static @Nullable InputStream getResource(String name) {
        return VBMappingDataLoader.class.getClassLoader().getResourceAsStream("assets/viabackwards/data/" + name);
    }

    public static JsonObject loadData(String name) {
        try (InputStream stream = getResource(name)) {
            if (stream == null) return null;
            return GsonUtil.getGson().fromJson(new InputStreamReader(stream), JsonObject.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void mapIdentifiers(int[] output, JsonObject oldIdentifiers, JsonObject newIdentifiers, JsonObject diffIdentifiers, boolean warnOnMissing) {
        Object2IntMap<String> newIdentifierMap = MappingDataLoader.indexedObjectToMap(newIdentifiers);
        for (Map.Entry<String, JsonElement> entry : oldIdentifiers.entrySet()) {
            String key = entry.getValue().getAsString();
            int mappedId = newIdentifierMap.getInt(key);
            if (mappedId == -1) {
                if (diffIdentifiers != null) {
                    // Search in diff mappings
                    JsonPrimitive diffValueJson = diffIdentifiers.getAsJsonPrimitive(key);
                    String diffValue = diffValueJson != null ? diffValueJson.getAsString() : null;

                    int dataIndex;
                    if (diffValue == null && (dataIndex = key.indexOf('[')) != -1
                            && (diffValueJson = diffIdentifiers.getAsJsonPrimitive(key.substring(0, dataIndex))) != null) {
                        // Check for wildcard mappings
                        diffValue = diffValueJson.getAsString();

                        // Keep original properties if value ends with [
                        if (diffValue.endsWith("[")) {
                            diffValue += key.substring(dataIndex + 1);
                        }
                    }

                    if (diffValue != null) {
                        mappedId = newIdentifierMap.getInt(diffValue);
                    }
                }

                if (mappedId == -1) {
                    // Nothing found :(
                    if (warnOnMissing && !Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                        ViaBackwards.getPlatform().getLogger().warning("No key for " + entry.getValue() + " :( ");
                    }
                    continue;
                }
            }

            output[Integer.parseInt(entry.getKey())] = mappedId;
        }
    }

    public static Map<String, String> objectToNamespacedMap(JsonObject object) {
        Map<String, String> mappings = new HashMap<>(object.size());
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            if (key.indexOf(':') == -1) {
                key = "minecraft:" + key;
            }
            String value = entry.getValue().getAsString();
            if (value.indexOf(':') == -1) {
                value = "minecraft:" + value;
            }
            mappings.put(key, value);
        }
        return mappings;
    }

    public static Map<String, String> objectToMap(JsonObject object) {
        Map<String, String> mappings = new HashMap<>(object.size());
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            mappings.put(entry.getKey(), entry.getValue().getAsString());
        }
        return mappings;
    }

    public static Int2ObjectMap<MappedItem> loadItemMappings(JsonObject oldMapping, JsonObject newMapping, JsonObject diffMapping, boolean warnOnMissing) {
        Int2ObjectMap<MappedItem> itemMapping = new Int2ObjectOpenHashMap<>(diffMapping.size(), 0.99F);
        Object2IntMap<String> newIdentifierMap = MappingDataLoader.indexedObjectToMap(newMapping);
        Object2IntMap<String> oldIdentifierMap = MappingDataLoader.indexedObjectToMap(oldMapping);
        for (Map.Entry<String, JsonElement> entry : diffMapping.entrySet()) {
            JsonObject object = entry.getValue().getAsJsonObject();
            String mappedIdName = object.getAsJsonPrimitive("id").getAsString();
            int mappedId = newIdentifierMap.getInt(mappedIdName);
            if (mappedId == -1) {
                if (!Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                    ViaBackwards.getPlatform().getLogger().warning("No key for " + mappedIdName + " :( ");
                }
                continue;
            }

            int oldId = oldIdentifierMap.getInt(entry.getKey());
            if (oldId == -1) {
                if (!Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                    ViaBackwards.getPlatform().getLogger().warning("No old entry for " + mappedIdName + " :( ");
                }
                continue;
            }

            String name = object.getAsJsonPrimitive("name").getAsString();
            itemMapping.put(oldId, new MappedItem(mappedId, name));
        }

        // Look for missing keys
        if (warnOnMissing && !Via.getConfig().isSuppressConversionWarnings()) {
            for (Object2IntMap.Entry<String> entry : oldIdentifierMap.object2IntEntrySet()) {
                if (!newIdentifierMap.containsKey(entry.getKey()) && !itemMapping.containsKey(entry.getIntValue())) {
                    ViaBackwards.getPlatform().getLogger().warning("No item mapping for " + entry.getKey() + " :( ");
                }
            }
        }

        return itemMapping;
    }
}
