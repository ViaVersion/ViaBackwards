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

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaversion.util.IdAndData;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MappedLegacyBlockItem {

    private final int id;
    private final short data;
    private final String name;
    private final IdAndData block;
    private BlockEntityHandler blockEntityHandler;

    public MappedLegacyBlockItem(int id) {
        this(id, (short) -1, null, Type.ITEM);
    }

    public MappedLegacyBlockItem(int id, short data, @Nullable String name, Type type) {
        this.id = id;
        this.data = data;
        this.name = name != null ? "§f" + name : null;
        this.block = type != Type.ITEM ? data != -1 ? new IdAndData(id, data) : new IdAndData(id) : null;
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

        void handleCompoundTag(int block, CompoundTag tag);
    }

    public enum Type {

        ITEM("items"),
        BLOCK_ITEM("block-items"),
        BLOCK("blocks");

        final String name;

        Type(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
