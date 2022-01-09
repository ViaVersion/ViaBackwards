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
package com.viaversion.viabackwards.api.data;

import com.viaversion.viaversion.api.data.IntArrayMappings;
import com.viaversion.viaversion.api.data.MappingDataLoader;
import com.viaversion.viaversion.api.data.Mappings;

import java.util.Arrays;

public final class VBMappings extends IntArrayMappings {

    private VBMappings(final int[] oldToNew, final int mappedIds) {
        super(oldToNew, mappedIds);
    }

    public static Mappings.Builder<VBMappings> vbBuilder() {
        return new Builder(VBMappings::new);
    }

    public static final class Builder extends Mappings.Builder<VBMappings> {

        private Builder(final MappingsSupplier<VBMappings> supplier) {
            super(supplier);
        }

        @Override
        public VBMappings build() {
            final int size = this.size != -1 ? this.size : size(unmapped);
            final int mappedSize = this.mappedSize != -1 ? this.mappedSize : size(mapped);
            final int[] mappings = new int[size];
            Arrays.fill(mappings, -1);
            // Do conversion if one is an array and the other an object, otherwise directly map
            if (unmapped.isJsonArray()) {
                if (mapped.isJsonObject()) {
                    VBMappingDataLoader.mapIdentifiers(mappings, toJsonObject(unmapped.getAsJsonArray()), mapped.getAsJsonObject(), diffMappings, warnOnMissing);
                } else {
                    // Use the normal loader
                    MappingDataLoader.mapIdentifiers(mappings, unmapped.getAsJsonArray(), mapped.getAsJsonArray(), diffMappings, warnOnMissing);
                }
            } else if (mapped.isJsonArray()) {
                VBMappingDataLoader.mapIdentifiers(mappings, unmapped.getAsJsonObject(), toJsonObject(mapped.getAsJsonArray()), diffMappings, warnOnMissing);
            } else {
                VBMappingDataLoader.mapIdentifiers(mappings, unmapped.getAsJsonObject(), mapped.getAsJsonObject(), diffMappings, warnOnMissing);
            }
            return supplier.supply(mappings, mappedSize);
        }
    }
}
