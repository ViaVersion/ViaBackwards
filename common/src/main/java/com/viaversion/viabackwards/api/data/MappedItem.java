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

import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.util.ComponentUtil;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MappedItem {

    private final int id;
    private final String jsonName;
    private final Tag tagName;
    private final Integer customModelData;

    public MappedItem(final int id, final String name) {
        this(id, name, null);
    }

    public MappedItem(final int id, final String name, @Nullable final Integer customModelData) {
        this.id = id;
        this.jsonName = ComponentUtil.legacyToJsonString("§f" + name, true);
        this.tagName = ComponentUtil.jsonStringToTag(jsonName);
        this.customModelData = customModelData;
    }

    public int id() {
        return id;
    }

    public String jsonName() {
        return jsonName;
    }

    public Tag tagName() {
        return tagName;
    }

    public @Nullable Integer customModelData() {
        return customModelData;
    }
}
