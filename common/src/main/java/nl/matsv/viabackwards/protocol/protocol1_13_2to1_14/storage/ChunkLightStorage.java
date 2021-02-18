package nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.storage;

import us.myles.ViaVersion.api.data.StoredObject;
import us.myles.ViaVersion.api.data.UserConnection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ChunkLightStorage extends StoredObject {
    public static final byte[] FULL_LIGHT = new byte[2048];
    public static final byte[] EMPTY_LIGHT = new byte[2048];
    private static Constructor<?> fastUtilLongObjectHashMap;

    private final Map<Long, ChunkLight> storedLight = createLongObjectMap();

    static {
        Arrays.fill(FULL_LIGHT, (byte) 0xFF);
        Arrays.fill(EMPTY_LIGHT, (byte) 0x0);
        try {
            fastUtilLongObjectHashMap = Class.forName("it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap").getConstructor();
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
        }
    }

    public ChunkLightStorage(UserConnection user) {
        super(user);
    }

    public void setStoredLight(byte[][] skyLight, byte[][] blockLight, int x, int z) {
        storedLight.put(getChunkSectionIndex(x, z), new ChunkLight(skyLight, blockLight));
    }

    public ChunkLight getStoredLight(int x, int z) {
        return storedLight.get(getChunkSectionIndex(x, z));
    }

    public void clear() {
        storedLight.clear();
    }

    public void unloadChunk(int x, int z) {
        storedLight.remove(getChunkSectionIndex(x, z));
    }

    private long getChunkSectionIndex(int x, int z) {
        return ((x & 0x3FFFFFFL) << 38) | (z & 0x3FFFFFFL);
    }

    private Map<Long, ChunkLight> createLongObjectMap() {
        if (fastUtilLongObjectHashMap != null) {
            try {
                return (Map<Long, ChunkLight>) fastUtilLongObjectHashMap.newInstance();
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return new HashMap<>();
    }

    public static class ChunkLight {
        private final byte[][] skyLight;
        private final byte[][] blockLight;

        public ChunkLight(byte[][] skyLight, byte[][] blockLight) {
            this.skyLight = skyLight;
            this.blockLight = blockLight;
        }

        public byte[][] getSkyLight() {
            return skyLight;
        }

        public byte[][] getBlockLight() {
            return blockLight;
        }
    }
}
