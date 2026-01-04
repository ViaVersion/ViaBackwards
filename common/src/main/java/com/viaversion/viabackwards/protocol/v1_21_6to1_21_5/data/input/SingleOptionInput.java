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
package com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.input;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class SingleOptionInput implements Input {

    private final String key;
    private final Entry[] options;
    private final @Nullable Tag label;

    private int value;

    public SingleOptionInput(final CompoundTag tag) {
        final ListTag<CompoundTag> options = tag.getListTag("options", CompoundTag.class);
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Options must not be empty in tag: " + tag);
        }

        this.key = tag.getString("key");
        this.options = options.stream().map(Entry::new).toArray(Entry[]::new);
        this.label = tag.getBoolean("label_visible", true) ? tag.get("label") : null;

        for (int i = 0; i < this.options.length; i++) {
            if (this.options[i].initial()) {
                if (this.value != 0) {
                    throw new IllegalArgumentException("Multiple initial options found in tag: " + tag);
                }
                this.value = i;
            }
        }
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String asCommandSubstitution() {
        return this.options[value].id;
    }

    @Override
    public Tag asTag() {
        return new StringTag(asCommandSubstitution());
    }

    public Entry[] options() {
        return options;
    }

    public @Nullable Tag label() {
        return label;
    }

    public int value() {
        return value;
    }

    public void setValue(final int value) {
        if (value < 0 || value >= options.length) {
            throw new IllegalArgumentException("Value must be between 0 and " + (options.length - 1));
        }
        this.value = value;
    }

    public void setClampedValue(final int value) {
        if (value < 0) {
            this.value = 0;
        } else if (value >= options.length) {
            this.value = 0;
        } else {
            this.value = value;
        }
    }

    public static class Entry {
        private final String id;
        private final Tag display;
        private final boolean initial;

        public Entry(final CompoundTag tag) {
            id = tag.getString("id");
            display = tag.get("display");
            initial = tag.getBoolean("initial", false);
        }

        public String id() {
            return id;
        }

        public Tag display() {
            return display;
        }

        public boolean initial() {
            return initial;
        }

        public Tag computeDisplay() {
            return Objects.requireNonNullElseGet(display, () -> new StringTag(id));
        }
    }
}
