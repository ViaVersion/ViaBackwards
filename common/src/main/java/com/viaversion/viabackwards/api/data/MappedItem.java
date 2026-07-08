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
package com.viaversion.viabackwards.api.data;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import com.viaversion.viaversion.util.Key;

public class MappedItem {

    private final int id;
    private final String jsonName;
    private final Tag tagName;
    private final int customModelData;

    public MappedItem(final int id, String identifier, final String fallbackName, final int customModelData) {
        this.id = id;
        identifier = Key.stripMinecraftNamespace(identifier);
        final String translateKey = "vb.item." + identifier;
        this.jsonName = itemJsonName(fallbackName);
        this.tagName = itemTagName(translateKey, fallbackName);
        this.customModelData = customModelData;
    }

    // Make explicitly non-italic and white to override default item formatting.
    // Properly parsing is expensive...
    private static String itemJsonName(final String name) {
        return """
            {"italic":false,"color":"white","text":%s}
            """.formatted(new JsonPrimitive(name).toString());
    }

    private static Tag itemTagName(final String translateKey, final String fallbackName) {
        // Proper translate with fallback field (valid for 1.19.4+ clients)
        final CompoundTag tag = new CompoundTag();
        tag.putString("color", "white");
        tag.putString("translate", translateKey);
        tag.putString("fallback", fallbackName);
        tag.putBoolean("italic", false);
        return tag;
    }

    public int id() {
        return id;
    }

    public String jsonName() {
        return jsonName;
    }

    public Tag tagName() {
        return tagName.copy();
    }

    public int customModelData() {
        return customModelData;
    }
}
