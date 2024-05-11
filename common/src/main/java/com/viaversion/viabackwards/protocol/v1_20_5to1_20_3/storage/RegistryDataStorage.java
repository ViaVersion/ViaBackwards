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
package com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.storage;

import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.nbt.tag.CompoundTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class RegistryDataStorage implements StorableObject {

    private final CompoundTag registryData = new CompoundTag();
    private String[] dimensionKeys;

    public CompoundTag registryData() {
        return registryData;
    }

    public String @Nullable [] dimensionKeys() {
        return dimensionKeys;
    }

    public void setDimensionKeys(final String[] dimensionKeys) {
        this.dimensionKeys = dimensionKeys;
    }

    public void clear() {
        registryData.clear();
        dimensionKeys = null;
    }
}
