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
package com.viaversion.viabackwards.item;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viaversion.api.minecraft.item.DataItem;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonParser;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import com.viaversion.viaversion.libs.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Prevent expensive parsing/toString by checking against cached JsonElement instances, used from 1.14 to 1.20.5.
 * <p>
 * When using this, be careful not to break caching by modifying the display tag directly.
 */
public final class DataItemWithExtras extends DataItem {

    private JsonElement name;
    private List<JsonElement> lore;

    public DataItemWithExtras(final Item from) {
        setIdentifier(from.identifier());
        setAmount(from.amount());
        setData(from.data());
        setTag(from.tag());

        if (tag() == null) {
            return;
        }

        final CompoundTag display = tag().getCompoundTag("display");
        if (display == null) {
            return;
        }

        final StringTag name = display.getStringTag("Name");
        if (name != null) {
            this.name = parse(name.getValue());
        }

        final ListTag<StringTag> lore = display.getListTag("Lore", StringTag.class);
        if (lore != null) {
            this.lore = new ArrayList<>(lore.size());
            for (int i = 0; i < lore.size(); i++) {
                this.lore.add(parse(lore.get(i).getValue()));
            }
        }
    }

    public @Nullable JsonElement name() {
        return name;
    }

    public @Nullable StringTag rawName() {
        if (tag() == null) {
            return null;
        }
        final CompoundTag display = tag().getCompoundTag("display");
        return display != null ? display.getStringTag("Name") : null;
    }

    public @Nullable List<JsonElement> lore() {
        return lore;
    }

    public @Nullable ListTag<StringTag> rawLore() {
        if (tag() == null) {
            return null;
        }
        final CompoundTag display = tag().getCompoundTag("display");
        return display != null ? display.getListTag("Lore", StringTag.class) : null;
    }

    private JsonElement parse(final String value) {
        try {
            return JsonParser.parseString(value);
        } catch (final JsonSyntaxException e) {
            return new JsonPrimitive(value);
        }
    }
}
