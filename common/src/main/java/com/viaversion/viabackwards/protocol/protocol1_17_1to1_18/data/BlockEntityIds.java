package com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.data;

import java.util.Arrays;

public final class BlockEntityIds {

    private static final int[] IDS;

    static {
        final int[] ids = com.viaversion.viaversion.protocols.protocol1_18to1_17_1.BlockEntityIds.getIds();
        IDS = new int[Arrays.stream(ids).max().getAsInt() + 1];
        for (int i = 0; i < ids.length; i++) {
            IDS[ids[i]] = i;
        }
    }

    public static int mappedId(final int id) {
        if (id < 0 || id > IDS.length) {
            return -1;
        }
        return IDS[id];
    }
}
