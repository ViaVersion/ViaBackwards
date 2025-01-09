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
package com.viaversion.viabackwards.protocol.v1_16to1_15_2.data;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.Protocol1_15_2To1_16;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.data.AttributeMappings1_16;
import com.viaversion.viaversion.util.Key;
import java.util.HashMap;
import java.util.Map;

public class BackwardsMappingData1_16 extends BackwardsMappingData {
    private final Map<String, String> attributeMappings = new HashMap<>();

    public BackwardsMappingData1_16() {
        super("1.16", "1.15", Protocol1_15_2To1_16.class);
    }

    @Override
    protected void loadExtras(final CompoundTag data) {
        super.loadExtras(data);
        for (Map.Entry<String, String> entry : AttributeMappings1_16.attributeIdentifierMappings().entrySet()) {
            attributeMappings.put(Key.stripMinecraftNamespace(entry.getValue()), entry.getKey());
        }
    }

    public String mappedAttributeIdentifier(final String identifier) {
        return attributeMappings.getOrDefault(identifier, identifier);
    }
}
