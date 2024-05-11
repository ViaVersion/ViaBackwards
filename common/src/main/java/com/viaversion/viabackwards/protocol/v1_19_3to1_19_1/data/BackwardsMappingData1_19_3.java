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
package com.viaversion.viabackwards.protocol.v1_19_3to1_19_1.data;

import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.data.BackwardsMappingDataLoader;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntOpenHashMap;
import com.viaversion.viaversion.libs.gson.JsonArray;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.Protocol1_19_1To1_19_3;
import com.viaversion.viaversion.util.Key;

public final class BackwardsMappingData1_19_3 extends BackwardsMappingData {

    private final Object2IntMap<String> mappedSounds = new Object2IntOpenHashMap<>();

    public BackwardsMappingData1_19_3() {
        super("1.19.3", "1.19", Protocol1_19_1To1_19_3.class);
        mappedSounds.defaultReturnValue(-1);
    }

    @Override
    protected void loadExtras(final CompoundTag data) {
        super.loadExtras(data);

        final JsonArray sounds = BackwardsMappingDataLoader.INSTANCE.loadData("sounds-1.19.json").getAsJsonArray("sounds");
        int i = 0;
        for (final JsonElement sound : sounds) {
            mappedSounds.put(sound.getAsString(), i++);
        }
    }

    public int mappedSound(final String sound) {
        return mappedSounds.getInt(Key.stripMinecraftNamespace(sound));
    }
}