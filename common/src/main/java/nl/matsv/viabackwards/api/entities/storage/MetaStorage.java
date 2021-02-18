/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.api.entities.storage;

import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;

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

    @Nullable
    public Metadata get(int index) {
        for (Metadata meta : this.metaDataList) {
            if (index == meta.getId()) {
                return meta;
            }
        }
        return null;
    }

    public Metadata getOrDefault(int index, Metadata data) {
        return getOrDefault(index, false, data);
    }

    public Metadata getOrDefault(int index, boolean removeIfExists, Metadata data) {
        Metadata existingData = get(index);
        if (removeIfExists && existingData != null) {
            delete(existingData);
        }
        return existingData != null ? existingData : data;
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
