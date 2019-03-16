package nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.data;

import us.myles.viaversion.libs.gson.JsonArray;
import us.myles.viaversion.libs.gson.JsonElement;
import us.myles.viaversion.libs.gson.JsonObject;

import java.util.Arrays;

import static us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.MappingData.loadData;

public class SoundMapping {
    private static short[] sounds = new short[789];

    public static void init() {
        JsonObject mapping1_13_2 = loadData("mapping-1.13.2.json");
        JsonObject mapping1_14 = loadData("mapping-1.14.json");

        Arrays.fill(sounds, (short) -1);
        mapIdentifiers(sounds, mapping1_14.getAsJsonArray("sounds"), mapping1_13_2.getAsJsonArray("sounds"));
    }

    private static void mapIdentifiers(short[] output, JsonArray oldIdentifiers, JsonArray newIdentifiers) {
        for (int i = 0; i < oldIdentifiers.size(); i++) {
            JsonElement v = oldIdentifiers.get(i);
            Integer index = findIndex(newIdentifiers, v.getAsString());
            if (index == null) continue;  //There will be missing sounds, since we are goind backwards
            output[i] = index.shortValue();
        }
    }

    private static Integer findIndex(JsonArray array, String value) {
        for (int i = 0; i < array.size(); i++) {
            JsonElement v = array.get(i);
            if (v.getAsString().equals(value)) {
                return i;
            }
        }
        return null;
    }

    public static int getOldSound(int newSound) {
        return newSound >= sounds.length ? -1 : sounds[newSound];
    }
}
