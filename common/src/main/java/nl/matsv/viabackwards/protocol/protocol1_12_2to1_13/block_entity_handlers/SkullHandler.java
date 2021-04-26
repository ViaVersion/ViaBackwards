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

import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.providers.BackwardsBlockEntityProvider.BackwardsBlockEntityHandler;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;

public class SkullHandler implements BackwardsBlockEntityHandler {
    private static final int SKULL_START = 5447;

    @Override
    public CompoundTag transform(UserConnection user, int blockId, CompoundTag tag) {
        int diff = blockId - SKULL_START;
        int pos = diff % 20;
        byte type = (byte) Math.floor(diff / 20f);

        // Set type
        tag.put("SkullType", new ByteTag(type));

        // Remove wall skulls
        if (pos < 4) {
            return tag;
        }

        // Add rotation for normal skulls
        tag.put("Rot", new ByteTag((byte) ((pos - 4) & 255)));

        return tag;
    }
}
