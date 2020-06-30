package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.data;

import us.myles.viaversion.libs.fastutil.ints.Int2IntMap;
import us.myles.viaversion.libs.fastutil.ints.Int2IntOpenHashMap;

public class MapColorRewriter {

    private static final Int2IntMap MAPPINGS = new Int2IntOpenHashMap();

    static {
        MAPPINGS.put(208, 113);
        MAPPINGS.put(209, 114);
        MAPPINGS.put(210, 114);
        MAPPINGS.put(211, 112);
        MAPPINGS.put(212, 152);
        MAPPINGS.put(213, 83);
        MAPPINGS.put(214, 83);
        MAPPINGS.put(215, 155);
        MAPPINGS.put(216, 143);
        MAPPINGS.put(217, 115);
        MAPPINGS.put(218, 115);
        MAPPINGS.put(219, 143);
        MAPPINGS.put(220, 127);
        MAPPINGS.put(221, 127);
        MAPPINGS.put(222, 127);
        MAPPINGS.put(223, 95);
        MAPPINGS.put(224, 127);
        MAPPINGS.put(225, 127);
        MAPPINGS.put(226, 124);
        MAPPINGS.put(227, 95);
        MAPPINGS.put(228, 187);
        MAPPINGS.put(229, 155);
        MAPPINGS.put(230, 184);
        MAPPINGS.put(231, 187);
        MAPPINGS.put(232, 127);
        MAPPINGS.put(233, 124);
        MAPPINGS.put(234, 125);
        MAPPINGS.put(235, 127);
    }

    public static int getMappedColor(int color) {
        return MAPPINGS.getOrDefault(color, -1);
    }
}
