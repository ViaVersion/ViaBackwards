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

import us.myles.ViaVersion.api.Via;
import us.myles.viaversion.libs.gson.JsonElement;
import us.myles.viaversion.libs.gson.JsonObject;

import java.util.Arrays;
import java.util.Map;

import static us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.MappingData.loadData;

public class BackwardsMappings {
    public static BlockMappings blockMappings;

    public static void init() {
        us.myles.viaversion.libs.gson.JsonObject mapping1_12 = loadData("mapping-1.12.json");
        us.myles.viaversion.libs.gson.JsonObject mapping1_13 = loadData("mapping-1.13.json");

        Via.getPlatform().getLogger().info("Loading block mapping...");
        blockMappings = new BlockMappingsShortArray(mapping1_13.getAsJsonObject("blocks"), mapping1_12.getAsJsonObject("blocks"));
    }


    private static void mapIdentifiers(short[] output, JsonObject oldIdentifiers, JsonObject newIdentifiers) {
        for (Map.Entry<String, JsonElement> entry : oldIdentifiers.entrySet()) {
            Map.Entry<String, JsonElement> value = findValue(newIdentifiers, entry.getValue().getAsString());
            if (value == null) {
                if (!Via.getConfig().isSuppress1_13ConversionErrors() || Via.getManager().isDebug()) {
                    Via.getPlatform().getLogger().warning("No key for " + entry.getValue() + " :( ");
                }
                continue;
            }
            output[Integer.parseInt(entry.getKey())] = Short.parseShort(value.getKey());
        }
    }


    private static Map.Entry<String, JsonElement> findValue(JsonObject object, String needle) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String value = entry.getValue().getAsString();
            if (value.equals(needle)) {
                return entry;
            }
        }
        return null;
    }

    public interface BlockMappings {
        int getNewBlock(int old);
    }

    private static class BlockMappingsShortArray implements BlockMappings {
        private short[] oldToNew = new short[4084 * 6];

        private BlockMappingsShortArray(JsonObject oldMapping, JsonObject newMapping) {
            Arrays.fill(oldToNew, (short) -1);
            mapIdentifiers(oldToNew, oldMapping, newMapping);
        }

        @Override
        public int getNewBlock(int old) {
            return old >= 0 && old < oldToNew.length ? oldToNew[old] : -1;
        }
    }
}
