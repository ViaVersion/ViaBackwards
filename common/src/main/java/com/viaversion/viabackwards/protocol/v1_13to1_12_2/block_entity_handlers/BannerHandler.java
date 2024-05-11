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

package com.viaversion.viabackwards.protocol.v1_13to1_12_2.block_entity_handlers;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.providers.BackwardsBlockEntityProvider.BackwardsBlockEntityHandler;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.nbt.tag.Tag;

public class BannerHandler implements BackwardsBlockEntityHandler {
    private static final int WALL_BANNER_START = 7110; // 4 each
    private static final int WALL_BANNER_STOP = 7173;

    private static final int BANNER_START = 6854; // 16 each
    private static final int BANNER_STOP = 7109;

    @Override
    public CompoundTag transform(int blockId, CompoundTag tag) {
        // Normal banners
        if (blockId >= BANNER_START && blockId <= BANNER_STOP) {
            int color = (blockId - BANNER_START) >> 4;
            tag.putInt("Base", 15 - color);
        }
        // Wall banners
        else if (blockId >= WALL_BANNER_START && blockId <= WALL_BANNER_STOP) {
            int color = (blockId - WALL_BANNER_START) >> 2;
            tag.putInt("Base", 15 - color);
        } else {
            ViaBackwards.getPlatform().getLogger().warning("Why does this block have the banner block entity? :(" + tag);
        }

        // Invert colors
        ListTag<CompoundTag> patternsTag = tag.getListTag("Patterns", CompoundTag.class);
        if (patternsTag != null) {
            for (CompoundTag pattern : patternsTag) {
                NumberTag colorTag = pattern.getNumberTag("Color");
                pattern.putInt("Color", 15 - colorTag.asInt()); // Invert color id
            }
        }

        return tag;
    }
}
