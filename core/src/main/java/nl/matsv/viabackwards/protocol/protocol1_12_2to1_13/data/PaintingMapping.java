package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data;

import java.util.HashMap;
import java.util.Map;

public class PaintingMapping {
    private static final Map<Integer, String> PAINTINGS = new HashMap<>();

    public static void init() {
        add("Kebab");
        add("Aztec");
        add("Alban");
        add("Aztec2");
        add("Bomb");
        add("Plant");
        add("Wasteland");
        add("Pool");
        add("Courbet");
        add("Sea");
        add("Sunset");
        add("Creebet");
        add("Wanderer");
        add("Graham");
        add("Match");
        add("Bust");
        add("Stage");
        add("Void");
        add("SkullAndRoses");
        add("Wither");
        add("Fighters");
        add("Pointer");
        add("Pigscene");
        add("BurningSkull");
        add("Skeleton");
        add("DonkeyKong");
    }

    private static void add(String motive) {
        PAINTINGS.put(PAINTINGS.size(), motive);
    }

    public static String getStringId(int id) {
        return PAINTINGS.getOrDefault(id, "kebab");
    }
}
