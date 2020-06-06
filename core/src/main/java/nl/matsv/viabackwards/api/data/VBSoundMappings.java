package nl.matsv.viabackwards.api.data;

import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.data.Mappings;
import us.myles.viaversion.libs.gson.JsonArray;
import us.myles.viaversion.libs.gson.JsonElement;
import us.myles.viaversion.libs.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Backwards mappings for sound ids and names.
 */
public class VBSoundMappings {

    private final Map<String, String> namedSoundMappings = new HashMap<>();
    private final Mappings idMappings;

    public VBSoundMappings(JsonArray oldSounds, JsonArray newSounds, JsonObject diffMapping) {
        idMappings = new VBMappings(oldSounds, newSounds, diffMapping);
        for (Map.Entry<String, JsonElement> entry : diffMapping.entrySet()) {
            String key = entry.getKey();
            if (key.indexOf(':') == -1) {
                key = "minecraft:" + key;
            }
            String value = entry.getValue().getAsString();
            if (value.indexOf(':') == -1) {
                value = "minecraft:" + value;
            }
            namedSoundMappings.put(key, value);
        }
    }

    @Nullable
    public String getNewId(String oldId) {
        return namedSoundMappings.get(oldId);
    }

    public int getNewId(int oldId) {
        return idMappings.getNewId(oldId);
    }
}
