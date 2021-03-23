/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
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
package nl.matsv.viabackwards.api.data;

import us.myles.ViaVersion.api.data.Mappings;
import us.myles.viaversion.libs.gson.JsonArray;
import us.myles.viaversion.libs.gson.JsonObject;

import java.util.Arrays;

public class VBMappings extends Mappings {

    public VBMappings(int size, JsonObject oldMapping, JsonObject newMapping, JsonObject diffMapping, boolean warnOnMissing) {
        super(create(size, oldMapping, newMapping, diffMapping, warnOnMissing));
    }

    public VBMappings(JsonObject oldMapping, JsonObject newMapping, JsonObject diffMapping, boolean warnOnMissing) {
        super(create(oldMapping.entrySet().size(), oldMapping, newMapping, diffMapping, warnOnMissing));
    }

    public VBMappings(JsonObject oldMapping, JsonObject newMapping, boolean warnOnMissing) {
        this(oldMapping, newMapping, null, warnOnMissing);
    }

    public VBMappings(JsonArray oldMapping, JsonArray newMapping, JsonObject diffMapping, boolean warnOnMissing) {
        super(oldMapping.size(), oldMapping, newMapping, diffMapping, warnOnMissing);
    }

    private static short[] create(int size, JsonObject oldMapping, JsonObject newMapping, JsonObject diffMapping, boolean warnOnMissing) {
        short[] oldToNew = new short[size];
        Arrays.fill(oldToNew, (short) -1);
        VBMappingDataLoader.mapIdentifiers(oldToNew, oldMapping, newMapping, diffMapping, warnOnMissing);
        return oldToNew;
    }
}
