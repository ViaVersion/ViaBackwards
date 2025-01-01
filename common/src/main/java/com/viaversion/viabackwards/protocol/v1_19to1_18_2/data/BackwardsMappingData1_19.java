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
package com.viaversion.viabackwards.protocol.v1_19to1_18_2.data;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.data.BackwardsMappingDataLoader;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.viaversion.protocols.v1_18_2to1_19.Protocol1_18_2To1_19;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BackwardsMappingData1_19 extends BackwardsMappingData {

    private final boolean sculkShriekerToCryingObsidian = ViaBackwards.getConfig().sculkShriekerToCryingObsidian();

    private final Int2ObjectMap<CompoundTag> defaultChatTypes = new Int2ObjectOpenHashMap<>();

    public BackwardsMappingData1_19() {
        super("1.19", "1.18", Protocol1_18_2To1_19.class);
    }

    @Override
    protected void loadExtras(final CompoundTag data) {
        super.loadExtras(data);

        if (sculkShriekerToCryingObsidian) {
            blockMappings.setNewId(850, 750);
            for (int i = 18900; i <= 18907; i++) {
                blockStateMappings.setNewId(i, 16082);
            }
        }

        final ListTag<CompoundTag> chatTypes = BackwardsMappingDataLoader.INSTANCE.loadNBT("chat-types-1.19.1.nbt").getListTag("values", CompoundTag.class);
        for (final CompoundTag chatType : chatTypes) {
            final NumberTag idTag = chatType.getNumberTag("id");
            defaultChatTypes.put(idTag.asInt(), chatType);
        }
    }

    @Override
    public int getNewItemId(final int id) {
        if (sculkShriekerToCryingObsidian && id == 329) {
            return 1065;
        }
        return super.getNewItemId(id);
    }

    public @Nullable CompoundTag chatType(final int id) {
        return defaultChatTypes.get(id);
    }
}
