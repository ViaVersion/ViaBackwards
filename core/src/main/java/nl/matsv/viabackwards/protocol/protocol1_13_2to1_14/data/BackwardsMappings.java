package nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.data;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.data.VBMappingDataLoader;
import nl.matsv.viabackwards.api.data.VBMappings;
import us.myles.ViaVersion.api.data.MappingDataLoader;
import us.myles.ViaVersion.api.data.Mappings;
import us.myles.viaversion.libs.gson.JsonObject;

public class BackwardsMappings {
    public static Mappings blockStateMappings;
    public static Mappings blockMappings;
    public static Mappings soundMappings;

    public static void init() {
        JsonObject mapping1_13_2 = MappingDataLoader.loadData("mapping-1.13.2.json");
        JsonObject mapping1_14 = MappingDataLoader.loadData("mapping-1.14.json");
        JsonObject mapping1_13_2to1_14 = VBMappingDataLoader.loadData("mapping-1.13.2to1.14.json");

        ViaBackwards.getPlatform().getLogger().info("Loading 1.14 -> 1.13.2 block mapping...");
        blockStateMappings = new VBMappings(mapping1_14.getAsJsonObject("blockstates"), mapping1_13_2.getAsJsonObject("blockstates"), mapping1_13_2to1_14.getAsJsonObject("blockstates"));
        blockMappings = new VBMappings(mapping1_14.getAsJsonObject("blocks"), mapping1_13_2.getAsJsonObject("blocks"), mapping1_13_2to1_14.getAsJsonObject("blocks"), false);
        soundMappings = new VBMappings(mapping1_14.getAsJsonArray("sounds"), mapping1_13_2.getAsJsonArray("sounds"), mapping1_13_2to1_14.getAsJsonObject("sounds"));
    }
}
