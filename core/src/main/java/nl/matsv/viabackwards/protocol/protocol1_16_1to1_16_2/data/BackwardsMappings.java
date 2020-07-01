package nl.matsv.viabackwards.protocol.protocol1_16_1to1_16_2.data;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.data.VBItemMappings;
import nl.matsv.viabackwards.api.data.VBMappingDataLoader;
import nl.matsv.viabackwards.api.data.VBMappings;
import nl.matsv.viabackwards.api.data.VBSoundMappings;
import us.myles.ViaVersion.api.data.MappingDataLoader;
import us.myles.ViaVersion.api.data.Mappings;
import us.myles.viaversion.libs.gson.JsonObject;

public class BackwardsMappings {
    public static Mappings blockStateMappings;
    public static Mappings blockMappings;
    public static VBSoundMappings soundMappings;
    public static VBItemMappings itemMappings;

    public static void init() {
        ViaBackwards.getPlatform().getLogger().info("Loading 1.16.2 -> 1.16.1 mappings...");
        JsonObject mapping1_16 = MappingDataLoader.getMappingsCache().get("mapping-1.16.json");
        JsonObject mapping1_16_2 = MappingDataLoader.getMappingsCache().get("mapping-1.16.2.json");
        JsonObject mapping1_16to1_16_2 = VBMappingDataLoader.loadFromDataDir("mapping-1.16to1.16.2.json");

        blockStateMappings = new VBMappings(mapping1_16_2.getAsJsonObject("blockstates"), mapping1_16.getAsJsonObject("blockstates"), mapping1_16to1_16_2.getAsJsonObject("blockstates"));
        blockMappings = new VBMappings(mapping1_16_2.getAsJsonObject("blocks"), mapping1_16.getAsJsonObject("blocks"), mapping1_16to1_16_2.getAsJsonObject("blocks"), false);
        itemMappings = new VBItemMappings(mapping1_16_2.getAsJsonObject("items"), mapping1_16.getAsJsonObject("items"), mapping1_16to1_16_2.getAsJsonObject("items"));
        soundMappings = new VBSoundMappings(mapping1_16_2.getAsJsonArray("sounds"), mapping1_16.getAsJsonArray("sounds"), mapping1_16to1_16_2.getAsJsonObject("sounds"));
    }
}