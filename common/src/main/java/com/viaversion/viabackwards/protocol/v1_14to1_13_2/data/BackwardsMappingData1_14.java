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
package com.viaversion.viabackwards.protocol.v1_14to1_13_2.data;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.Protocol1_13_2To1_14;

public final class BackwardsMappingData1_14 extends BackwardsMappingData {

    private final boolean scaffoldingToWater = ViaBackwards.getConfig().scaffoldingToWater();

    public BackwardsMappingData1_14() {
        super("1.14", "1.13.2", Protocol1_13_2To1_14.class);
    }

    @Override
    protected void loadExtras(final CompoundTag data) {
        super.loadExtras(data);

        if (scaffoldingToWater) {
            blockMappings.setNewId(658, 26);
            for (int i = 11099; i <= 11130; i++) {
                blockStateMappings.setNewId(i, 49);
            }
        }
    }

}
