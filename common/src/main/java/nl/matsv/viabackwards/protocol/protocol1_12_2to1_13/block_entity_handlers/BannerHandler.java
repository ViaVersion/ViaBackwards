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

package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.providers.BackwardsBlockEntityProvider.BackwardsBlockEntityHandler;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.IntTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.ListTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.Tag;

public class BannerHandler implements BackwardsBlockEntityHandler {
    private static final int WALL_BANNER_START = 7110; // 4 each
    private static final int WALL_BANNER_STOP = 7173;

    private static final int BANNER_START = 6854; // 16 each
    private static final int BANNER_STOP = 7109;

    @Override
    public CompoundTag transform(UserConnection user, int blockId, CompoundTag tag) {
        // Normal banners
        if (blockId >= BANNER_START && blockId <= BANNER_STOP) {
            int color = (blockId - BANNER_START) >> 4;
            tag.put("Base", new IntTag((15 - color)));
        }
        // Wall banners
        else if (blockId >= WALL_BANNER_START && blockId <= WALL_BANNER_STOP) {
            int color = (blockId - WALL_BANNER_START) >> 2;
            tag.put("Base", new IntTag((15 - color)));
        } else {
            ViaBackwards.getPlatform().getLogger().warning("Why does this block have the banner block entity? :(" + tag);
        }

        // Invert colors
        Tag patternsTag = tag.get("Patterns");
        if (patternsTag instanceof ListTag) {
            for (Tag pattern : (ListTag) patternsTag) {
                if (!(pattern instanceof CompoundTag)) continue;

                IntTag c = ((CompoundTag) pattern).get("Color");
                c.setValue(15 - c.asInt()); // Invert color id
            }
        }

        return tag;
    }
}
