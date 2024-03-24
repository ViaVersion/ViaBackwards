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
package com.viaversion.viabackwards.protocol.protocol1_19_4to1_20.data;

import com.viaversion.viabackwards.api.data.BackwardsMappingDataLoader;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.protocols.protocol1_20to1_19_4.Protocol1_20To1_19_4;

public class BackwardsMappings extends com.viaversion.viabackwards.api.data.BackwardsMappings {

    private CompoundTag trimPatternRegistry;

    public BackwardsMappings() {
        super("1.20", "1.19.4", Protocol1_20To1_19_4.class);
    }

    @Override
    protected void loadExtras(CompoundTag data) {
        super.loadExtras(data);

        trimPatternRegistry = BackwardsMappingDataLoader.INSTANCE.loadNBT("trim_pattern-1.19.4.nbt");
    }

    public CompoundTag getTrimPatternRegistry() {
        return trimPatternRegistry.copy();
    }
}
