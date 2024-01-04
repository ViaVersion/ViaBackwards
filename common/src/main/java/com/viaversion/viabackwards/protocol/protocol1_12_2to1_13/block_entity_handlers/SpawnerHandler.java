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

package com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers;

import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.data.EntityNameRewrites;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.providers.BackwardsBlockEntityProvider;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;

public class SpawnerHandler implements BackwardsBlockEntityProvider.BackwardsBlockEntityHandler {

    @Override
    public CompoundTag transform(UserConnection user, int blockId, CompoundTag tag) {
        Tag dataTag = tag.get("SpawnData");
        if (dataTag instanceof CompoundTag) {
            CompoundTag data = (CompoundTag) dataTag;
            Tag idTag = data.get("id");
            if (idTag instanceof StringTag) {
                StringTag s = (StringTag) idTag;
                s.setValue(EntityNameRewrites.rewrite(s.getValue()));
            }
        }
        return tag;
    }
}
