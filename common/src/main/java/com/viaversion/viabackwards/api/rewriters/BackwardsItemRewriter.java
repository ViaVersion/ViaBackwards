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
package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.nbt.tag.ByteTag;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.MappedItem;
import com.viaversion.viabackwards.item.DataItemWithExtras;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.gson.JsonElement;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BackwardsItemRewriter<C extends ClientboundPacketType, S extends ServerboundPacketType,
    T extends BackwardsProtocol<C, ?, ?, S>> extends BackwardsItemRewriterBase<C, S, T> {

    public BackwardsItemRewriter(T protocol, Type<Item> itemType, Type<Item[]> itemArrayType) {
        super(protocol, itemType, itemArrayType, true);
    }

    public BackwardsItemRewriter(T protocol, Type<Item> itemType, Type<Item[]> itemArrayType, Type<Item> mappedItemType, Type<Item[]> mappedItemArrayType) {
        super(protocol, itemType, itemArrayType, mappedItemType, mappedItemArrayType, true);
    }

    @Override
    public @Nullable Item handleItemToClient(UserConnection connection, @Nullable Item item) {
        if (item == null) {
            return null;
        }

        CompoundTag display = item.tag() != null ? item.tag().getCompoundTag("display") : null;
        if (protocol.getComponentRewriter() != null && display != null) {
            final DataItemWithExtras fullItem;
            if (item instanceof DataItemWithExtras) {
                fullItem = (DataItemWithExtras) item;
            } else {
                item = fullItem = new DataItemWithExtras(item);
            }

            // Handle name and lore components
            final JsonElement name = fullItem.name();
            if (name != null) {
                final JsonElement updatedName = name.deepCopy();
                protocol.getComponentRewriter().processText(connection, updatedName);
                if (!updatedName.equals(name)) {
                    final StringTag rawName = fullItem.rawName();
                    saveStringTag(display, rawName, "Name");
                    rawName.setValue(updatedName.toString());
                }
            }

            final List<JsonElement> lore = fullItem.lore();
            if (lore != null) {
                boolean changed = false;
                final ListTag<StringTag> rawLore = fullItem.rawLore();
                for (int i = 0; i < lore.size(); i++) {
                    final JsonElement loreEntry = lore.get(i);
                    final JsonElement updatedLoreEntry = loreEntry.deepCopy();
                    protocol.getComponentRewriter().processText(connection, updatedLoreEntry);
                    if (updatedLoreEntry.equals(loreEntry)) {
                        continue;
                    }

                    if (!changed) {
                        // Backup original lore before doing any modifications
                        changed = true;
                        saveListTag(display, rawLore, "Lore");
                    }

                    final StringTag rawLoreEntry = rawLore.get(i);
                    rawLoreEntry.setValue(updatedLoreEntry.toString());
                }
            }
        }

        MappedItem data = protocol.getMappingData() != null ? protocol.getMappingData().getMappedItem(item.identifier()) : null;
        if (data == null) {
            // Just rewrite the id
            return super.handleItemToClient(connection, item);
        }

        if (item.tag() == null) {
            item.setTag(new CompoundTag());
        }

        // Save original id, set remapped id
        item.tag().putInt(nbtTagName("id"), item.identifier());
        item.setIdentifier(data.id());

        // Add custom model data
        if (data.customModelData() != null && !item.tag().contains("CustomModelData")) {
            item.tag().putInt("CustomModelData", data.customModelData());
        }

        // Set custom name - only done if there is no original one
        if (display == null) {
            item.tag().put("display", display = new CompoundTag());
        }
        if (!display.contains("Name")) {
            display.put("Name", new StringTag(data.jsonName()));
            display.put(nbtTagName("customName"), new ByteTag(false));
        }
        return item;
    }

    @Override
    public @Nullable Item handleItemToServer(UserConnection connection, @Nullable Item item) {
        if (item == null) return null;

        item = super.handleItemToServer(connection, item);
        if (item.tag() != null) {
            Tag originalId = item.tag().remove(nbtTagName("id"));
            if (originalId instanceof IntTag) {
                item.setIdentifier(((NumberTag) originalId).asInt());
            }
        }
        return item;
    }
}
