/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2022 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.viabackwards.protocol.protocol1_13_2to1_14.storage;

import com.viaversion.viaversion.api.connection.StoredObject;
import com.viaversion.viaversion.api.connection.UserConnection;

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
