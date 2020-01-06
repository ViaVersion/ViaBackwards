/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.data.VBMappingDataLoader;
import nl.matsv.viabackwards.api.data.VBMappings;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.MappingDataLoader;
import us.myles.ViaVersion.api.data.Mappings;
import us.myles.viaversion.libs.gson.JsonElement;
import us.myles.viaversion.libs.gson.JsonObject;
import us.myles.viaversion.libs.gson.JsonPrimitive;

import java.util.Arrays;
import java.util.Map;

public class BackwardsMappings {
    public static BlockMappingsShortArray blockMappings;
    public static Mappings soundMappings;

    public static void init() {
        JsonObject mapping1_12 = MappingDataLoader.loadData("mapping-1.12.json");
        JsonObject mapping1_13 = MappingDataLoader.loadData("mapping-1.13.json");
        JsonObject mapping1_12_2to1_13 = VBMappingDataLoader.loadFromDataDir("mapping-1.12.2to1.13.json");

        ViaBackwards.getPlatform().getLogger().info("Loading 1.13 -> 1.12.2 block mapping...");
        blockMappings = new BlockMappingsShortArray(mapping1_13.getAsJsonObject("blocks"), mapping1_12.getAsJsonObject("blocks"), mapping1_12_2to1_13.getAsJsonObject("blockstates"));
        ViaBackwards.getPlatform().getLogger().info("Loading 1.13 -> 1.12.2 sound mapping...");
        soundMappings = new VBMappings(mapping1_13.getAsJsonArray("sounds"), mapping1_12.getAsJsonArray("sounds"), mapping1_12_2to1_13.getAsJsonObject("sounds"));
    }

    // Has lots of compat layers, so we can't use the default Via method
    private static void mapIdentifiers(short[] output, JsonObject newIdentifiers, JsonObject oldIdentifiers, JsonObject mapping) {
        for (Map.Entry<String, JsonElement> entry : newIdentifiers.entrySet()) {
            String key = entry.getValue().getAsString();
            Map.Entry<String, JsonElement> value = MappingDataLoader.findValue(oldIdentifiers, key);
            short hardId = -1;
            if (value == null) {
                JsonPrimitive replacement = mapping.getAsJsonPrimitive(key);
                if (replacement == null && key.contains("[")) {
                    replacement = mapping.getAsJsonPrimitive(key.substring(0, key.indexOf('[')));
                }
                if (replacement != null) {
                    if (replacement.getAsString().startsWith("id:")) {
                        String id = replacement.getAsString().replace("id:", "");
                        hardId = Short.parseShort(id);
                        value = MappingDataLoader.findValue(oldIdentifiers, oldIdentifiers.getAsJsonPrimitive(id).getAsString());
                    } else {
                        value = MappingDataLoader.findValue(oldIdentifiers, replacement.getAsString());
                    }
                }
                if (value == null) {
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
            output[Integer.parseInt(entry.getKey())] = hardId != -1 ? hardId : Short.parseShort(value.getKey());
        }
    }

    public static class BlockMappingsShortArray {
        private final short[] oldToNew = new short[8582];

        private BlockMappingsShortArray(JsonObject newIdentifiers, JsonObject oldIdentifiers, JsonObject mapping) {
            Arrays.fill(oldToNew, (short) -1);
            mapIdentifiers(oldToNew, newIdentifiers, oldIdentifiers, mapping);
        }

        public int getNewId(int old) {
            return old >= 0 && old < oldToNew.length ? oldToNew[old] : -1;
        }
    }
}
