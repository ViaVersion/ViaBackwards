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

import com.viaversion.nbt.tag.ByteTag;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.Tag;

public final class BooleanInput implements Input {

    private final String key;
    private final Tag label;
    private final boolean initial;
    private final String onTrue;
    private final String onFalse;

    private boolean value;

    public BooleanInput(final CompoundTag tag) {
        this.key = tag.getString("key");
        this.label = tag.get("label");
        this.initial = tag.getBoolean("initial", false);
        this.onTrue = tag.getString("on_true", "true");
        this.onFalse = tag.getString("on_false", "false");

        this.value = initial;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String asCommandSubstitution() {
        return value ? onTrue : onFalse;
    }

    @Override
    public Tag asTag() {
        return new ByteTag(value);
    }

    public Tag label() {
        return label;
    }

    public boolean initial() {
        return initial;
    }

    public String onTrue() {
        return onTrue;
    }

    public String onFalse() {
        return onFalse;
    }

    public boolean value() {
        return value;
    }

    public void setValue(final boolean value) {
        this.value = value;
    }
}
