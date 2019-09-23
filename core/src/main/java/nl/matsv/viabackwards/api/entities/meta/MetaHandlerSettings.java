/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.api.entities.meta;

import lombok.Getter;
import lombok.ToString;
import nl.matsv.viabackwards.api.exceptions.RemovedValueException;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;

@ToString
@Getter
public class MetaHandlerSettings {
    private EntityType filterType;
    private boolean filterFamily = false;
    private int filterIndex = -1;
    private MetaHandler handler;

    public MetaHandlerSettings filter(EntityType type) {
        return filter(type, filterFamily, filterIndex);
    }

    public MetaHandlerSettings filter(EntityType type, boolean filterFamily) {
        return filter(type, filterFamily, filterIndex);
    }

    public MetaHandlerSettings filter(int index) {
        return filter(filterType, filterFamily, index);
    }

    public MetaHandlerSettings filter(EntityType type, int index) {
        return filter(type, filterFamily, index);
    }

    public MetaHandlerSettings filter(EntityType type, boolean filterFamily, int index) {
        this.filterType = type;
        this.filterFamily = filterFamily;
        this.filterIndex = index;
        return this;
    }

    public void handle(MetaHandler handler) {
        this.handler = handler;
    }

    public void handleIndexChange(final int newIndex) {
        handle(e -> {
            Metadata data = e.getData();
            data.setId(newIndex);
            return data;
        });
    }

    public void removed() {
        handle(e -> {
            throw RemovedValueException.EX;
        });
    }

    public boolean hasHandler() {
        return handler != null;
    }

    public boolean hasType() {
        return filterType != null;
    }

    public boolean hasIndex() {
        return filterIndex > -1;
    }

    public boolean isFilterFamily() {
        return filterFamily;
    }

    public boolean isGucci(EntityType type, Metadata metadata) {
        if (!hasHandler()) return false;

        if (hasType()) {
            if (filterFamily) {
                if (!type.isOrHasParent(filterType)) {
                    return false;
                }
            } else {
                if (!filterType.is(type)) {
                    return false;
                }
            }
        }
        return !hasIndex() || metadata.getId() == filterIndex;
    }
}
