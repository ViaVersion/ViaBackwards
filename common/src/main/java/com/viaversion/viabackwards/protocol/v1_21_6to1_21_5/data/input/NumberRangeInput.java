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
import com.viaversion.nbt.tag.FloatTag;
import com.viaversion.nbt.tag.Tag;

import static com.viaversion.viabackwards.utils.ChatUtil.text;
import static com.viaversion.viabackwards.utils.ChatUtil.translate;

public final class NumberRangeInput implements Input {

    private final String key;
    private final Tag label;
    private final String labelFormat;
    private final float start;
    private final float end;
    private final Float initial;
    private final Float step;

    private float value;

    public NumberRangeInput(final CompoundTag tag) {
        this.key = tag.getString("key");
        this.label = tag.get("label");
        this.labelFormat = tag.getString("label_format", "options.generic_value");
        this.start = tag.getFloat("start");
        this.end = tag.getFloat("end");
        this.initial = tag.getFloat("initial", (this.start + this.end) / 2F);
        final FloatTag stepTag = tag.getFloatTag("step");
        if (stepTag != null && stepTag.asFloat() < 0F) {
            throw new IllegalArgumentException("Step must be non-negative, got: " + stepTag.asFloat());
        }
        this.step = stepTag == null ? -1F : stepTag.asFloat();

        this.value = initial;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String asCommandSubstitution() {
        return valueAsString();
    }

    @Override
    public Tag asTag() {
        return new FloatTag(value);
    }

    public Tag label() {
        return label;
    }

    public String labelFormat() {
        return labelFormat;
    }

    public float start() {
        return start;
    }

    public float end() {
        return end;
    }

    public float initial() {
        return initial;
    }

    public float step() {
        return step;
    }

    public float value() {
        return value;
    }

    public String valueAsString() {
        final int asInt = (int) value;
        return asInt == value ? Integer.toString(asInt) : Float.toString(value);
    }

    public void setValue(final float value) {
        this.value = value;
    }

    public void setClampedValue(final float value) {
        this.value = (value < start) ? start : Math.min(value, end);
    }

    public Tag displayName() {
        return translate(labelFormat, label, text(valueAsString()));
    }
}
