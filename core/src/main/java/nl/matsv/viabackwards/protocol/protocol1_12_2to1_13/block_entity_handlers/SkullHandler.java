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

import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.providers.BackwardsBlockEntityProvider.BackwardsBlockEntityHandler;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.viaversion.libs.opennbt.tag.builtin.ByteTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;

public class SkullHandler implements BackwardsBlockEntityHandler {
    private final int SKULL_START = 5447;

    @Override
    public CompoundTag transform(UserConnection user, int blockId, CompoundTag tag) {
        int pos = (blockId - SKULL_START) % 20;
        byte type = (byte) Math.floor((blockId - SKULL_START) / 20);

        // Set type
        tag.put(new ByteTag("SkullType", type));

        // Remove wall skulls
        if (pos < 4) {
            return tag;
        }

        // Add rotation for normal skulls
        tag.put(new ByteTag("Rot", (byte) ((pos - 4) & 255)));

        return tag;
    }
}
