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

package nl.matsv.viabackwards.api.entities.storage;

import org.checkerframework.checker.nullness.qual.Nullable;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;

import java.util.List;

public class MetaStorage {
    private List<Metadata> metaDataList;

    public MetaStorage(List<Metadata> metaDataList) {
        this.metaDataList = metaDataList;
    }

    public boolean has(Metadata data) {
        return this.metaDataList.contains(data);
    }

    public void delete(Metadata data) {
        this.metaDataList.remove(data);
    }

    public void delete(int index) {
        metaDataList.removeIf(meta -> meta.getId() == index);
    }

    public void add(Metadata data) {
        this.metaDataList.add(data);
    }

    public @Nullable Metadata get(int index) {
        for (Metadata meta : this.metaDataList) {
            if (index == meta.getId()) {
                return meta;
            }
        }
        return null;
    }

    public List<Metadata> getMetaDataList() {
        return metaDataList;
    }

    public void setMetaDataList(List<Metadata> metaDataList) {
        this.metaDataList = metaDataList;
    }

    @Override
    public String toString() {
        return "MetaStorage{" + "metaDataList=" + metaDataList + '}';
    }
}
