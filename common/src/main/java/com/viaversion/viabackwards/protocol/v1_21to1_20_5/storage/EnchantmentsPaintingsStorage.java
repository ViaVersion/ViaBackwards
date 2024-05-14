/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2024 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_21to1_20_5.storage;

import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.util.KeyMappings;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class EnchantmentsPaintingsStorage implements StorableObject {
    private KeyMappings enchantments;
    private KeyMappings paintings;
    private int[] paintingMappings;
    private Tag[] enchantmentDescriptions;

    public KeyMappings enchantments() {
        return enchantments;
    }

    public void setEnchantments(final KeyMappings enchantment, final Tag[] enchantmentDescriptions) {
        this.enchantments = enchantment;
        this.enchantmentDescriptions = enchantmentDescriptions;
    }

    public KeyMappings paintings() {
        return paintings;
    }

    public void setPaintings(final KeyMappings paintings, final int[] paintingMappings) {
        this.paintings = paintings;
        this.paintingMappings = paintingMappings;
    }

    @Override
    public boolean clearOnServerSwitch() {
        return false;
    }

    public int mappedPainting(final int id) {
        return id > 0 && id < paintingMappings.length ? paintingMappings[id] : 0;
    }

    public @Nullable Tag enchantmentDescription(final int id) {
        return id > 0 && id < enchantmentDescriptions.length ? enchantmentDescriptions[id] : null;
    }
}
