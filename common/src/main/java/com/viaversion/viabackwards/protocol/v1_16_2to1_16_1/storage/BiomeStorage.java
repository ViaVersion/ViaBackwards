/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2024 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_16_2to1_16_1.storage;

import com.viaversion.viabackwards.protocol.v1_16_2to1_16_1.data.BiomeMappings1_16_1;
import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntOpenHashMap;

public final class BiomeStorage implements StorableObject {

    private final Int2IntMap modernToLegacyBiomes = new Int2IntOpenHashMap();

    public BiomeStorage() {
        modernToLegacyBiomes.defaultReturnValue(-1);
    }

    public void addBiome(final String biome, final int id) {
        modernToLegacyBiomes.put(id, BiomeMappings1_16_1.toLegacyBiome(biome));
    }

    public int legacyBiome(final int biome) {
        return modernToLegacyBiomes.get(biome);
    }

    public void clear() {
        modernToLegacyBiomes.clear();
    }
}
