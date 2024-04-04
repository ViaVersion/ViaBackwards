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
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.rewriter.ItemRewriter;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class ItemRewriterBase<C extends ClientboundPacketType, S extends ServerboundPacketType,
    T extends BackwardsProtocol<C, ?, ?, S>> extends ItemRewriter<C, S, T> {

    protected final boolean jsonNameFormat;

    protected ItemRewriterBase(T protocol, Type<Item> itemType, Type<Item[]> itemArrayType, Type<Item> mappedItemType, Type<Item[]> mappedItemArrayType, boolean jsonFormat) {
        super(protocol, itemType, itemArrayType, mappedItemType, mappedItemArrayType);
        this.jsonNameFormat = jsonFormat;
    }

    protected ItemRewriterBase(T protocol, Type<Item> itemType, Type<Item[]> itemArrayType, boolean jsonNameFormat) {
        this(protocol, itemType, itemArrayType, itemType, itemArrayType, jsonNameFormat);
    }

    @Override
    public @Nullable Item handleItemToServer(@Nullable Item item) {
        if (item == null) return null;
        super.handleItemToServer(item);

        restoreDisplayTag(item);
        return item;
    }

    protected boolean hasBackupTag(CompoundTag displayTag, String tagName) {
        return displayTag.contains(nbtTagName("o" + tagName));
    }

    protected void saveStringTag(CompoundTag displayTag, StringTag original, String name) {
        // Multiple places might try to backup data
        String backupName = nbtTagName("o" + name);
        if (!displayTag.contains(backupName)) {
            displayTag.putString(backupName, original.getValue());
        }
    }

    protected void saveListTag(CompoundTag displayTag, ListTag<?> original, String name) {
        // Multiple places might try to backup data
        String backupName = nbtTagName("o" + name);
        if (!displayTag.contains(backupName)) {
            displayTag.put(backupName, original.copy());
        }
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
        Tag original = tag.remove(nbtTagName("o" + tagName));
        if (original instanceof StringTag) {
            tag.putString(tagName, ((StringTag) original).getValue());
        }
    }

    protected void restoreListTag(CompoundTag tag, String tagName) {
        Tag original = tag.remove(nbtTagName("o" + tagName));
        if (original instanceof ListTag) {
            tag.put(tagName, ((ListTag<?>) original).copy());
        }
    }
}
