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
package com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.data;

import com.viaversion.viabackwards.api.data.VBMappingDataLoader;
import com.viaversion.viaversion.libs.gson.JsonArray;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.Protocol1_20_5To1_20_3;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BackwardsMappings extends com.viaversion.viabackwards.api.data.BackwardsMappings {

    private String[] sounds;

    public BackwardsMappings() {
        super("1.20.5", "1.20.3", Protocol1_20_5To1_20_3.class);
    }

    @Override
    protected void loadExtras(final CompoundTag data) {
        super.loadExtras(data);

        final JsonArray sounds = VBMappingDataLoader.INSTANCE.loadData("sounds-1.20.3.json").getAsJsonArray("sounds");
        this.sounds = new String[sounds.size()];
        int i = 0;
        for (final JsonElement sound : sounds) {
            this.sounds[i++] = sound.getAsString();
        }
    }

    public @Nullable String mappedSoundName(final int mappedId) {
        return mappedId >= 0 && mappedId < sounds.length ? sounds[mappedId] : null;
    }
}
