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
package com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.data;

import com.viaversion.viaversion.libs.gson.JsonArray;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.Protocol1_19To1_18_2;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BackwardsMappings extends com.viaversion.viabackwards.api.data.BackwardsMappings {

    private String[] argumentTypes;

    public BackwardsMappings() {
        super("1.19", "1.18", Protocol1_19To1_18_2.class, true);
    }

    @Override
    protected void loadExtras(final JsonObject oldMappings, final JsonObject newMappings, @Nullable final JsonObject diffMappings) {
        super.loadExtras(oldMappings, newMappings, diffMappings);
        int i = 0;
        final JsonArray types = oldMappings.getAsJsonArray("argumenttypes");
        this.argumentTypes = new String[types.size()];
        for (final JsonElement element : types) {
            final String id = element.getAsString();
            this.argumentTypes[i++] = id;
        }
    }

    public @Nullable String argumentType(final int argumentTypeId) {
        return argumentTypeId >= 0 && argumentTypeId < argumentTypes.length ? argumentTypes[argumentTypeId] : null;
    }
}