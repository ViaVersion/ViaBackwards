package nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data;

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
        JsonObject mapping1_14 = MappingDataLoader.loadData("mapping-1.14.json");
        JsonObject mapping1_15 = MappingDataLoader.loadData("mapping-1.15.json");
        JsonObject mapping1_14to1_15 = VBMappingDataLoader.loadFromDataDir("mapping-1.14.4to1.15.json");

        ViaBackwards.getPlatform().getLogger().info("Loading 1.15 -> 1.14.4 mappings...");
        blockStateMappings = new VBMappings(mapping1_15.getAsJsonObject("blockstates"), mapping1_14.getAsJsonObject("blockstates"), mapping1_14to1_15.getAsJsonObject("blockstates"));
        blockMappings = new VBMappings(mapping1_15.getAsJsonObject("blocks"), mapping1_14.getAsJsonObject("blocks"), mapping1_14to1_15.getAsJsonObject("blocks"), false);
        itemMappings = new VBItemMappings(mapping1_15.getAsJsonObject("items"), mapping1_14.getAsJsonObject("items"), mapping1_14to1_15.getAsJsonObject("items"));
        soundMappings = new VBSoundMappings(mapping1_15.getAsJsonArray("sounds"), mapping1_14.getAsJsonArray("sounds"), mapping1_14to1_15.getAsJsonObject("sounds"));
    }
}