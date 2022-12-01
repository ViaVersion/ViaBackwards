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
package com.viaversion.viabackwards.protocol.protocol1_15_2to1_16.data;

import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.protocols.protocol1_16to1_15_2.Protocol1_16To1_15_2;

import java.util.HashMap;
import java.util.Map;

public class BackwardsMappings extends com.viaversion.viabackwards.api.data.BackwardsMappings {
    private final Map<String, String> attributeMappings = new HashMap<>();

    public BackwardsMappings() {
        super("1.16", "1.15", Protocol1_16To1_15_2.class, true);
    }

    @Override
    protected void loadVBExtras(JsonObject unmapped, JsonObject mapped) {
        for (Map.Entry<String, String> entry : Protocol1_16To1_15_2.MAPPINGS.getAttributeMappings().entrySet()) {
            attributeMappings.put(entry.getValue(), entry.getKey());
        }
    }

    public Map<String, String> getAttributeMappings() {
        return attributeMappings;
    }
}