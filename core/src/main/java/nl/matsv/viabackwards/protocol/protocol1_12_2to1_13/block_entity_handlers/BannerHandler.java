/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
    private final int WALL_BANNER_START = 7110; // 4 each
    private final int WALL_BANNER_STOP = 7173;

    private final int BANNER_START = 6854; // 16 each
    private final int BANNER_STOP = 7109;

    @Override
    public CompoundTag transform(UserConnection user, int blockId, CompoundTag tag) {
        // Normal banners
        if (blockId >= BANNER_START && blockId <= BANNER_STOP) {
            int color = (blockId - BANNER_START) >> 4;
            tag.put(new IntTag("Base", (15 - color)));
        }
        // Wall banners
        else if (blockId >= WALL_BANNER_START && blockId <= WALL_BANNER_STOP) {
            int color = (blockId - WALL_BANNER_START) >> 2;
            tag.put(new IntTag("Base", (15 - color)));
        } else {
            ViaBackwards.getPlatform().getLogger().warning("Why does this block have the banner block entity? :(" + tag);
        }

        // Invert colors
        if (tag.contains("Patterns") && tag.get("Patterns") instanceof ListTag) {
            for (Tag pattern : (ListTag) tag.get("Patterns")) {
                if (pattern instanceof CompoundTag) {
                    IntTag c = ((CompoundTag) pattern).get("Color");
                    c.setValue(15 - c.getValue()); // Invert color id
                }
            }
        }

        return tag;
    }
}
