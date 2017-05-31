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

import lombok.*;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;

import java.util.List;
import java.util.Optional;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class MetaStorage {
    @NonNull
    private List<Metadata> metaDataList;

    public boolean has(Metadata data) {
        return this.getMetaDataList().contains(data);
    }

    public void delete(Metadata data) {
        this.getMetaDataList().remove(data);
    }

    public void delete(int index) {
        Optional<Metadata> data = get(index);
        if (data.isPresent())
            delete(data.get());
    }

    public void add(Metadata data) {
        this.getMetaDataList().add(data);
    }

    public Optional<Metadata> get(int index) {
        for (Metadata meta : this.getMetaDataList())
            if (index == meta.getId())
                return Optional.of(meta);
        return Optional.empty();
    }

    public Metadata getOrDefault(int index, Metadata data) {
        return getOrDefault(index, false, data);
    }

    public Metadata getOrDefault(int index, boolean removeIfExists, Metadata data) {
        Optional<Metadata> existingData = get(index);

        if (removeIfExists && existingData.isPresent())
            delete(existingData.get());
        return existingData.orElse(data);
    }
}
