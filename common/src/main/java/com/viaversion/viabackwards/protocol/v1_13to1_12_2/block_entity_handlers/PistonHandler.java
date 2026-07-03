/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
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

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.provider.BackwardsBlockEntityProvider;
import java.util.Map;
import java.util.StringJoiner;

public class PistonHandler implements BackwardsBlockEntityProvider.BackwardsBlockEntityHandler {

    @Override
    public CompoundTag transform(int blockId, CompoundTag tag) {
        CompoundTag blockState = tag.getCompoundTag("blockState");
        if (blockState == null) return tag;

        String dataFromTag = getDataFromTag(blockState);
        if (dataFromTag == null) return tag;

        int id = Protocol1_13To1_12_2.MAPPINGS.getPistonIds().getInt(dataFromTag);
        if (id == -1) {
            //TODO see why this could be null and if this is bad
            return tag;
        }

        tag.putInt("blockId", id >> 4);
        tag.putInt("blockData", id & 15);
        return tag;
    }

    // The type hasn't actually been updated in the blockstorage, so we need to construct it
    private String getDataFromTag(CompoundTag tag) {
        StringTag name = tag.getStringTag("Name");
        if (name == null) return null;

        CompoundTag properties = tag.getCompoundTag("Properties");
        if (properties == null) return name.getValue();

        StringJoiner joiner = new StringJoiner(",", name.getValue() + "[", "]");
        for (Map.Entry<String, Tag> entry : properties) {
            if (!(entry.getValue() instanceof StringTag)) continue;
            joiner.add(entry.getKey() + "=" + ((StringTag) entry.getValue()).getValue());
        }
        return joiner.toString();
    }
}
