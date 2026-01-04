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
package com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.widget;

import com.viaversion.nbt.tag.CompoundTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ItemWidget implements Widget {

    private final CompoundTag item;
    private final @Nullable TextWidget description;
    private final boolean showTooltip;
    private final int width;
    private final int height;

    public ItemWidget(final CompoundTag tag) {
        this.item = tag.getCompoundTag("item");
        if (this.item == null) {
            throw new IllegalArgumentException("Item tag is missing in ItemWidget tag: " + tag);
        }
        this.description = tag.contains("description") ? new TextWidget(tag.getCompoundTag("description")) : null;
        this.showTooltip = tag.getBoolean("show_tooltip", true);
        final int width = tag.getInt("width", 16);
        final int height = tag.getInt("height", 16);
        if (width < 1 || width > 256) {
            throw new IllegalArgumentException("Width must be between 1 and 256, got: " + width);
        }
        if (height < 1 || height > 256) {
            throw new IllegalArgumentException("Height must be between 1 and 256, got: " + height);
        }
        this.width = width;
        this.height = height;
    }

    public CompoundTag item() {
        return item;
    }

    public @Nullable TextWidget description() {
        return description;
    }

    public boolean showTooltip() {
        return showTooltip;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
