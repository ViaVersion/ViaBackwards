package nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.data;

import nl.matsv.viabackwards.ViaBackwards;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.data.MappingData;
import us.myles.ViaVersion.util.GsonUtil;
import us.myles.viaversion.libs.gson.JsonElement;
import us.myles.viaversion.libs.gson.JsonObject;
import us.myles.viaversion.libs.gson.JsonPrimitive;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;

public class BackwardsMappings {
    public static BlockMappings blockMappings;

    public static void init() {
        JsonObject mapping1_13_2 = MappingData.loadData("mapping-1.13.2.json");
        JsonObject mapping1_14 = MappingData.loadData("mapping-1.14.json");
        JsonObject mapping1_13_2to1_14 = loadData("mapping-1.13.2to1.14.json");

        ViaBackwards.getPlatform().getLogger().info("Loading block mapping...");
        blockMappings = new BlockMappingsShortArray(mapping1_14.getAsJsonObject("blockstates"), mapping1_13_2.getAsJsonObject("blockstates"), mapping1_13_2to1_14.getAsJsonObject("blockstates"));
    }


    private static void mapIdentifiers(short[] output, JsonObject newIdentifiers, JsonObject oldIdentifiers, JsonObject mapping) {
        for (Map.Entry<String, JsonElement> entry : newIdentifiers.entrySet()) {
            String key = entry.getValue().getAsString();
            Map.Entry<String, JsonElement> value = findValue(oldIdentifiers, key);
            if (value == null) {
                JsonPrimitive replacement = mapping.getAsJsonPrimitive(key);
                if (replacement == null && key.contains("[")) {
                    replacement = mapping.getAsJsonPrimitive(key.substring(0, key.indexOf('[')));
                }
                if (replacement != null) {
                    if (replacement.getAsString().startsWith("id:")) {
                        String id = replacement.getAsString().replace("id:", "");
                        value = findValue(oldIdentifiers, oldIdentifiers.getAsJsonPrimitive(id).getAsString());
                    } else {
                        value = findValue(oldIdentifiers, replacement.getAsString());
                    }
                }
                if (value == null) {
                    if (!Via.getConfig().isSuppress1_13ConversionErrors() || Via.getManager().isDebug()) {
                        if (replacement != null) {
                            ViaBackwards.getPlatform().getLogger().warning("No key for " + entry.getValue() + "/" + replacement.getAsString() + " :( ");
                        } else {
                            ViaBackwards.getPlatform().getLogger().warning("No key for " + entry.getValue() + " :( ");
                        }
                    }
                    continue;
                }
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

    public static JsonObject loadData(String name) {
        try (InputStreamReader reader = new InputStreamReader(BackwardsMappings.class.getClassLoader().getResourceAsStream("assets/viabackwards/data/" + name))) {
            return GsonUtil.getGson().fromJson(reader, JsonObject.class);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public interface BlockMappings {
        int getNewBlock(int old);
    }

    private static class BlockMappingsShortArray implements BlockMappings {
        private short[] oldToNew = new short[11258 + 1];

        private BlockMappingsShortArray(JsonObject newIdentifiers, JsonObject oldIdentifiers, JsonObject mapping) {
            Arrays.fill(oldToNew, (short) -1);
            mapIdentifiers(oldToNew, newIdentifiers, oldIdentifiers, mapping);
        }

        @Override
        public int getNewBlock(int old) {
            return old >= 0 && old < oldToNew.length ? oldToNew[old] : -1;
        }
    }
}
