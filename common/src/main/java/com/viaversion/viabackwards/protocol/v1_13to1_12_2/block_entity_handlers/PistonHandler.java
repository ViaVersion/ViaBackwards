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
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.providers.BackwardsBlockEntityProvider;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.MappingDataLoader;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntOpenHashMap;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.blockconnections.ConnectionData;
import java.util.Map;
import java.util.StringJoiner;

public class PistonHandler implements BackwardsBlockEntityProvider.BackwardsBlockEntityHandler {

    private final Object2IntMap<String> pistonIds = new Object2IntOpenHashMap<>();

    public PistonHandler() {
        pistonIds.defaultReturnValue(-1);
        if (Via.getConfig().isServersideBlockConnections()) {
            Map<String, Integer> keyToId = ConnectionData.getKeyToId();
            for (Map.Entry<String, Integer> entry : keyToId.entrySet()) {
                if (!entry.getKey().contains("piston")) {
                    continue;
                }

                addEntries(entry.getKey(), entry.getValue());
            }
        } else {
            ListTag<StringTag> blockStates = MappingDataLoader.INSTANCE.loadNBT("blockstates-1.13.nbt").getListTag("blockstates", StringTag.class);
            for (int id = 0; id < blockStates.size(); id++) {
                StringTag state = blockStates.get(id);
                String key = state.getValue();
                if (!key.contains("piston")) {
                    continue;
                }

                addEntries(key, id);
            }
        }
    }

    // There doesn't seem to be a nicer way around it :(
    private void addEntries(String data, int id) {
        id = Protocol1_13To1_12_2.MAPPINGS.getNewBlockStateId(id);
        pistonIds.put(data, id);

        String substring = data.substring(10);
        if (!substring.startsWith("piston") && !substring.startsWith("sticky_piston")) return;

        // Swap properties and add them to the map
        String[] split = data.substring(0, data.length() - 1).split("\\[");
        String[] properties = split[1].split(",");
        data = split[0] + "[" + properties[1] + "," + properties[0] + "]";
        pistonIds.put(data, id);
    }

    @Override
    public CompoundTag transform(int blockId, CompoundTag tag) {
        CompoundTag blockState = tag.getCompoundTag("blockState");
        if (blockState == null) return tag;

        String dataFromTag = getDataFromTag(blockState);
        if (dataFromTag == null) return tag;

        int id = pistonIds.getInt(dataFromTag);
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
