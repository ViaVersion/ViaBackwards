/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2025 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_20_3to1_20_2.storage;

import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.libs.fastutil.Pair;
import java.util.Objects;

public class SpawnPositionStorage implements StorableObject {
    public static final Pair<BlockPosition, Float> DEFAULT_SPAWN_POSITION = Pair.of(new BlockPosition(8, 64, 8), 0.0F); // Default values copied from the original client

    private Pair<BlockPosition, Float> spawnPosition;
    private String dimension;

    public Pair<BlockPosition, Float> getSpawnPosition() {
        return spawnPosition;
    }

    public void setSpawnPosition(final Pair<BlockPosition, Float> spawnPosition) {
        this.spawnPosition = spawnPosition;
    }

    /**
     * Sets the dimension and resets the spawn position to the default value if the dimension changed.
     *
     * @param dimension The new dimension
     */
    public void setDimension(final String dimension) {
        final boolean changed = !Objects.equals(this.dimension, dimension);
        this.dimension = dimension;

        if (changed) {
            this.spawnPosition = DEFAULT_SPAWN_POSITION;
        }
    }
}
