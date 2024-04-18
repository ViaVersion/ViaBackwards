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
package com.viaversion.viabackwards.api.data;

import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.util.IdAndData;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MappedLegacyBlockItem {

    private final int id;
    private final short data;
    private final String name;
    private final IdAndData block;
    private BlockEntityHandler blockEntityHandler;

    public MappedLegacyBlockItem(int id, short data, @Nullable String name, boolean block) {
        this.id = id;
        this.data = data;
        this.name = name != null ? "§f" + name : null;
        this.block = block ? data != -1 ? new IdAndData(id, data) : new IdAndData(id) : null;
    }

    public int getId() {
        return id;
    }

    public short getData() {
        return data;
    }

    public String getName() {
        return name;
    }

    public boolean isBlock() {
        return block != null;
    }

    public IdAndData getBlock() {
        return block;
    }

    public boolean hasBlockEntityHandler() {
        return blockEntityHandler != null;
    }

    public @Nullable BlockEntityHandler getBlockEntityHandler() {
        return blockEntityHandler;
    }

    public void setBlockEntityHandler(@Nullable BlockEntityHandler blockEntityHandler) {
        this.blockEntityHandler = blockEntityHandler;
    }

    @FunctionalInterface
    public interface BlockEntityHandler {

        CompoundTag handleOrNewCompoundTag(int block, CompoundTag tag);
    }
}
