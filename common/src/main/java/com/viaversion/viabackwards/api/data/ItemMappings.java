/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2023 ViaVersion and contributors
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

import com.viaversion.viaversion.api.data.BiMappings;
import com.viaversion.viaversion.api.data.Mappings;

public final class ItemMappings implements BiMappings {

    private final Mappings mappings;
    private final ItemMappings inverse;

    private ItemMappings(final Mappings mappings, final Mappings inverse) {
        this.mappings = mappings;
        this.inverse = new ItemMappings(inverse, this);
    }

    private ItemMappings(final Mappings mappings, final ItemMappings inverse) {
        this.mappings = mappings;
        this.inverse = inverse;
    }

    public static ItemMappings of(final Mappings mappings, final Mappings inverse) {
        return new ItemMappings(mappings, inverse);
    }

    @Override
    public BiMappings inverse() {
        return inverse;
    }

    @Override
    public int getNewId(final int id) {
        return mappings.getNewId(id);
    }

    @Override
    public void setNewId(final int id, final int mappedId) {
        // Only set one-way
        mappings.setNewId(id, mappedId);
    }

    @Override
    public int size() {
        return mappings.size();
    }

    @Override
    public int mappedSize() {
        return mappings.mappedSize();
    }

    //TODO remove
    public Mappings createInverse() {
        return null;
    }
}
