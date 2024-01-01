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

package com.viaversion.viabackwards.utils;

public class Block {
    private final int id;
    private final short data;

    public Block(int id, int data) {
        this.id = id;
        this.data = (short) data;
    }

    public Block(int id) {
        this.id = id;
        this.data = 0;
    }

    public int getId() {
        return id;
    }

    public int getData() {
        return data;
    }

    public Block withData(int data) {
        return new Block(this.id, data);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Block block = (Block) o;
        if (id != block.id) return false;
        return data == block.data;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + data;
        return result;
    }
}
