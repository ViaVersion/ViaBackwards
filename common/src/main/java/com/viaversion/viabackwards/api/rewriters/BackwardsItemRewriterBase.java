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
package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.rewriter.ItemRewriter;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class BackwardsItemRewriterBase<C extends ClientboundPacketType, S extends ServerboundPacketType,
    T extends BackwardsProtocol<C, ?, ?, S>> extends ItemRewriter<C, S, T> {

    protected final boolean jsonNameFormat;

    protected BackwardsItemRewriterBase(T protocol, Type<Item> itemType, Type<Item[]> itemArrayType, Type<Item> mappedItemType, Type<Item[]> mappedItemArrayType, boolean jsonFormat) {
        super(protocol, itemType, itemArrayType, mappedItemType, mappedItemArrayType);
        this.jsonNameFormat = jsonFormat;
    }

    protected BackwardsItemRewriterBase(T protocol, Type<Item> itemType, Type<Item[]> itemArrayType, boolean jsonNameFormat) {
        this(protocol, itemType, itemArrayType, itemType, itemArrayType, jsonNameFormat);
    }

    @Override
    public @Nullable Item handleItemToServer(UserConnection connection, @Nullable Item item) {
        if (item == null) return null;
        super.handleItemToServer(connection, item);

        restoreDisplayTag(item);
        return item;
    }

    protected boolean hasBackupTag(CompoundTag tag, String tagName) {
        return tag.contains(nbtTagName(tagName));
    }

    protected void saveStringTag(CompoundTag tag, StringTag original, String name) {
        // Multiple places might try to backup data
        String backupName = nbtTagName(name);
        if (!tag.contains(backupName)) {
            tag.putString(backupName, original.getValue());
        }
    }

    protected void saveListTag(CompoundTag tag, ListTag<?> original, String name) {
        // Multiple places might try to backup data
        String backupName = nbtTagName(name);
        if (!tag.contains(backupName)) {
            tag.put(backupName, original.copy());
        }
    }

    protected void saveGenericTagList(CompoundTag tag, List<Tag> original, String name) {
        // List tags cannot contain tags of different types, so we have to store them a bit more awkwardly as an indexed compound tag
        String backupName = nbtTagName(name);
        if (!tag.contains(backupName)) {
            CompoundTag output = new CompoundTag();
            for (int i = 0; i < original.size(); i++) {
                output.put(Integer.toString(i), original.get(i));
            }
            tag.put(backupName, output);
        }
    }

    protected List<Tag> removeGenericTagList(CompoundTag tag, String name) {
        String backupName = nbtTagName(name);
        CompoundTag data = tag.getCompoundTag(backupName);
        if (data == null) {
            return null;
        }

        tag.remove(backupName);
        return new ArrayList<>(data.values());
    }

    protected void restoreDisplayTag(Item item) {
        if (item.tag() == null) return;

        CompoundTag display = item.tag().getCompoundTag("display");
        if (display != null) {
            // Remove custom name / restore original name
            if (display.remove(nbtTagName("customName")) != null) {
                display.remove("Name");
            } else {
                restoreStringTag(display, "Name");
            }

            // Restore lore
            restoreListTag(display, "Lore");
        }
    }

    protected void restoreStringTag(CompoundTag tag, String tagName) {
        Tag original = tag.remove(nbtTagName(tagName));
        if (original instanceof StringTag) {
            tag.putString(tagName, ((StringTag) original).getValue());
        }
    }

    protected void restoreListTag(CompoundTag tag, String tagName) {
        Tag original = tag.remove(nbtTagName(tagName));
        if (original instanceof ListTag) {
            tag.put(tagName, ((ListTag<?>) original).copy());
        }
    }

    public <T extends Tag> @Nullable ListTag<T> removeListTag(CompoundTag tag, String tagName, Class<T> tagType) {
        String backupName = nbtTagName(tagName);
        ListTag<T> data = tag.getListTag(backupName, tagType);
        if (data == null) {
            return null;
        }

        tag.remove(backupName);
        return data;
    }

    @Override
    public String nbtTagName() {
        return "VB|" + protocol.getClass().getSimpleName();
    }
}
