package nl.matsv.viabackwards.protocol.protocol1_16_1to1_16_2.data;
import nl.matsv.viabackwards.ViaBackwards;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.protocols.protocol1_16_2to1_16_1.data.BiomeMappings;
import us.myles.viaversion.libs.fastutil.ints.Int2IntMap;
import us.myles.viaversion.libs.fastutil.ints.Int2IntOpenHashMap;

public class BackwardsBiomeMappings {

    private static final Int2IntMap BIOMES = new Int2IntOpenHashMap();
    private static final int HIGHEST_VANILLA_BIOME;

    static {
        int lastBiome = 0;
        for (Int2IntMap.Entry entry : BiomeMappings.getBiomes().int2IntEntrySet()) {
            int newBiome = entry.getIntValue();
            BIOMES.put(newBiome, entry.getIntKey());
            if (newBiome > lastBiome) {
                lastBiome = newBiome;
            }
        }

        HIGHEST_VANILLA_BIOME = lastBiome;
    }

    public static int getOldBiomeId(int biomeId) {
        if (biomeId > HIGHEST_VANILLA_BIOME) {
            if (!Via.getConfig().isSuppressConversionWarnings()) {
                ViaBackwards.getPlatform().getLogger().warning("Custom biome id could not be mapped: " + biomeId);
            }
            return 0;
        }

        return BIOMES.getOrDefault(biomeId, biomeId);
    }
}
