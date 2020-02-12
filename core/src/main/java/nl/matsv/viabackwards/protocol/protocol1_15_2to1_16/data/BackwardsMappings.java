package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.data;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.data.VBItemMappings;
import nl.matsv.viabackwards.api.data.VBMappingDataLoader;
import nl.matsv.viabackwards.api.data.VBMappings;
import us.myles.ViaVersion.api.data.MappingDataLoader;
import us.myles.ViaVersion.api.data.Mappings;
import us.myles.viaversion.libs.gson.JsonObject;

public class BackwardsMappings {
    public static Mappings blockStateMappings;
    public static Mappings blockMappings;
    public static Mappings soundMappings;
    public static VBItemMappings itemMappings;

    public static void init() {
        JsonObject mapping1_15 = MappingDataLoader.loadData("mapping-1.15.json");
        JsonObject mapping1_16 = MappingDataLoader.loadData("mapping-1.16.json");
        JsonObject mapping1_15to1_16 = VBMappingDataLoader.loadFromDataDir("mapping-1.15to1.16.json");

        ViaBackwards.getPlatform().getLogger().info("Loading 1.16 -> 1.15.2 mappings...");
        blockStateMappings = new VBMappings(mapping1_16.getAsJsonObject("blockstates"), mapping1_15.getAsJsonObject("blockstates"), mapping1_15to1_16.getAsJsonObject("blockstates"));
        blockMappings = new VBMappings(mapping1_16.getAsJsonObject("blocks"), mapping1_15.getAsJsonObject("blocks"), mapping1_15to1_16.getAsJsonObject("blocks"), false);
        itemMappings = new VBItemMappings(mapping1_16.getAsJsonObject("items"), mapping1_15.getAsJsonObject("items"), mapping1_15to1_16.getAsJsonObject("items"));
        soundMappings = new VBMappings(mapping1_16.getAsJsonArray("sounds"), mapping1_15.getAsJsonArray("sounds"), mapping1_15to1_16.getAsJsonObject("sounds"));
    }
}