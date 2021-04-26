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
package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.MappedItem;
import org.checkerframework.checker.nullness.qual.Nullable;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;

public abstract class ItemRewriter<T extends BackwardsProtocol> extends ItemRewriterBase<T> {

    private final TranslatableRewriter translatableRewriter;

    protected ItemRewriter(T protocol, @Nullable TranslatableRewriter translatableRewriter) {
        super(protocol, true);
        this.translatableRewriter = translatableRewriter;
    }

    @Override
    public @Nullable Item handleItemToClient(Item item) {
        if (item == null) return null;

        CompoundTag display = item.getTag() != null ? item.getTag().get("display") : null;
        if (translatableRewriter != null && display != null) {
            // Handle name and lore components
            StringTag name = display.get("Name");
            if (name != null) {
                String newValue = translatableRewriter.processText(name.getValue()).toString();
                if (!newValue.equals(name.getValue())) {
                    saveStringTag(display, name, "Name");
                }

                name.setValue(newValue);
            }

            ListTag lore = display.get("Lore");
            if (lore != null) {
                boolean changed = false;
                for (Tag loreEntryTag : lore) {
                    if (!(loreEntryTag instanceof StringTag)) continue;

                    StringTag loreEntry = (StringTag) loreEntryTag;
                    String newValue = translatableRewriter.processText(loreEntry.getValue()).toString();
                    if (!changed && !newValue.equals(loreEntry.getValue())) {
                        // Backup original lore before doing any modifications
                        changed = true;
                        saveListTag(display, lore, "Lore");
                    }

                    loreEntry.setValue(newValue);
                }
            }
        }

        MappedItem data = protocol.getMappingData().getMappedItem(item.getIdentifier());
        if (data == null) {
            // Just rewrite the id
            return super.handleItemToClient(item);
        }

        if (item.getTag() == null) {
            item.setTag(new CompoundTag());
        }

        // Save original id, set remapped id
        item.getTag().put(nbtTagName + "|id", new IntTag(item.getIdentifier()));
        item.setIdentifier(data.getId());

        // Set custom name - only done if there is no original one
        if (display == null) {
            item.getTag().put("display", display = new CompoundTag());
        }
        if (!display.contains("Name")) {
            display.put("Name", new StringTag(data.getJsonName()));
            display.put(nbtTagName + "|customName", new ByteTag());
        }
        return item;
    }

    @Override
    public @Nullable Item handleItemToServer(Item item) {
        if (item == null) return null;

        super.handleItemToServer(item);
        if (item.getTag() != null) {
            IntTag originalId = item.getTag().remove(nbtTagName + "|id");
            if (originalId != null) {
                item.setIdentifier(originalId.asInt());
            }
        }
        return item;
    }
}
