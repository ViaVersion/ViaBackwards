package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.data;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.data.VBItemMappings;
import nl.matsv.viabackwards.api.data.VBMappingDataLoader;
import nl.matsv.viabackwards.api.data.VBMappings;
import nl.matsv.viabackwards.api.data.VBSoundMappings;
import us.myles.ViaVersion.api.data.MappingDataLoader;
import us.myles.ViaVersion.api.data.Mappings;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.data.MappingData;
import us.myles.viaversion.libs.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class BackwardsMappings {
    public static Mappings blockStateMappings;
    public static Mappings blockMappings;
    public static Mappings statisticsMappings;
    public static VBSoundMappings soundMappings;
    public static VBItemMappings itemMappings;
    public static Map<String, String> attributeMappings = new HashMap<>();

    public static void init() {
        ViaBackwards.getPlatform().getLogger().info("Loading 1.16 -> 1.15.2 mappings...");
        JsonObject mapping1_15 = MappingDataLoader.getMappingsCache().get("mapping-1.15.json");
        JsonObject mapping1_16 = MappingDataLoader.getMappingsCache().get("mapping-1.16.json");
        JsonObject mapping1_15to1_16 = VBMappingDataLoader.loadFromDataDir("mapping-1.15to1.16.json");

        blockStateMappings = new VBMappings(mapping1_16.getAsJsonObject("blockstates"), mapping1_15.getAsJsonObject("blockstates"), mapping1_15to1_16.getAsJsonObject("blockstates"));
        blockMappings = new VBMappings(mapping1_16.getAsJsonObject("blocks"), mapping1_15.getAsJsonObject("blocks"), mapping1_15to1_16.getAsJsonObject("blocks"), false);
        itemMappings = new VBItemMappings(mapping1_16.getAsJsonObject("items"), mapping1_15.getAsJsonObject("items"), mapping1_15to1_16.getAsJsonObject("items"));
        soundMappings = new VBSoundMappings(mapping1_16.getAsJsonArray("sounds"), mapping1_15.getAsJsonArray("sounds"), mapping1_15to1_16.getAsJsonObject("sounds"));
        statisticsMappings = new Mappings(mapping1_16.getAsJsonArray("statistics"), mapping1_15.getAsJsonArray("statistics"), false);

        for (Map.Entry<String, String> entry : MappingData.attributeMappings.entrySet()) {
            attributeMappings.put(entry.getValue(), entry.getKey());
        }
    }
}