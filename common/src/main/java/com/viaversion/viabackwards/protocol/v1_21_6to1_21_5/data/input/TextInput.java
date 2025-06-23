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
package com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.input;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class TextInput implements Input {

    private final String key;
    private final @Nullable Tag label;
    private final String initial;
    private final int maxLength;
    private final @Nullable MultilineOptions[] options;

    private String value;

    public TextInput(final CompoundTag tag) {
        this.key = tag.getString("key");
        this.label = tag.getBoolean("label_visible", true) ? tag.get("label") : null;
        this.initial = tag.getString("initial", "");
        this.maxLength = tag.getInt("max_length", 32);
        if (this.maxLength < 1) {
            throw new IllegalArgumentException("Max length must be at least 1, got: " + this.maxLength);
        }
        final ListTag<CompoundTag> multilineList = tag.getListTag("multiline", CompoundTag.class);
        if (multilineList != null && !multilineList.isEmpty()) {
            this.options = multilineList.stream().map(MultilineOptions::new).toArray(MultilineOptions[]::new);
        } else {
            this.options = null;
        }

        this.value = initial;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String asCommandSubstitution() {
        return value;
    }

    @Override
    public Tag asTag() {
        return new StringTag(value);
    }

    public @Nullable Tag label() {
        return label;
    }

    public String initial() {
        return initial;
    }

    public int maxLength() {
        return maxLength;
    }

    public @Nullable MultilineOptions[] options() {
        return options;
    }

    public String value() {
        return value;
    }

    public void setValue(final String value) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException("Value exceeds max length of " + maxLength + ": " + value);
        }
        this.value = value;
    }

    public void setClampedValue(final String value) {
        if (value.length() > maxLength) {
            this.value = value.substring(0, maxLength);
        } else {
            this.value = value;
        }
    }

    public static class MultilineOptions {

        private final @Nullable Integer maxLines;

        public MultilineOptions(final CompoundTag tag) {
            final IntTag maxLinesTag = tag.getIntTag("max_lines");
            if (maxLinesTag != null && maxLinesTag.asInt() < 1) {
                throw new IllegalArgumentException("Max lines must be at least 1, got: " + maxLinesTag);
            }
            this.maxLines = maxLinesTag != null ? maxLinesTag.asInt() : null;
        }

        public @Nullable Integer maxLines() {
            return maxLines;
        }
    }
}
