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
package com.viaversion.viabackwards.protocol.v1_20to1_19_4.data;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.data.BackwardsMappingDataLoader;
import com.viaversion.viaversion.protocols.v1_19_4to1_20.Protocol1_19_4To1_20;

public class BackwardsMappingData1_20 extends BackwardsMappingData {

    private CompoundTag trimPatternRegistry;

    public BackwardsMappingData1_20() {
        super("1.20", "1.19.4", Protocol1_19_4To1_20.class);
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
