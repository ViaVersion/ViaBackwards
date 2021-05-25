/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
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

package com.viaversion.viabackwards.api.entities.storage;

import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public final class WrappedMetadata {
    private final List<Metadata> metadataList;

    public WrappedMetadata(List<Metadata> metadataList) {
        this.metadataList = metadataList;
    }

    public boolean has(Metadata data) {
        return this.metadataList.contains(data);
    }

    public void remove(Metadata data) {
        this.metadataList.remove(data);
    }

    public void remove(int index) {
        metadataList.removeIf(meta -> meta.id() == index);
    }

    public void add(Metadata data) {
        this.metadataList.add(data);
    }

    public @Nullable Metadata get(int index) {
        for (Metadata meta : this.metadataList) {
            if (index == meta.id()) {
                return meta;
            }
        }
        return null;
    }

    public List<Metadata> metadataList() {
        return metadataList;
    }

    @Override
    public String toString() {
        return "MetaStorage{" + "metaDataList=" + metadataList + '}';
    }
}
